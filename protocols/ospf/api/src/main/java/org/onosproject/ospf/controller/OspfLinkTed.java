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
package org.onosproject.ospf.controller;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.util.Bandwidth;

import java.util.List;

/**
 * Represents OSPF Link Traffic Engineering parameters.
 */
public interface OspfLinkTed {

    /**
     * Provides maximum bandwidth can be used on the link.
     *
     * @return maximum bandwidth
     */
    public Bandwidth maximumLink();

    /**
     * Sets maximum band width.
     *
     * @param bandwidth maximum bandwidth
     */
    public void setMaximumLink(Bandwidth bandwidth);

    /**
     * Amount of bandwidth reservable on the link.
     *
     * @return unreserved bandwidth
     */
    public List<Bandwidth> maxUnResBandwidth();

    /**
     * Sets max bandwidth that is not reserved on the link.
     *
     * @param bandwidth max bandwidth that is not reserved on the link
     */
    public void setMaxUnResBandwidth(Bandwidth bandwidth);

    /**
     * Provides max bandwidth that can be reserved on the link.
     *
     * @return max bandwidth reserved
     */
    public Bandwidth maxReserved();

    /**
     * Sets max bandwidth that can be reserved on the link.
     *
     * @param bandwidth max bandwidth that can be reserved on the link
     */
    public void setMaxReserved(Bandwidth bandwidth);

    /**
     * Provides Traffic Engineering metric for the link.
     *
     * @return Traffic Engineering metric
     */
    public Integer teMetric();

    /**
     * Sets Traffic Engineering metric for the link.
     *
     * @param teMetric Traffic Engineering metric for the link
     */
    public void setTeMetric(Integer teMetric);

    /**
     * Provides IPv4 router-Id of local node.
     *
     * @return IPv4 router-Id of local node
     */
    public List<Ip4Address> ipv4LocRouterId();

    /**
     * Sets IPv4 router-Id of local node.
     *
     * @param routerIds IPv4 router-Id of local node
     */
    public void setIpv4LocRouterId(List<Ip4Address> routerIds);

    /**
     * Provides IPv6 router-Id of local node.
     *
     * @return IPv6 router-Id of local node
     */
    public List<Ip6Address> ipv6LocRouterId();

    /**
     * Sets IPv6 router-Id of local node.
     *
     * @param routerIds IPv6 router-Id of local node
     */
    public void setIpv6LocRouterId(List<Ip6Address> routerIds);

    /**
     * Provides IPv4 router-Id of remote node.
     *
     * @return IPv4 router-Id of remote node
     */
    public List<Ip4Address> ipv4RemRouterId();

    /**
     * Sets IPv4 router-Id of remote node.
     *
     * @param routerIds IPv4 router-Id of remote node
     */
    public void setIpv4RemRouterId(List<Ip4Address> routerIds);

    /**
     * Provides IPv6 router-Id of remote node.
     *
     * @return IPv6 router-Id of remote node
     */
    public List<Ip6Address> ipv6RemRouterId();

    /**
     * Sets IPv6 router-Id of remote node.
     *
     * @param routerIds IPv6 router-Id of remote node
     */
    public void setIpv6RemRouterId(List<Ip6Address> routerIds);
}