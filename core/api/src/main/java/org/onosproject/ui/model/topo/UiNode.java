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
 * Represents a node drawn on the topology view (region, device, host).
 */
public abstract class UiNode extends UiElement {

    /**
     * Default "layer" tag.
     */
    public static final String LAYER_DEFAULT = "def";

    /**
     * Packet layer tag.
     */
    public static final String LAYER_PACKET = "pkt";

    /**
     * Optical layer tag.
     */
    public static final String LAYER_OPTICAL = "opt";


    private String layer = LAYER_DEFAULT;

    /**
     * Returns the tag for the "layer" that the node should be rendered in
     * when viewed in the oblique view.
     *
     * @return the node's layer
     */
    public String layer() {
        return layer;
    }

    /**
     * Sets this node's "layer", for layered rendering.
     *
     * @param layerTag the layer tag to set
     */
    public void setLayer(String layerTag) {
        layer = layerTag;
    }
}
