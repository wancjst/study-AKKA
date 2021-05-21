package com.iquantex.akka.lifecycle;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quail
 */
public class WatchActor extends UntypedActor {

    LoggingAdapter logger = Logging.getLogger(getContext().system(),this);

    /**
     * 监听一个actor
     * @param actorRef
     */
    public WatchActor(ActorRef actorRef){
        logger.info("123");
        getContext().watch(actorRef);
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if(msg instanceof Terminated){
            logger.error(((Terminated)msg).getActor().path() + " has Terminated. now shutdown the system");
            getContext().system().shutdown();
        }else {
            unhandled(msg);
        }
    }
}
