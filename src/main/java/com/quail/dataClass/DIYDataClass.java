package com.quail.dataClass;

import akka.cluster.ddata.AbstractReplicatedData;
import akka.cluster.ddata.GSet;

import java.util.HashSet;
import java.util.Set;

/**
 * 自定义数据类型
 *
 * @author quail
 */
public class DIYDataClass extends AbstractReplicatedData<DIYDataClass> {

    public final GSet<String> adds;
    public final GSet<String> removals;

    public DIYDataClass(GSet<String> adds, GSet<String> removals) {
        this.adds = adds;
        this.removals = removals;
    }

    public static DIYDataClass create(){
        return new DIYDataClass(GSet.create(), GSet.create());
    }

    public DIYDataClass add(String element){
        return new DIYDataClass(adds.add(element),removals);
    }

    public DIYDataClass remove(String element){
        return new DIYDataClass(adds, removals.add(element));
    }

    public Set<String> getElements(){
        Set<String> result = new HashSet<>(adds.getElements());
        result.removeAll(removals.getElements());
        return result;
    }

    @Override
    public DIYDataClass mergeData(DIYDataClass that) {
        return new DIYDataClass(this.adds.merge(that.adds), this.removals.merge(that.removals));
    }
}
