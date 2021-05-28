package com.iquantex.akka.cluster;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

/**
 * @author quail
 */
public class MyAkkaClusterServer2551 extends UntypedActor {

    LoggingAdapter logger = Logging.getLogger(getContext().system(),this);
    // 集群管理者
    Cluster cluster = Cluster.get(getContext().system());

    @Override
    public void preStart() throws Exception {
        // 订阅
        cluster.subscribe(getSelf(),ClusterEvent.MemberUp.class);
    }

    @Override
    public void postStop() throws Exception {
        // 退订
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if(msg instanceof TransformationMessage.TransformationJob){
            TransformationMessage.TransformationJob job = (TransformationMessage.TransformationJob) msg;
            logger.info(job.getText());
            getSender().tell(new TransformationMessage.TransformationResult(job.getText().toUpperCase()),getSelf());
        }else if(msg instanceof ClusterEvent.CurrentClusterState){
            /*
            当前节点在刚刚加入集群时，会收到CurrentClusterState（即时集群状态）消息，
            从中可以解析出集群中的所有前端节点（即roles为frontend的），
            并向其发送BACKEND_REGISTRATION消息，用于注册自己
             */
            ClusterEvent.CurrentClusterState state = (ClusterEvent.CurrentClusterState) msg;// 集群中每个node的状态信息
            for (Member member : state.getMembers()) {// 每个成员（node）
                if(member.status().equals(MemberStatus.up())){// node当前状态是up（工作状态）
                    register(member);
                }
            }
        }else if(msg instanceof ClusterEvent.MemberUp){
            /*
            有新节点加入
             */
            ClusterEvent.MemberUp mUp = (ClusterEvent.MemberUp) msg;
            register(mUp.member());
        }else{
            unhandled(msg);
        }
    }

    /**
     * 如果是客户端角色，则像客户端注册自己的信息。客户端收到消息以后会将这个服务端存到本机服务列表中
     * @param member
     */
    void register(Member member){
        if(member.hasRole("client")){
            getContext().actorSelection(member.address()+"/system/cluster/core/daemon/MyAkkaClusterClient2555").tell(TransformationMessage.BACKEND_REGISTRATION,getSelf());
        }
    }

    public static void main(String[] args) {
        System.out.println("Start MyAkkaClusterServer");
        // 集群中只能有一个ActorSystem，所以每个node的system名字相同，如果不同将被集群排除在外
        ActorSystem system = ActorSystem.create("akkaClusterTest", ConfigFactory.load("reference-2551.conf"));
        system.actorOf(Props.create(MyAkkaClusterServer2551.class), "myAkkaClusterServer");
        System.out.println("Started MyAkkaClusterServer");
    }
}
