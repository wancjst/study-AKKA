package com.iquantex.akka.persistence;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.SnapshotOffer;
import akka.persistence.UntypedPersistentActor;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import static java.util.Arrays.asList;

class Cmd implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String data;

    public Cmd(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}

class Evt implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String data;
    private final String uuid;

    public Evt(String data, String uuid) {
        this.data = data;
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getData() {
        return data;
    }
}

class ExampleState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayList<String> events;//事件存储

    public ExampleState() {
        this(new ArrayList<String>());
    }

    public ExampleState(ArrayList<String> events) {
        this.events = events;
    }

    // 复制一份事件存储
    public ExampleState copy() {
        return new ExampleState(new ArrayList<String>(events));
    }

    // 加入一个事件
    public void update(Evt evt) {
        events.add(evt.getData());
    }

    public int size() {
        return events.size();
    }

    @Override
    public String toString() {
        return events.toString();
    }
}

class ExamplePersistentActor extends UntypedPersistentActor {

    LoggingAdapter log = Logging.getLogger(getContext().system (), this );

    @Override
    public String persistenceId() { return "sample-id-1"; }

    private ExampleState state = new ExampleState();

    public int getNumEvents() {
        return state.size();
    }

    /**
     * 重启，首先从快照加载，然后回收日志事件以更新状态。
     * @param msg
     */
    @Override
    public void onReceiveRecover(Object msg){
        log.info("onReceiveRecover: " + msg);
        if (msg instanceof Evt) {
            log.info("onReceiveRecover -- msg instanceof Event");
            log.info("event --- " + ((Evt) msg).getData());
            state.update((Evt) msg);
        } else if (msg instanceof SnapshotOffer) {
            log.info("onReceiveRecover -- msg instanceof SnapshotOffer");
            state = (ExampleState)((SnapshotOffer)msg).snapshot();
        } else {
            unhandled(msg);
        }
    }

    /**
     * Called on Command dispatch
     * @param msg
     */
    @Override
    public void onReceiveCommand(Object msg) throws JsonProcessingException {
        log.info("onReceiveCommand: " + msg);
        if (msg instanceof Cmd) {
            final String data = ((Cmd)msg).getData();

            // 生成一个事件，用uuid充实后持久化
            final Evt evt1 = new Evt(data + "-" + getNumEvents(), UUID.randomUUID().toString());
            final Evt evt2 = new Evt(data + "-" + (getNumEvents() + 1), UUID.randomUUID().toString());

            // 持久化事件后更新处理器状态
            persistAll(asList(evt1, evt2), evt -> {
                state.update(evt);
                if (evt.equals(evt2)) {
                    // broadcast event on eventstream 发布该事件
                    getContext().system().eventStream().publish(evt);
                }
            });
        } else if (msg.equals("snap")) {
            // IMPORTANT: create a copy of snapshot
            // because ExampleState is mutable !!!
            saveSnapshot(state.copy());
        } else if (msg.equals("print")) {
            System.out.println("state:  " + state);
        } else {
            unhandled(msg);
        }
    }
}

class EventHandler extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object msg ) throws Exception {
        log.info( "Handled Event: " + msg);
    }
}

