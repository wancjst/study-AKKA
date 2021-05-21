package com.iquantex.akka.future;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * 将一个actor的返回结果重定向到另一个actor中进行处理，主actor或者进程无需等待actor的返回结果。
 * @author quail
 */
public class AskMain {
    public static void main(String[] args) throws Exception {
        // actor的管理员，负责创建actor、给actor分配任务等，比如：指定某个actor负责监控
        ActorSystem system = ActorSystem.create("strategy", ConfigFactory.load("akka.config"));
        // 实例化actorRef对象
        ActorRef printActor = system.actorOf(Props.create(PrintActor.class), "PrintActor");
        ActorRef workerActor = system.actorOf(Props.create(WorkerActor.class), "workerActor");

        // 等future返回
        // ask与tell都是发送消息，tell发完就不管了，ask会在一定时间等待返回结果，如果超时了则抛异常（异步）
        // ask 操作包括创建一个内部临时actor来处理回应，必须为这个内部actor指定一个超时期限，过了超时期限内部actor将被销毁以防止内存泄露。
        Future<Object> future = Patterns.ask(workerActor, 5, 1000);
        // 使用Await.result等待Future完成或者超时。
        // Future代表一个异步计算，可以设置回调函数或者利用Await.result等待获取异步计算的结果,也可以组合多个future为一个新的future。
        int result = (int) Await.result(future, Duration.create(3, TimeUnit.SECONDS));
        System.out.println("result:"+result);

        // 不等返回值，直接重定向到其他actor，有返回值来的时候会重定向到pringactor
        Future<Object> future1 = Patterns.ask(workerActor, 9, 1000);
        // 将一个actor的返回结果重定向到另一个actor中进行处理，主actor或者进程无需等待actor的返回结果。
        Patterns.pipe(future1,system.dispatcher()).to(printActor);

        // 毒丸信息，当消息被处理时，它将停止Actor。毒丸作为普通消息排队，并将在邮箱中已经排队的消息后处理。
        workerActor.tell(PoisonPill.getInstance(),ActorRef.noSender());
    }
}
