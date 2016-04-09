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
package org.onosproject.ospf.controller.impl;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.util.Bandwidth;
import org.onosproject.ospf.controller.OspfLinkTed;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements OSPF Link Traffic engineering details.
 */
public class OspfLinkTedImpl implements OspfLinkTed {


    Bandwidth maximumLink;
    List<Bandwidth> maxUnResBandwidth = new ArrayList<>();
    Bandwidth maxReserved;
    Integer teMetric;
    List<Ip4Address> ipv4LocRouterId = new ArrayList<>();
    List<Ip6Address> ipv6LocRouterId = new ArrayList<>();
    List<Ip4Address> ipv4RemRouterId = new ArrayList<>();
    List<Ip6Address> ipv6RemRouterId = new ArrayList<>();


    /**
     * Gets maximum link.
     *
     * @return maximum link
     */
    public Bandwidth maximumLink() {
        return maximumLink;
    }

    /**
     * Sets maximum link.
     *
     * @param maximumLink maximum link
     */
    public void setMaximumLink(Bandwidth maximumLink) {
        this.maximumLink = maximumLink;
    }

    /**
     * Gets list of IPv6 remote router id.
     *
     * @return list of IPv6 remote router id
     */
    public List<Ip6Address> ipv6RemRouterId() {
        return ipv6RemRouterId;
    }


    /**
     * Sets list of IPv6 remote router id.
     *
     * @param ipv6RemRouterId IPv6 remote router id
     */
    public void setIpv6RemRouterId(List<Ip6Address> ipv6RemRouterId) {
        this.ipv6RemRouterId = ipv6RemRouterId;
    }

    /**
     * Gets list of IPv4 remote router id.
     *
     * @return list of IPv4 remote router id
     */
    public List<Ip4Address> ipv4RemRouterId() {
        return ipv4RemRouterId;
    }

    /**
     * Sets IPv4 remote router id.
     *
     * @param ipv4RemRouterId IPv4 remote router id
     */
    public void setIpv4RemRouterId(List<Ip4Address> ipv4RemRouterId) {
        this.ipv4RemRouterId = ipv4RemRouterId;
    }

    /**
     * Gets list of IPv6 local router id.
     *
     * @return list of IPv6 local router id
     */
    public List<Ip6Address> ipv6LocRouterId() {
        return ipv6LocRouterId;
    }

    /**
     * Sets list of IPv6 local router id.
     *
     * @param ipv6LocRouterId IPv6 local router id
     */
    public void setIpv6LocRouterId(List<Ip6Address> ipv6LocRouterId) {
        this.ipv6LocRouterId = ipv6LocRouterId;
    }

    /**
     * Gets list of IPv4 local router id.
     *
     * @return list of IPv4 local router id
     */
    public List<Ip4Address> ipv4LocRouterId() {
        return ipv4LocRouterId;
    }

    /**
     * Sets list of IPv4 local router id.
     *
     * @param ipv4LocRouterId IPv4 local router id
     */
    public void setIpv4LocRouterId(List<Ip4Address> ipv4LocRouterId) {
        this.ipv4LocRouterId = ipv4LocRouterId;
    }

    /**
     * Gets traffic engineering metric.
     *
     * @return traffic engineering metric
     */
    public Integer teMetric() {
        return teMetric;
    }

    /**
     * Sets traffic engineering metric.
     *
     * @param teMetric Traffic engineering metric
     */
    public void setTeMetric(Integer teMetric) {
        this.teMetric = teMetric;
    }

    /**
     * Gets maximum bandwidth reserved.
     *
     * @return maximum bandwidth reserved
     */
    public Bandwidth maxReserved() {
        return maxReserved;
    }

    /**
     * Sets maximum bandwidth reserved.
     *
     * @param maxReserved maximum bandwidth reserved
     */
    public void setMaxReserved(Bandwidth maxReserved) {
        this.maxReserved = maxReserved;
    }

    /**
     * Gets list of maximum unreserved bandwidth.
     *
     * @return list of maximum unreserved bandwidth
     */
    public List<Bandwidth> maxUnResBandwidth() {
        return maxUnResBandwidth;
    }

    /**
     * Sets ist of maximum unreserved bandwidth.
     *
     * @param bandwidth maximum unreserved bandwidth
     */
    public void setMaxUnResBandwidth(Bandwidth bandwidth) {
        this.maxUnResBandwidth.add(bandwidth);
    }
}