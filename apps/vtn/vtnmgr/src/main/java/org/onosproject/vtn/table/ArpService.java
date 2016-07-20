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
package org.onosproject.vtn.table;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtnrsc.SegmentationId;


/**
 * ArpService interface providing the rules in ARP table which is Table(10).
 */
public interface ArpService {

    /**
     * Assemble the arp rules.
     * Match: arp type, vnid and destination ip.
     * Action: set arp_operation, move arp_eth_src to arp_eth_dst, set arp_eth_src,
     * move arp_ip_src to arp_ip_dst, set arp_ip_src, set output port.
     *
     * @param hander DriverHandler
     * @param deviceId Device Id
     * @param dstIP destination ip
     * @param matchVni the vni of the source network (l2vni)
     * @param dstMac destination mac
     * @param type the operation type of the flow rules
     */
    void programArpRules(DriverHandler hander, DeviceId deviceId, IpAddress dstIP,
                                SegmentationId matchVni, MacAddress dstMac,
                                Objective.Operation type);
}
