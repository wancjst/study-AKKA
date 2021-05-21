package com.iquantex.akka.unmodifiable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.iquantex.akka.common.JSONObject;

import java.util.Arrays;

/**
 * @author quail
 */
public class HelloWorld extends UntypedActor {

    ActorRef greeter;

    @Override
    public void preStart() {
        // create the greeter actor
        greeter = getContext().actorOf(Props.create(Greeter.class), "greeter");
        System.out.println("Greeter actor path：" + greeter.path());
        // tell it to perform the greeting
        greeter.tell(new Message(2, Arrays.asList("2", "dsf")), getSelf());
    }

    @Override
    public void onReceive(Object o) throws JsonProcessingException {
        System.out.println("helloworld收到的数据：" + JSONObject.toJsonString(o));
    }
}
