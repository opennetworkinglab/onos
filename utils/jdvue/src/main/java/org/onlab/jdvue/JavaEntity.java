package org.onlab.jdvue;

import java.util.Objects;

/**
 * Abstraction of a Java source entity.
 */
public abstract class JavaEntity {

    private final String name;

    /**
     * Creates a new Java source entity with the given name.
     *
     * @param name source entity name
     */
    JavaEntity(String name) {
        this.name = name;
    }

    /**
     * Returns the Java source entity name.
     *
     * @return source entity name
     */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JavaEntity) {
            JavaEntity that = (JavaEntity) o;
            return getClass().equals(that.getClass()) &&
                    Objects.equals(name, that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
