/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.cli;

import java.io.IOException;

/**
 * Common APIs for T3 CLI commands to load snapshots of the network states
 * from {@link org.onosproject.t3.api.NibProfile.SourceType} and fill the NIB.
 */
public interface NibLoader {

    /**
     * Extracts flow-related information and fills the flow NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadFlowNib() throws IOException;

    /**
     * Extracts group-related information and fills the group NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadGroupNib() throws IOException;

    /**
     * Extracts host-related information and fills the host NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadHostNib() throws IOException;

    /**
     * Extracts link-related information and fills the link NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadLinkNib() throws IOException;

    /**
     * Extracts device-related information and fills the device NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadDeviceNib() throws IOException;

    /**
     * Extracts driver-related information and fills the driver NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadDriverNib() throws IOException;

    /**
     * Extracts mastership-related information and fills the mastership NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadMastershipNib() throws IOException;

    /**
     * Extracts edge port-related information and fills the edge port NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadEdgePortNib() throws IOException;

    /**
     * Extracts route-related information and fills the route NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadRouteNib() throws IOException;

    /**
     * Extracts network config-related information and fills the network configuration NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadNetworkConfigNib() throws IOException;

    /**
     * Extracts multicast route-related information and fills the multicast route NIB.
     *
     * @throws IOException  exception during possible file I/O
     */
    void loadMulticastRouteNib() throws IOException;

}
