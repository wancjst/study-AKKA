package com.quail.replicator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.quail.replicator.HelloWorld;

/**
 * @author quail
 */
public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        ActorRef ref = system.actorOf(Props.create(HelloWorld.class), "hello");
    }
}
