package com.iquantex.akka.unmodifiable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;

/**
 * @author quail
 */
public class Main {

    public static void main(String[] args) {
        // 创建ActorSystem(actor入口)，一般来说，一个系统只需要一个ActorSystem
        // 参数1：系统名称。参数2：配置文件
        ActorSystem system = ActorSystem.create("Hello", ConfigFactory.load("akka.config"));
        ActorRef ref = system.actorOf(Props.create(HelloWorld.class),"HelloWorld");
        System.out.println(ref.path());
    }
}
