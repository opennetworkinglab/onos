/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.service;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of a AtomicValue update notification.
 *
 * @param <V> atomic value type
 */
public final class AtomicValueEvent<V> {

    /**
     * AtomicValueEvent type.
     */
    public enum Type {

        /**
         * Value was updated.
         */
        UPDATE,
    }

    private final String name;
    private final Type type;
    private final V value;

    /**
     * Creates a new event object.
     *
     * @param name AtomicValue name
     * @param type the type of the event
     * @param value the new value
     */
    public AtomicValueEvent(String name, Type type, V value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * Returns the AtomicValue name.
     *
     * @return name of atomic value
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
     * Returns the new updated value.
     *
     * @return the value
     */
    public V value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AtomicValueEvent)) {
            return false;
        }

        AtomicValueEvent that = (AtomicValueEvent) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("type", type)
                .add("value", value)
                .toString();
    }
}
