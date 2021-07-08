package com.quail.dataClass;

import akka.actor.ExtendedActorSystem;
import akka.cluster.ddata.GSet;
import akka.cluster.ddata.protobuf.AbstractSerializationSupport;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author quail
 */
public class DIYDataClassSerializer extends AbstractSerializationSupport {

    private final ExtendedActorSystem system;

    public DIYDataClassSerializer(ExtendedActorSystem system) {
        this.system = system;
    }

    @Override
    public ExtendedActorSystem system() {
        return this.system;
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        return diyDataClassFromBinary(bytes);
    }

    @Override
    public int identifier() {
        return 0;
    }

    @Override
    public byte[] toBinary(Object o) {
        if (o instanceof DIYDataClass){
            return DIYDataClassToProto((DIYDataClass) o).toByteArray();
        }else {
            try {
                throw new IllegalAccessException("Can't serialize object of type " + o.getClass());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    private DataClassMessages.DataClass DIYDataClassToProto(DIYDataClass diyDataClass){
        DataClassMessages.DataClass.Builder builder = DataClassMessages.DataClass.newBuilder();
        ArrayList<String> adds = new ArrayList<>(diyDataClass.adds.getElements());
        if(!adds.isEmpty()){
            Collections.sort(adds);
            builder.addAllAdds(adds);
        }
        ArrayList<String> removals = new ArrayList<>(diyDataClass.removals.getElements());
        if(!removals.isEmpty()){
            Collections.sort(removals);
            builder.addAllRemovals(removals);
        }
        return builder.build();
    }

    private DIYDataClass diyDataClassFromBinary(byte[] bytes){
        try{
            DataClassMessages.DataClass msg = DataClassMessages.DataClass.parseFrom(bytes);
            GSet<String> adds = GSet.create();
            for(String elem : msg.getAddsList()){
                adds = adds.add(elem);
            }
            GSet<String> removals = GSet.create();
            for (String elem : msg.getRemovalsList()) {
                removals = removals.add(elem);
            }
            return new DIYDataClass(adds.resetDelta(), removals.resetDelta());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
