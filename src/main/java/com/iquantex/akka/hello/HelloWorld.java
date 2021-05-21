package com.iquantex.akka.hello;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * @author quail
 */
public class HelloWorld extends UntypedActor {

    /**
     * 启动akka
     * 在preStart()中创建greeterActor
     * tell发送消息，第一个参数是message，第二个参数是actorRef
     */
    @Override
    public void preStart(){
        // 创建greeter actor
        final ActorRef greeter = getContext().actorOf(Props.create(Greeter.class),"greeter");
        greeter.tell(Greeter.Msg.GREET, getSelf());
    }

    /**
     * 接收消息
     * @param msg 接收到的数据
     */
    @Override
    public void onReceive(Object msg){
        if (msg == Greeter.Msg.DONE){
            getContext().stop(getSelf());
        } else {
            unhandled(msg);
        }
    }
}
