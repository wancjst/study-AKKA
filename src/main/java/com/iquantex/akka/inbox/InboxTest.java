package com.iquantex.akka.inbox;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author quail
 */
public class InboxTest extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public enum Msg{
        WORKING, DONE, CLOST;
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o == Msg.WORKING){
            log.info("i am working");
        } else if(o == Msg.DONE){
            log.info("i am done");
        } else if(o == Msg.CLOST){
            log.info("i am close");
            getSender().tell(Msg.CLOST, getSelf());// 告诉消息发送者我要关闭了
            getContext().stop(getSelf());// 关闭自己
        } else{
            unhandled(o);
        }
    }

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("inbox", ConfigFactory.load("akka.conf"));
        ActorRef inboxTest = actorSystem.actorOf(Props.create(InboxTest.class), "InboxTest");

        Inbox inbox = Inbox.create(actorSystem);
        inbox.watch(inboxTest);//监听一个actor

        // 通过inbox来发送消息
        inbox.send(inboxTest,Msg.WORKING);
        inbox.send(inboxTest,Msg.DONE);
        inbox.send(inboxTest,Msg.CLOST);

        while (true){
            try {
                Object receive = inbox.receive(Duration.create(1, TimeUnit.SECONDS));
                if (receive == Msg.CLOST){//收到的inbox的消息
                    System.out.println("inboxTestActor is closing");
                } else if(receive instanceof Terminated){
                    System.out.println("inboxTestActor is closed");
                    System.exit(1);
                } else {
                    System.out.println(receive);
                }
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
    }
}
