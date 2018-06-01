/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.host;

import org.apache.commons.lang3.NotImplementedException;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.provider.ProviderService;

/**
 * Means of conveying host information to the core.
 */
public interface HostProviderService extends ProviderService<HostProvider> {

    /**
     * Notifies the core when a host has been detected on a network along with
     * information that identifies the host location.
     *
     * @param hostId          id of the host that been detected
     * @param hostDescription description of host and its location
     * @param replaceIps      replace IP set if true, merge IP set otherwise
     */
    void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps);

    /**
     * Notifies the core when a host is no longer detected on a network.
     *
     * @param hostId id of the host that vanished
     */
    void hostVanished(HostId hostId);

    /**
     * Notifies the core when an IP is no longer associated with a host.
     *
     * @param hostId id of the host
     * @param ipAddress ip address of host that vanished
     */
    void removeIpFromHost(HostId hostId, IpAddress ipAddress);

    /**
     * Notifies the core when a location is associated with a host.
     *
     * @param hostId id of the host
     * @param location location of host that gets discovered
     */
    default void addLocationToHost(HostId hostId, HostLocation location) {
        throw new NotImplementedException("addLocationToHost is not implemented");
    }

    /**
     * Notifies the core when a location is no longer associated with a host.
     *
     * @param hostId id of the host
     * @param location location of host that vanished
     */
    void removeLocationFromHost(HostId hostId, HostLocation location);

    /**
     * Notifies HostProviderService the beginning of pending host location verification and
     * retrieves the unique MAC address for the probe.
     *
     * @param hostId ID of the host
     * @param connectPoint the connect point that is under verification
     * @param probeMode probe mode
     * @return probeMac, the source MAC address ONOS uses to probe the host
     * @deprecated in ONOS 1.12, replaced by {@link HostProbingProviderService}
     */
    @Deprecated
    default MacAddress addPendingHostLocation(HostId hostId, ConnectPoint connectPoint, ProbeMode probeMode) {
        return MacAddress.NONE;
    }

    /**
     * Notifies HostProviderService the end of pending host location verification.
     *
     * @param probeMac the source MAC address ONOS uses to probe the host
     * @deprecated in ONOS 1.12, replaced by {@link HostProbingProviderService}
     */
    @Deprecated
    default void removePendingHostLocation(MacAddress probeMac) {}
}
