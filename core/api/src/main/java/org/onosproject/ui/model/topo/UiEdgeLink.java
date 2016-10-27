/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.ui.model.topo;

/**
 * Designates a link between a device and a host; that is, an edge link.
 */
public class UiEdgeLink extends UiLink {

    private final String hostId;
    private final String edgeDevice;
    private final String edgePort;

    /**
     * Creates a UI link.
     *
     * @param topology parent topology
     * @param id       canonicalized link identifier
     */
    public UiEdgeLink(UiTopology topology, UiLinkId id) {
        super(topology, id);
        hostId = id.idA();
        edgeDevice = id.elementB().toString();
        edgePort = id.portB().toString();
    }

    @Override
    public String endPointA() {
        return hostId;
    }

    @Override
    public String endPointB() {
        return edgeDevice;
    }

    // no port for end-point A

    @Override
    public String endPortB() {
        return edgePort;
    }
}
