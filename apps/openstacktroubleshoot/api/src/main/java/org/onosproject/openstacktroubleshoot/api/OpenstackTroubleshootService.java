/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktroubleshoot.api;

import org.onlab.packet.IpAddress;

import java.util.Map;

/**
 * Openstack troubleshoot interface.
 */
public interface OpenstackTroubleshootService {

    /**
     * Checks all east-west VMs' connectivity.
     *
     * @return reachability map
     */
    Map<String, Reachability> probeEastWestBulk();

    /**
     * Checks a single VM-to-Vm connectivity.
     *
     * @param srcNetId  source network ID
     * @param srcIp     source IP address
     * @param dstNetId  destination network ID
     * @param dstIp     destination IP address
     * @return reachability
     */
    Reachability probeEastWest(String srcNetId, IpAddress srcIp,
                               String dstNetId, IpAddress dstIp);

    /**
     * Checks all north-south router to VMs' connectivity.
     *
     * @return reachability map
     */
    Map<String, Reachability> probeNorthSouth();

    /**
     * Checks a single router-to-VM connectivity.
     *
     * @param netId network ID
     * @param ip destination VM IP address
     * @return reachability
     */
    Reachability probeNorthSouth(String netId, IpAddress ip);
}
