package com.quail.replicator.get;

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
import akka.japi.pf.ReceiveBuilder;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Set;

/**
 * @author quail
 */
public class DemoGet extends AbstractActor {
    final ActorRef replicator = DistributedData.get(getContext().getSystem()).replicator();

    final Key<PNCounter> counter1Key = PNCounterKey.create("counter1");
    final Key<GSet<String>> set1Key = GSetKey.create("set1");
    final Key<ORSet<String>> set2Key = ORSetKey.create("set2");
    final Key<Flag> activeFlagKey = FlagKey.create("active");

    @Override
    public Receive createReceive() {
        ReceiveBuilder b = receiveBuilder();

        b.matchEquals(
                "demonstrate get",
                msg -> {
                    replicator.tell(
                            new Replicator.Get<PNCounter>(counter1Key, Replicator.readLocal()), getSelf());

                    final Replicator.ReadConsistency readFrom3 = new Replicator.ReadFrom(3, Duration.ofSeconds(1));
                    replicator.tell(new Replicator.Get<GSet<String>>(set1Key, readFrom3), getSelf());

                    final Replicator.ReadConsistency readMajority = new Replicator.ReadMajority(Duration.ofSeconds(5));
                    replicator.tell(new Replicator.Get<ORSet<String>>(set2Key, readMajority), getSelf());

                    final Replicator.ReadConsistency readAll = new Replicator.ReadAll(Duration.ofSeconds(5));
                    replicator.tell(new Replicator.Get<Flag>(activeFlagKey, readAll), getSelf());
                    /**
                     * readLocal，该值将只从本地副本中读取
                     * ReadFrom(n)，该值将从n个副本（包括本地副本）中读取和合并
                     * ReadMajority，该值将从大多数副本（即至少N/2 + 1个副本）中读取和合并，其中N是集群（或集群角色组）中的节点数
                     * ReadAll，该值将从群集中的所有节点（或群集角色组中的所有节点）中读取和合并。
                     */
                });
        b.match(
                Replicator.GetSuccess.class,
                a -> a.key().equals(counter1Key),
                a -> {
                    Replicator.GetSuccess<PNCounter> g = a;
                    BigInteger value = g.dataValue().getValue();
                })
                .match(
                        Replicator.NotFound.class, // key不存在时
                        a -> a.key().equals(counter1Key),
                        a -> {
                            // key counter1 does not exist
                            ActorRef replyTo = (ActorRef) a.getRequest().get(); // 上下文
                            replyTo.tell(0L, getSelf());
                        });
        b.match(
                Replicator.GetSuccess.class,
                a -> a.key().equals(set1Key),
                a -> {
                    Replicator.GetSuccess<GSet<String>> g = a;
                    Set<String> value = g.dataValue().getElements();
                })
                .match(
                        Replicator.GetFailure.class,
                        a -> a.key().equals(set1Key),
                        a -> {
                            // read from 3 nodes failed within 1.second
                        })
                .match(
                        Replicator.NotFound.class,
                        a -> a.key().equals(set1Key),
                        a -> {
                            // key set1 does not exist
                        });
        return b.build();
    }
}
