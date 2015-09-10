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

package org.onosproject.ui.topo;

import com.google.common.base.MoreObjects;

/**
 * Designates the identity of a button on the topology view panels.
 */
public class ButtonId {

    private final String id;

    /**
     * Creates a button ID with the given identifier.
     *
     * @param id identifier for the button
     */
    public ButtonId(String id) {
        this.id = id;
    }

    /**
     * Returns the identifier for this button.
     *
     * @return identifier
     */
    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ButtonId that = (ButtonId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
