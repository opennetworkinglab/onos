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

package org.onosproject.ui.topo;

/**
 * Parent class of {@link DeviceHighlight} and {@link HostHighlight}.
 */
public abstract class NodeHighlight extends AbstractHighlight {

    private NodeBadge badge;

    /**
     * Constructs a node highlight entity.
     *
     * @param type element type
     * @param elementId element identifier
     */
    public NodeHighlight(TopoElementType type, String elementId) {
        super(type, elementId);
    }

    /**
     * Sets the badge for this node.
     *
     * @param badge badge to apply
     */
    public void setBadge(NodeBadge badge) {
        this.badge = badge;
    }

    /**
     * Returns the badge for this node, if any.
     *
     * @return badge, or null
     */
    public NodeBadge badge() {
        return badge;
    }
}
