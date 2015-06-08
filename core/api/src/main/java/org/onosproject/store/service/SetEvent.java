package org.onosproject.store.service;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of a DistributedSet update notification.
 *
 * @param <E> element type
 */
public class SetEvent<E> {

    /**
     * SetEvent type.
     */
    public enum Type {
        /**
         * Entry added to the set.
         */
        ADD,

        /**
         * Entry removed from the set.
         */
        REMOVE
    }

    private final String name;
    private final Type type;
    private final E entry;

    /**
     * Creates a new event object.
     *
     * @param name set name
     * @param type the type of the event
     * @param entry the entry the event concerns
     */
    public SetEvent(String name, Type type, E entry) {
        this.name = name;
        this.type = type;
        this.entry = entry;
    }

    /**
     * Returns the set name.
     *
     * @return name of set
     */
    public String name() {
        return name;
    }

    /**
     * Returns the type of the event.
     *
     * @return the type of the event
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the entry this event concerns.
     *
     * @return the entry
     */
    public E entry() {
        return entry;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SetEvent)) {
            return false;
        }

        SetEvent<E> that = (SetEvent) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.entry, that.entry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, entry);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("type", type)
                .add("entry", entry)
                .toString();
    }
}
