# akka-cluster DistributedData

官方文档：https://doc.akka.io/docs/akka/current/typed/distributed-data.html

参考博客：https://blog.csdn.net/qq_35246620/article/details/55004877

demo：https://github.com/wancjst/study-AKKA/tree/DistributedData

## 需求背景

在实现分布式DGC的时候，可能会有个需求是各个JVM节点共享同一个数据，然后做增删改查，akka-cluster提供了Distributed Data特性，该issue需要学习研究该特性，分析是否支持，关注的点有：

1. 分布式数据是如何使用的（增删改查）？
2. 数据是如何共享的？
3. 数据是如何持久化的？
4. 一致性说是最终一致，是什么样的原理？
5. 是否有数据的回调机制？一个JVM节点更新了 分布式数据，然后他可以通知其他JVM中使用该数据的actor，做一些操作

## 理论基础

### java8 :: 语法糖

静态方法引用（static method）语法：classname::methodname 例如：Person::getAge
对象的实例方法引用语法：instancename::methodname 例如：System.out::println
对象的超类方法引用语法： super::methodname
类构造器引用语法： classname::new 例如：ArrayList::new
数组构造器引用语法： typename[]::new 例如： String[]:new

### CAP理论

CAP理论作为分布式系统的基础理论,它描述的是一个分布式系统在以下三个特性中：

- 一致性（**C**onsistency）
- 可用性（**A**vailability）
- 分区容错性（**P**artition tolerance）

最多满足其中的两个特性。也就是下图所描述的。分布式系统要么满足CA,要么CP，要么AP。无法同时满足CAP。

### CRDT

CRDT(Conflict-Free Replicated Data Type)：无冲突复制数据类型，是各种基础数据结构最终一致性算法的理论总结，能根据一定的规制自动合并，解决冲突，达到强最终一致性的效果。

### gossip协议

gossip 协议利用一种随机的方式将信息传播到整个网络中，并在一定时间内使得系统内的所有节点数据一致，保证最终一致性。Gossip 其实是一种去中心化思路的分布式协议，解决状态在集群中的传播和状态一致性的保证两个问题。

## 分布式数据（DistributedData）

### 复制器（Replicator)

`akka.cluster.ddata.Replicator`Actor提供了与数据交互的API。`Replicator`必须在集群中的每个节点上启动，或在标记有特定角色的节点组上启动。它与运行在其他节点上具有相同路径（不是地址）的其他`Replicator`实例通信。

总结：

- 复制器是一个特别的actor。
- 通过订阅指定ORSet，监控它的变化进行不同处理。

- ORSet变化时会发布一个Change类型的消息。
- 用于接收Update实例，根据参数和指定级别复制新值，由同一路径的管理者下的actor发送Update给其他replicator。
- replicator必须在cluster每个node上启动，与运行在其他节点上的、具有相同路径的其他复制器实例通信。

#### update

作为update操作的答复，如果在提供的超时内根据提供的一致性级别成功复制了值，则会向update的发送方发送`Replicator.UpdateSuccess`，失败`Replicator.UpdateFailure`，超时`Replicator.UpdateTimeout`，回复并不意味着更新完全失败或已回滚，它可能仍然被复制到一些节点上，并最终通过`gossip`协议复制到所有节点上。

```
Update<ORSet<String>> update = new Update<>( // 将数据写入ORSet中
        dataKey, ORSet.create(), Replicator.writeLocal(), curr -> curr.add(cluster, s));
```

#### get

要检索数据的当前值，请向`Replicator`发生`Replicator.Get`消息。你提供的一致性级别具有以下含义：

- `readLocal`，该值将只从本地副本中读取
- `ReadFrom(n)`，该值将从`n`个副本（包括本地副本）中读取和合并
- `ReadMajority`，该值将从大多数副本（即至少`N/2 + 1`个副本）中读取和合并，其中`N`是集群（或集群角色组）中的节点数
- `ReadAll`，该值将从群集中的所有节点（或群集角色组中的所有节点）中读取和合并。

```java
replicator.tell(
              new Replicator.Get<PNCounter>(counter1Key, Replicator.readLocal()), getSelf());
```

#### delete

向`Delete`的发送者发送`Replicator.DeleteSuccess`。否则将发送`Replicator.ReplicationDeleteFailure`。请注意，`ReplicationDeleteFailure`并不意味着删除完全失败或已回滚。它可能仍然被复制到某些节点，并最终被复制到所有节点。

已删除的键不能再次使用，但仍建议删除未使用的数据条目，因为这样可以减少新节点加入群集时的复制开销。随后的`Delete`、`Update`和`Get`请求将用`Replicator.DataDeleted`回复。订阅者将收到`Replicator.Deleted`。

```java
replicator.tell(
                  new Delete<PNCounter>(counter1Key, Replicator.writeLocal()), getSelf());
```



#### 一致性

使用`readLocal`读取数据存在过时数据风险，也就是说其他节点的更新不可见。

解决方案：**(nodes_written + nodes_read) > N**    （N是node数），保证一致性读写均使用：`WriteMajority`和`ReadMajority`。

例：当集群中存在7个node，那么读写总节点数要大于7，比如写入4个node，读取4个node；写入5个node，读取3个node。因为在进行某一波写入操作，再进行读取时，必有至少一个node完成了update。

