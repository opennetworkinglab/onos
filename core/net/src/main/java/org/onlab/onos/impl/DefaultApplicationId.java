package org.onlab.onos.impl;

import org.onlab.onos.ApplicationId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Application id generator class.
 */
public class DefaultApplicationId implements ApplicationId {

    private final short id;
    private final String name;

    // Ban public construction
    protected DefaultApplicationId(Short id, String identifier) {
        this.id = id;
        this.name = identifier;
    }

    @Override
    public short id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultApplicationId) {
            DefaultApplicationId other = (DefaultApplicationId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("name", name).toString();
    }

}
