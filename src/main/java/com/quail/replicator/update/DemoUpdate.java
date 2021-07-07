package com.quail.replicator.update;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Flag;
import akka.cluster.ddata.FlagKey;
import akka.cluster.ddata.GSet;
import akka.cluster.ddata.GSetKey;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.ORSetKey;
import akka.cluster.ddata.PNCounter;
import akka.cluster.ddata.PNCounterKey;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.Replicator.Update;
import akka.cluster.ddata.Replicator.WriteConsistency;
import akka.cluster.ddata.Replicator.WriteTo;
import akka.cluster.ddata.Replicator.WriteMajority;
import akka.cluster.ddata.Replicator.WriteAll;
import akka.cluster.ddata.SelfUniqueAddress;

import java.time.Duration;

/**
 * 各种Key的使用
 *
 * @author quail
 */
public class DemoUpdate extends AbstractActor {

    final SelfUniqueAddress node = DistributedData.get(getContext().getSystem()).selfUniqueAddress();

    final ActorRef replicator = DistributedData.get(getContext().getSystem()).replicator();

    final Key<PNCounter> counterKey = PNCounterKey.create("counter");
    final Key<GSet<String>> set1Key = GSetKey.create("set1");
    final Key<ORSet<String>> set2Key = ORSetKey.create("set2");
    final Key<Flag> activeFlagKey = FlagKey.create("active");

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchEquals(
                "demonstrate update",
                msg -> {
                    replicator.tell(
                            new Update<PNCounter>(
                                    counterKey,
                                    PNCounter.create(),
                                    Replicator.writeLocal(),
                                    curr -> curr.increment(node, 1) // 增/减计数器
                            ),getSelf());
                    final WriteConsistency writeTo3 = new WriteTo(3, Duration.ofSeconds(1));
                    replicator.tell(
                            new Replicator.Update<GSet<String>>(
                                    set1Key, GSet.create(), writeTo3, curr -> curr.add("hello")), // 增set
                            getSelf());

                    final WriteConsistency writeMajority = new WriteMajority(Duration.ofSeconds(5));
                    replicator.tell(
                            new Replicator.Update<ORSet<String>>(
                                    set2Key, ORSet.create(), writeMajority, curr -> curr.add(node, "hello")), // 增/减set
                            getSelf());

                    final WriteConsistency writeAll = new WriteAll(Duration.ofSeconds(5));
                    replicator.tell(
                            new Replicator.Update<Flag>(
                                    activeFlagKey, Flag.create(), writeAll, curr -> curr.switchOn()), // 标记，开关
                            getSelf());
                }
        ).build();
    }
}
