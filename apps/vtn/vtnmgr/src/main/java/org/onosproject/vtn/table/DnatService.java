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
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtnrsc.SegmentationId;

/**
 * DnatService interface provides the rules in DNAT table which is Table(20) for ovs pipeline.
 * DNAT means Destination Network Address Translation, it is acronym for network terminology.
 * Handle the downward flows.
 */
public interface DnatService {

    /**
     * Assemble the DNAT table rules.
     * Match: ipv4 type and destination ip.
     * Action: set eth_src, set ip_dst, set vnid and goto L3Forward Table(30).
     *
     * @param deviceId Device Id
     * @param dstIp floating ip
     * @param ethSrc floating ip gateway mac
     * @param ipDst destination vm ip
     * @param actionVni the vni of L3 network
     * @param type the operation type of the flow rules
     */
    void programRules(DeviceId deviceId, IpAddress dstIp,
                          MacAddress ethSrc, IpAddress ipDst,
                          SegmentationId actionVni, Objective.Operation type);
}
