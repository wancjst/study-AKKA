package com.iquantex.akka.cluster;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author quail
 */
public class MyAkkaClusterClient2555 extends UntypedActor {

    List<ActorRef> backends = new ArrayList<>();
    int jobCounter = 0;

    @Override
    public void onReceive(Object msg) throws Exception {
//        System.out.println(msg.toString());
        if(msg instanceof TransformationMessage.TransformationJob && backends.isEmpty()){// 无服务提供者
            TransformationMessage.TransformationJob job = (TransformationMessage.TransformationJob) msg;
            getSender().tell(new TransformationMessage.JobFailed("Service unavailable, try again later", job),getSelf());
        }else if(msg instanceof TransformationMessage.TransformationJob){
            /*
            这里在客户端业务代码里进行负载均衡操作。实际业务中可以提供多种负载均衡策略，并且也可以做分流限流等各种控制。
             */
            TransformationMessage.TransformationJob  job = (TransformationMessage.TransformationJob) msg;
            jobCounter++;
            backends.get(jobCounter % backends.size()).forward(job,getContext());
        }else if(msg.equals(TransformationMessage.BACKEND_REGISTRATION)){
            /*
            注册服务提供者
             */
            getContext().watch(getSender());// 这里对服务提供者进行watch
            backends.add(getSender());
        }else if(msg instanceof Terminated){
            /*
            移除服务提供者
             */
            Terminated terminated = (Terminated) msg;
            backends.remove(terminated.getActor());
        }else {
            unhandled(msg);
        }
    }

    public static void main(String[] args) {
        System.out.println("Start MyAkkaClusterClient");
        ActorSystem system = ActorSystem.create("akkaClusterTest", ConfigFactory.load("reference-2555.conf"));
        final ActorRef myAkkaClusterClient2555 = system.actorOf(Props.create(MyAkkaClusterClient2555.class), "myAkkaClusterClient2555");
        System.out.println("Started MyAkkaClusterServer");

        final FiniteDuration interval = Duration.create(2, TimeUnit.SECONDS);
        final Timeout timeout = new Timeout(Duration.create(5,TimeUnit.SECONDS));
        final ExecutionContext ec = system.dispatcher();
        final AtomicInteger counter = new AtomicInteger();

        system.scheduler().schedule(interval, interval,()->{
                Patterns.ask(myAkkaClusterClient2555, new TransformationMessage.TransformationJob("hello-" + counter.incrementAndGet()), timeout)
                        .onSuccess(new OnSuccess<Object>() {
                            @Override
                            public void onSuccess(Object o) throws Throwable {
                                System.out.println(o.toString());
                            }
                        },ec);
        },ec);
    }
}
