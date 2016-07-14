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
package org.onosproject.provider.lldpcommon;

import org.onosproject.mastership.MastershipService;
import org.onosproject.net.LinkKey;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.packet.PacketService;

/**
 * Shared context for use by link discovery.
 */
public interface LinkDiscoveryContext {

    /**
     * Returns the shared mastership service reference.
     *
     * @return mastership service
     */
    MastershipService mastershipService();

    /**
     * Returns the shared link provider service reference.
     *
     * @return link provider service
     */
    LinkProviderService providerService();

    /**
     * Returns the shared packet service reference.
     *
     * @return packet service
     */
    PacketService packetService();

    /**
     * Returns the DeviceService reference.
     *
     * @return the device service interface
     */
    DeviceService deviceService();

    /**
     * Returns the probe rate in millis.
     *
     * @return probe rate
     */
    long probeRate();

    /**
     * Indicates whether to emit BDDP.
     *
     * @return true to emit BDDP
     */
    boolean useBddp();

    /**
     * Touches the link identified by the given key to indicate that it's active.
     *
     * @param key link key
     */
    void touchLink(LinkKey key);

    /**
     * Returns the cluster-wide unique identifier.
     *
     * @return the cluster identifier
     */
    String fingerprint();
}
