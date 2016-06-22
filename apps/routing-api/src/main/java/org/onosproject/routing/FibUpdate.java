/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.routing;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Represents a change to the Forwarding Information Base (FIB).
 *
 * @deprecated use RouteService instead
 */
@Deprecated
public class FibUpdate {

    /**
     * Specifies the type of the FIB update.
     */
    public enum Type {
        /**
         * The update contains a new or updated FIB entry for a prefix.
         */
        UPDATE,

        /**
         * The update signals that a prefix should be removed from the FIB.
         */
        DELETE
    }

    private final Type type;
    private final FibEntry entry;

    /**
     * Creates a new FIB update.
     *
     * @param type type of the update
     * @param entry FIB entry describing the update
     */
    public FibUpdate(Type type, FibEntry entry) {
        this.type = type;
        this.entry = entry;
    }

    /**
     * Returns the type of the update.
     *
     * @return update type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the FIB entry which contains update information.
     *
     * @return the FIB entry
     */
    public FibEntry entry() {
        return entry;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FibUpdate)) {
            return false;
        }

        FibUpdate that = (FibUpdate) o;

        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.entry, that.entry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, entry);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type)
                .add("entry", entry)
                .toString();
    }
}
