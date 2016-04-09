/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.virtualbng;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

/**
 * Provides service of the virtual BNG.
 */
public interface VbngService {

    /**
     * Creates a virtual BNG.
     * <p>
     * It firstly finds out an available local public IP address. Then, it
     * sets up paths between the host configured with private IP and
     * next hop. Finally it returns the public IP address.
     * </p>
     *
     * @param privateIpAddress the private IP address
     * @param hostMacAddress the MAC address for the IP address
     * @param hostName the host name for the IP address
     * @return the public address if a virtual BGN is successfully created,
     *         otherwise return null
     */
    IpAddress createVbng(IpAddress privateIpAddress, MacAddress hostMacAddress,
                         String hostName);

    /**
     * Deletes a virtual BNG.
     *
     * @param privateIpAddress a private IP address
     * @return the public address assigned for this private IP address if the
     *         virtual BGN exists and is deleted, otherwise return null if
     *         there is no vBNG for this private IP address
     */
    IpAddress deleteVbng(IpAddress privateIpAddress);
}
