/*
 * Copyright 2015 Open Networking Laboratory
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
 * SnatService interface provides the rules in SNAT table which is Table(40) for ovs pipeline.
 * SNAT means Source Network Address Translation, it is acronym for network terminology.
 * Handle the upward flows.
 */
public interface SnatService {

    /**
     * Assemble the SNAT table rules.
     * Match: ipv4 type, vnid and source ip.
     * Action: set eth_src, set eth_dst, set ip_src, set vnid and goto L2Forward Table(50).
     *
     * @param deviceId Device Id
     * @param matchVni the vni of L3 network
     * @param srcIP source ip
     * @param ethDst external gateway mac
     * @param ethSrc external port mac
     * @param ipSrc floating ip
     * @param actionVni external network VNI
     * @param type the operation type of the flow rules
     */
    void programRules(DeviceId deviceId, SegmentationId matchVni,
                          IpAddress srcIP, MacAddress ethDst,
                          MacAddress ethSrc, IpAddress ipSrc,
                          SegmentationId actionVni, Objective.Operation type);
}
