package com.iquantex.akka.router;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.iquantex.akka.inbox.InboxTest;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author quail
 */
public class RouterTest extends UntypedActor {

    public Router router;

    {
        ArrayList<Routee> routees =  new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ActorRef actorRef = getContext().actorOf(Props.create(InboxTest.class), "worker_" + i);
            getContext().watch(actorRef);//监听
            routees.add(new ActorRefRoutee(actorRef));
        }
        /**
         * RoundRobinRoutingLogic:轮询
         * BroadcastRoutingLogic:广播
         * RandomRoutingLogic:随机
         * SmallestMailboxRoutingLogic:空闲
         */
        router = new Router(new RoundRobinRoutingLogic(),routees);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof InboxTest.Msg){
            router.route(o, getSender());//进行路由转发
        }else if(o instanceof Terminated){
            router.removeRoutee(((Terminated)o).actor());//发生中断，将该actor删除。
            System.out.println(((Terminated)o).actor().path() + "该actor已删除。router.size="+router.routees().size());

            if(router.routees().size() == 0){
                // 没有可以actor了
                System.out.println("没有可用actor，关闭系统");
                flag.compareAndSet(true,false);
                getContext().system().shutdown();
            }
        }else {
            unhandled(o);
        }
    }

    public static AtomicBoolean flag = new AtomicBoolean(true);

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("strategy", ConfigFactory.load("akka.config"));
        ActorRef routerTest = system.actorOf(Props.create(RouterTest.class), "RouterTest");

        int i = 1;
        while(flag.get()){
            routerTest.tell(InboxTest.Msg.WORKING,ActorRef.noSender());
            if(i%10==0) routerTest.tell(InboxTest.Msg.CLOST,ActorRef.noSender());
            Thread.sleep(500);
            i++;
        }
    }
}
