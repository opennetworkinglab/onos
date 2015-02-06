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
package org.onosproject.sdnip;

/**
 * Represents a change to the Forwarding Information Base (FIB).
 */
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
}
