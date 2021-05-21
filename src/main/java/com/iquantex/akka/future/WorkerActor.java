package com.iquantex.akka.future;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quail
 */
public class WorkerActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object o) throws InterruptedException {
        log.info("akka.future.WorkerActor.onReceive:" + o);

        if (o instanceof Integer) {
            Thread.sleep(1000);
            int i = Integer.parseInt(o.toString());
            getSender().tell(i*i, getSelf());
        } else {
            unhandled(o);
        }
    }

}
