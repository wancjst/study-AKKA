package com.iquantex.akka.unmodifiable;

import akka.actor.UntypedActor;
import com.iquantex.akka.common.JSONObject;

/**
 * @author quail
 */
public class Greeter extends UntypedActor {

    @Override
    public void onReceive(Object msg) throws Exception {
        try {
            System.out.println("Greeter收到的数据为：" + JSONObject.toJsonString(msg));
            getSender().tell("Greeter工作完成",getSelf());//给发送端响应信息
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
