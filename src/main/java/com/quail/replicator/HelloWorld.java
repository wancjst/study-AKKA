package com.quail.replicator;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.ORMultiMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.ORSetKey;
import akka.cluster.ddata.PNCounterMap;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.Replicator.Changed;
import akka.cluster.ddata.Replicator.Subscribe;
import akka.cluster.ddata.Replicator.Update;
import akka.cluster.ddata.Replicator.UpdateResponse;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;


/**
 * @author quail
 */
public class HelloWorld extends AbstractActor {
    // 数据，标记，负责触发消息处理机制（createReceive()）
    private static final String TICK = "tick";
    // 日志
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    // 复制器
    private final ActorRef replicator = DistributedData.get(getContext().getSystem()).replicator();
    // 集群
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    // 一个可以取消的异步任务
    private final Cancellable tickTask =
            getContext()
            .getSystem()
            .scheduler()
            .schedule(
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(5),
                    getSelf(),
                    TICK,
                    getContext().getDispatcher(),
                    getSelf()
            );

    // 设置键ORSetKey，增/减
    private final Key<ORSet<String>> dataKey = ORSetKey.create("key");

    // 接收消息
    @SuppressWarnings("unchecked") // 抑制警告
    @Override
    public Receive createReceive() {
        return receiveBuilder() // 接收消息的处理
                .match(String.class,a -> a.equals(TICK),a -> receiveTick()) // 匹配消息类型，进行不同处理
                .match(
                        Changed.class,
                        c -> c.key().equals(dataKey),
                        c -> receiveChanged((Changed<ORSet<String>>) c))
                .match(UpdateResponse.class, r -> receiveUpdateResponse())
                .match(
                        Replicator.UpdateSuccess.class,
                        a -> a.key().equals(dataKey),
                        a -> {
                            ActorRef replyTo = (ActorRef) a.getRequest().get(); // 上下文
                            log.info(dataKey.toString()+" update : success");
                            replyTo.tell("success", getSelf());
                        })
                .match(
                        Replicator.UpdateFailure.class,
                        a -> a.key().equals(dataKey),
                        a -> {
                            ActorRef replyTo = (ActorRef) a.getRequest().get();
                            log.info(dataKey.toString()+" update : fail");
                            replyTo.tell("fail", getSelf());
                        })
                .match(
                        Replicator.UpdateTimeout.class,
                        a -> a.key().equals(dataKey),
                        a -> {
                            ActorRef replyTo = (ActorRef) a.getRequest().get();
                            log.info(dataKey.toString()+" update : timeout");
                            replyTo.tell("timeout", getSelf());
                        })
                .build();
    }

    /**
     * 更新TICK中的元素，使用Update<>()对ORSet进行操作
     *
     * new Update<>(key, ORSet.create(), write, function)
     * ORSet<String> curr
     */
    private void receiveTick(){
        String s = String.valueOf((char) ThreadLocalRandom.current().nextInt(97,123)); // 随机生成一个26字母
        if (ThreadLocalRandom.current().nextBoolean()){ // 分开处理：add/remove
            log.info("Adding: {}", s);
            Update<ORSet<String>> update = new Update<>( // 将数据写入ORSet中
                    dataKey, ORSet.create(), Replicator.writeLocal(), curr -> curr.add(cluster, s));
            replicator.tell(update, getSelf());// 将update信息发送给本地replicator
        } else{
            log.info("remove: {}", s);
            Update<ORSet<String>> update = new Update<>(
                    dataKey, ORSet.create(), Replicator.writeLocal(), curr -> curr.remove(cluster, s));
            replicator.tell(update, getSelf());
        }
        /**
         * 将update信息发送给本地replicator，function返回更新后的ORSet<>,再根据write一致性级别复制新值，
         * 由与Replicator运行在同一本地ActorSystem的actor发送update。
         *
         * writeLocal，该值将立即只被写入本地副本，然后通过gossip进行传播。
         * WriteTo(n)，该值将立即写入至少n个副本，包括本地副本。
         * WriteMajority，该值将立即写入大多数副本，即至少N/2 + 1个副本，其中N是群集（或群集角色组）中的节点数。
         * WriteAll，该值将立即写入群集中的所有节点（或群集中角色组中的所有节点）。
         */
    }

    private void receiveChanged(Changed<ORSet<String>> s){
        ORSet<String> data = s.dataValue();
        log.info("Current elements: {}", data.getElements());
    }

    private void receiveUpdateResponse(){
        // ignore
    }

    @Override
    public void preStart(){
        final ORMultiMap<String, Integer> m0 = ORMultiMap.create();
        final ORMultiMap<String, Integer> m1 = m0.put(cluster, "a", new HashSet<>(Arrays.asList(1, 2, 3)));
        final ORMultiMap<String, Integer> m2 = m1.addBinding(cluster, "a", 4);
        final ORMultiMap<String, Integer> m3 = m2.removeBinding(cluster, "a", 2);
        final ORMultiMap<String, Integer> m4 = m3.addBinding(cluster, "b", 1);
        System.out.println(m4.getEntries());

        // 订阅, 监控dataKey的变化
//        Subscribe<ORSet<String>> sub = new Subscribe<>(dataKey, getSelf());
//        replicator.tell(sub, Actor.noSender());
    }

    @Override
    public void postStop(){
        tickTask.cancel();
    }
}
