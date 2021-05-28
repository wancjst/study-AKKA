package com.iquantex.akka.cluster;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

/**
 * @author quail
 */
public class SimpleClusterListener extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(),this);
    Cluster cluster = Cluster.get(getContext().system());

    // 订阅集群
    @Override
    public void preStart(){
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
    }

    // 退订
    @Override
    public void postStop(){
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message){
        if(message instanceof ClusterEvent.MemberUp){
            ClusterEvent.MemberUp mUp = (ClusterEvent.MemberUp) message;
            log.info("Member is Up: {}", mUp.member());
        }else if(message instanceof ClusterEvent.UnreachableMember){
            ClusterEvent.UnreachableMember mUnreachable = (ClusterEvent.UnreachableMember) message;
            log.info("Member detected as unreachable: {}", mUnreachable.member());
        }else if (message instanceof ClusterEvent.MemberRemoved) {
            ClusterEvent.MemberRemoved mRemoved = (ClusterEvent.MemberRemoved) message;
            log.info("Member is Removed: {}", mRemoved.member());
        } else if (message instanceof ClusterEvent.MemberEvent) {
            ClusterEvent.MemberEvent mMember = (ClusterEvent.MemberEvent) message;
            log.info("Member is Removed: {}", mMember.member());
        } else {
            unhandled(message);
        }
    }

    public static void main(String[] args) {
        System.out.println("Start simpleClusterListener");
        ActorSystem system = ActorSystem.create("akkaClusterTest", ConfigFactory.load("reference-2551.conf"));
        ActorRef listenerActor = system.actorOf(Props.create(SimpleClusterListener.class), "simpleClusterListener");
        System.out.println("Started simpleClusterListener");
    }
}
