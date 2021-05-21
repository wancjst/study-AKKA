package com.iquantex.akka.hello;

import akka.actor.UntypedActor;

/**
 * @author quail
 */
public class Greeter extends UntypedActor {

    public static enum Msg{
        GREET, DONE;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg == Msg.GREET) {
            System.out.println("hello world");
            Thread.sleep(1000);
            getSender().tell(Msg.DONE,getSelf());
        } else {
            unhandled(msg);
        }
    }
}