特殊情况：在进行读取操作的时候，先update了数据，又加入了新node，导致读取新值的时候刚好读到了还没update的node和新加入的node。

例：5个node，n1、n2、n3进行update，加入n6、n7，查询时写入2个node（n1/n2），读取4个node（n3/n4/n5/n6），导致读取的node都是旧数据。

#### 订阅

将在更新订阅键的数据时向注册订阅者发送`Replicator.Changed`消息。将使用配置的`notify-subscribers-interval`定期通知订阅者，还可以向`Replicator`发送显式`Replicator.FlushChange`消息以立即通知订阅者。

如果订阅者被终止，则会自动删除订阅者。订阅者也可以使用`Replicator.Unsubscribe`取消订阅消息。

```java
.match(
    Changed.class,
    a -> a.key().equals(counter1Key),
    a -> {
        Changed<PNCounter> g = a;
        currentValue = g.dataValue().getValue();
    })
```

```java
 // subscribe to changes of the Counter1Key value
 replicator.tell(new Subscribe<PNCounter>(counter1Key, getSelf()), ActorRef.noSender());
```

#### delta-CRDT

使其看到传播过程，满足因果一致性，不仅限于最终一致性。

例：{a,b}加入{c,d}，最终一致性就{a,b,c,d}，看不到{a,b,c}或{a,b,d}。

```java
akka.cluster.distributed-data.delta-crdt.enabled=off
```

### 数据类型

提供以下几种类型：

- Counters：`GCounter`、`PNCounter`
- Sets：`GSet`、`ORSet`
- Maps：`ORMap`、`ORMultiMap`、`LWWMap`、`PNCounterMap`
- Registers：`LWWRegister`，`Flag`

#### Counters

计数器

```java
final SelfUniqueAddress node = DistributedData.get(system).selfUniqueAddress();
final PNCounter c0 = PNCounter.create();
final PNCounter c1 = c0.increment(node, 1);
final PNCounter c2 = c1.increment(node, 7);
final PNCounter c3 = c2.decrement(node, 2);
System.out.println(c3.value()); // 6
```

#### Maps

- `ORMultiMap`（`observed-remove multi-map`）是一个多映射实现，它用一个`ORSet`来包装一个`ORMap`以获得该映射的值。
- `PNCounterMap`（`positive negative counter map`）是命名计数器的映射（其中名称可以是任何类型）。它是具有`PNCounter`值的特殊`ORMap`。
- `LWWMap`（`last writer wins map`）是一个具有`LWWRegister`（`last writer wins register`）值的特殊`ORMap`。

```java
final SelfUniqueAddress node = DistributedData.get(system).selfUniqueAddress();
final PNCounterMap<String> m0 = PNCounterMap.create();
final PNCounterMap<String> m1 = m0.increment(node, "a", 7);
final PNCounterMap<String> m2 = m1.decrement(node, "a", 2);
final PNCounterMap<String> m3 = m2.increment(node, "b", 1);
System.out.println(m3.get("a")); // 5
System.out.println(m3.getEntries()); //{a=5, b=1}
```

```java
final SelfUniqueAddress node = DistributedData.get(system).selfUniqueAddress();
final ORMultiMap<String, Integer> m0 = ORMultiMap.create();
final ORMultiMap<String, Integer> m1 = m0.put(node, "a", new HashSet<>(Arrays.asList(1, 2, 3)));
final ORMultiMap<String, Integer> m2 = m1.addBinding(node, "a", 4);
final ORMultiMap<String, Integer> m3 = m2.removeBinding(node, "a", 2);
final ORMultiMap<String, Integer> m4 = m3.addBinding(node, "b", 1);
System.out.println(m4.getEntries()); // {a=[1, 3, 4], b=[1]}
```

#### Sets

```java
final GSet<String> s2 = s1.add("b").add("c");
final ORSet<String> s2 = s1.add(node, "b");
final ORSet<String> s3 = s2.remove(node, "a");
```

#### Registers

```java
final Flag f0 = Flag.create(); // flase
final Flag f1 = f0.switchOn();
System.out.println(f1.enabled()); // true
```

LWWRegister可保存任何可序列化的值。

#### 自定义数据类型

唯一的要求是它实现`AbstractReplicatedData`特性的`mergeData`函数。

### 持久存储

#### 数据持久化

默认情况下，数据只保存在内存中，如果停止所有节点，数据就会丢失。

Entries 可以配置为持久化的，即存储在每个节点的本地磁盘上，重启actor系统时，将加载存储的数据。这意味着只要旧集群中至少有一个node加入到新集群中，数据就可以生存（node上的replicator会将数据传到每个node上）。

持久化条目的键配置：

```java
akka.cluster.distributed-data.durable.keys = ["*"] //所有条目持久化
```

#### CRDT垃圾

`CRDT`的一个问题是，某些数据类型会累积历史记录（垃圾）。例如，`GCounter`跟踪每个节点的一个计数器。如果已经从一个节点更新了`GCounter`，它将永远关联该节点的标识符。对于添加和删除了许多群集节点的长时间运行的系统来说，这可能成为一个问题。

解决方案：**replicator**将 已删除node的数据 在cluster中进行修剪。需要修剪的数据类型必须实现`RemovedNodePruning`特性。

