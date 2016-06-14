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

import java.util.List;

/**
 * Represents Device Traffic Engineering parameters.
 */
public interface OspfDeviceTed {

    /**
     * Obtain list of IPv4 router ids.
     *
     * @return IPv4 router ids
     */
    public List<Ip4Address> ipv4RouterIds();

    /**
     * Sets list of IPv4 router ids.
     *
     * @param routerIds list of IPv4 router ids
     */
    public void setIpv4RouterIds(List<Ip4Address> routerIds);

    /**
     * Obtain list of IPv6 router id.
     *
     * @return IPv4 router ids
     */
    public List<Ip6Address> ipv6RouterIds();

    /**
     * Sets list of IPv4 router ids.
     *
     * @param routerIds list of IPv4 router ids
     */
    public void setIpv6RouterIds(List<Ip6Address> routerIds);

    /**
     * Obtain the list of topology ids.
     *
     * @return list of topology ids
     */
    public List<Short> topologyIds();

    /**
     * Sets the list of topology ids.
     *
     * @param topologyIds the list of topology ids
     */
    public void setTopologyIds(List<Short> topologyIds);

    /**
     * Obtains position of device in the network.
     *
     * @return position of device in the network
     */
    public Boolean asbr();

    /**
     * Sets position of device in the network.
     *
     * @param asbr position of device in the network
     */
    public void setAsbr(Boolean asbr);

    /**
     * Obtains position of device in the network.
     *
     * @return position of device in the network
     */
    public Boolean abr();

    /**
     * Sets position of device in the network.
     *
     * @param abr position of device in the network
     */
    public void setAbr(Boolean abr);
}