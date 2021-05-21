package com.iquantex.akka.unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * actor中传递的对象要是不可变对象。为了安全
 * @author quail
 */
public final class Message {

    private final int age;
    private final List<String> list;

    public Message(int age, List<String> list) {
        this.age = age;
        // unmodifiableList只读，将普通list包装成不可变对象
        this.list = Collections.unmodifiableList(list);
    }

    public int getAge() {
        return age;
    }

    public List<String> getList() {
        return list;
    }
}
