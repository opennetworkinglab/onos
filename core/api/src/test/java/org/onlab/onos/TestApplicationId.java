package org.onlab.onos;

import org.onlab.onos.core.ApplicationId;

import java.util.Objects;

/**
 * Test application ID.
 */
public class TestApplicationId implements ApplicationId {

    private final String name;
    private final short id;

    public TestApplicationId(String name) {
        this.name = name;
        this.id = (short) Objects.hash(name);
    }

    public static ApplicationId create(String name) {
        return new TestApplicationId(name);
    }

    @Override
    public short id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }
}
