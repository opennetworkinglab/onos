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
package org.onosproject.isis.controller.topology;

import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;

import java.util.List;

/**
 * Representation of ISIS link traffic engineering parameters.
 */
public interface IsisLinkTed {

    /**
     * Gets the administrative group.
     *
     * @return administrative group
     */
    int administrativeGroup();

    /**
     * Sets the administrative group.
     *
     * @param administrativeGroup administrative group
     */
    void setAdministrativeGroup(int administrativeGroup);

    /**
     * Provides the IPv4 interface address.
     *
     * @return IPv4 interface address
     */
    Ip4Address ipv4InterfaceAddress();

    /**
     * Sets the IPv4 interface address.
     *
     * @param interfaceAddress IPv4 interface address
     */
    void setIpv4InterfaceAddress(Ip4Address interfaceAddress);

    /**
     * Provides the IPv4 neighbor address.
     *
     * @return IPv4 neighbor address
     */
    Ip4Address ipv4NeighborAddress();

    /**
     * Sets the IPv4 neighbor address.
     *
     * @param neighborAddress IPv4 neighbor address
     */
    void setIpv4NeighborAddress(Ip4Address neighborAddress);

    /**
     * Gets the maximum link bandwidth.
     *
     * @return maximum link bandwidth
     */
    Bandwidth maximumLinkBandwidth();

    /**
     * Sets the maximum link bandwidth.
     *
     * @param bandwidth maximum link bandwidth
     */
    void setMaximumLinkBandwidth(Bandwidth bandwidth);

    /**
     * Provides max bandwidth that can be reservable on the link.
     *
     * @return max bandwidth reservable
     */
    Bandwidth maximumReservableLinkBandwidth();

    /**
     * Sets max bandwidth that can be reservable on the link.
     *
     * @param bandwidth max bandwidth that can be reservable on the link
     */
    void setMaximumReservableLinkBandwidth(Bandwidth bandwidth);

    /**
     * Amount of bandwidth unreserved on the link.
     *
     * @return unreserved bandwidth
     */
    List<Bandwidth> unreservedBandwidth();

    /**
     * Sets the bandwidth unreserved on the link.
     *
     * @param bandwidth bandwidth unreserved
     */
    void setUnreservedBandwidth(List<Bandwidth> bandwidth);

    /**
     * Provides Traffic Engineering metric for the link.
     *
     * @return Traffic Engineering Default metric
     */
    long teDefaultMetric();

    /**
     * Sets Traffic Engineering metric for the link.
     *
     * @param teMetric Traffic Engineering Default metric for the link
     */
    void setTeDefaultMetric(long teMetric);
}