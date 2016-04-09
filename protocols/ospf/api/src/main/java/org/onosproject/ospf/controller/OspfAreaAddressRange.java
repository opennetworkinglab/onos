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

/**
 * Represents the collection of IP addresses contained in the address range.
 */
public interface OspfAreaAddressRange {

    /**
     * Gets the IP address.
     *
     * @return IP address
     */
    public Ip4Address ipAddress();

    /**
     * Sets the IP address.
     *
     * @param ipAddress IPv4 address
     */
    public void setIpAddress(Ip4Address ipAddress);

    /**
     * Gets the network mask.
     *
     * @return network mask
     */
    public String mask();

    /**
     * Sets the network mask.
     *
     * @param mask network mask
     */
    public void setMask(String mask);

    /**
     * Gets the advertise value, which indicates routing information is condensed at area boundaries.
     *
     * @return advertise true if advertise flag is set else false
     */
    public boolean isAdvertise();

    /**
     * Sets the advertise value, which indicates routing information is condensed at area boundaries.
     *
     * @param advertise true if advertise flag to set else false
     */
    public void setAdvertise(boolean advertise);
}