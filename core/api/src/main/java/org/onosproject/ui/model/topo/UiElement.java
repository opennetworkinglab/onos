/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

/**
 * Abstract base class of all elements in the UI topology model.
 */
public abstract class UiElement {

    /**
     * Removes all external references, and prepares the instance for
     * garbage collection. This default implementation does nothing.
     */
    protected void destroy() {
        // does nothing
    }

    /**
     * Returns a string representation of the element identifier.
     *
     * @return the element unique identifier
     */
    public abstract String idAsString();

    /**
     * Returns a friendly name to be used for display purposes.
     * This default implementation returns the result of calling
     * {@link #idAsString()}.
     *
     * @return the friendly name
     */
    public String name() {
        return idAsString();
    }
}
