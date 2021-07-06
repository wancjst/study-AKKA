# akka-cluster DistributedData

## 理论基础

### java8 :: 语法糖

- 静态方法引用（static method）语法：classname::methodname 例如：Person::getAge
- 对象的实例方法引用语法：instancename::methodname 例如：System.out::println
- 对象的超类方法引用语法： super::methodname
- 类构造器引用语法： classname::new 例如：ArrayList::new
- 数组构造器引用语法： typename[]::new 例如： String[]:new

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

