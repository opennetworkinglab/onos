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

import org.onosproject.net.Device;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Link;

import java.util.Set;

/**
 * Represents a bi-directional link backed by two uni-directional links.
 */
public class UiLink extends UiElement {

    // devices at either end of this link
    private Device deviceA;
    private Device deviceB;

    // two unidirectional links underlying this link...
    private Link linkAtoB;
    private Link linkBtoA;

    // ==OR== : private (synthetic) host link
    private EdgeLink edgeLink;

    // ==OR== : set of underlying UI links that this link aggregates
    private Set<UiLink> children;


    @Override
    protected void destroy() {
        deviceA = null;
        deviceB = null;
        linkAtoB = null;
        linkBtoA = null;
        edgeLink = null;
        if (children != null) {
            children.clear();
            children = null;
        }
    }
}
