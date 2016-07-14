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
import org.onosproject.ospf.controller.OspfDeviceTed;

import java.util.List;

/**
 * Representation of an OSPF device Traffic Engineering details.
 */
public class OspfDeviceTedImpl implements OspfDeviceTed {

    List<Ip4Address> ipv4RouterIds;
    List<Ip6Address> ipv6RouterIds;
    List<Short> topologyIds;
    Boolean asbr;
    Boolean abr;

    /**
     * Gets list of IPv4 router id.
     *
     * @return list of IPv4 router id
     */
    public List<Ip4Address> ipv4RouterIds() {
        return ipv4RouterIds;
    }

    @Override
    public void setIpv4RouterIds(List<Ip4Address> ipv4RouterIds) {
        this.ipv4RouterIds = ipv4RouterIds;
    }

    /**
     * Gets if router is area border router or not.
     *
     * @return true if it is area border router else false
     */
    public Boolean abr() {
        return abr;
    }

    @Override
    public void setAbr(Boolean abr) {
        this.abr = abr;
    }

    /**
     * Gets if router is autonomous system border router or not.
     *
     * @return true or false
     */
    public Boolean asbr() {
        return asbr;
    }

    @Override
    public void setAsbr(Boolean asbr) {
        this.asbr = asbr;
    }

    /**
     * Gets list of topology id's.
     *
     * @return list of topology id's
     */
    public List<Short> topologyIds() {
        return topologyIds;
    }

    @Override
    public void setTopologyIds(List<Short> topologyIds) {
        this.topologyIds = topologyIds;
    }

    /**
     * Gets list of ipv6 router id's.
     *
     * @return list of ipv6 router id's
     */
    public List<Ip6Address> ipv6RouterIds() {
        return ipv6RouterIds;
    }

    @Override
    public void setIpv6RouterIds(List<Ip6Address> ipv6RouterIds) {
        this.ipv6RouterIds = ipv6RouterIds;
    }
}