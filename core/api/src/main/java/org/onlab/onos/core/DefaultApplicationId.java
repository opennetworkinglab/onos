package org.onlab.onos.core;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Application identifier.
 */
public class DefaultApplicationId implements ApplicationId {

    private final short id;
    private final String name;

    /**
     * Creates a new application ID.
     *
     * @param id   application identifier
     * @param name application name
     */
    public DefaultApplicationId(Short id, String name) {
        this.id = id;
        this.name = name;
    }

    // Constructor for serializers.
    private DefaultApplicationId() {
        this.id = 0;
        this.name = null;
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
