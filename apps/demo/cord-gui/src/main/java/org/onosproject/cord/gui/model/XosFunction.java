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
 *
 */

package org.onosproject.cord.gui.model;


import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Designates a specific instance of an XOS function.
 */
public interface XosFunction {

    /**
     * Returns the descriptor for this function.
     *
     * @return function descriptor
     */
    XosFunctionDescriptor descriptor();

    /**
     * Applies a parameter change for the given user.
     *
     * @param user user to apply change to
     * @param param parameter name
     * @param value new parameter value
     */
    void applyParam(SubscriberUser user, String param, String value);

    /**
     * Create an initialized memento.
     * If the function maintains no state per user, return null.
     *
     * @return a new memento
     */
    Memento createMemento();

    /**
     * Create the XOS specific URL suffix for applying state change for
     * the given user.
     *
     * @param user the user
     * @return URL suffix
     */
    String xosUrlApply(SubscriberUser user);

    /**
     * Internal state memento.
     */
    interface Memento {
        /**
         * Returns a JSON representation of this memento.
         *
         * @return memento state as object node
         */
        ObjectNode toObjectNode();
    }
}

