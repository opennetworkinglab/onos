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
 * L3ForwardService interface provide the rules in L3Forward table which is Table(30).
 */
public interface L3ForwardService {

    /**
     * Assemble the L3Forward table rules.
     * Match: ipv4 type, vnid and destination ip.
     * Action: set eth_src, set eth_dst, set vnid and goto L2Forward Table(50).
     *
     * @param deviceId Device Id
     * @param l3Vni the vni of L3 network
     * @param dstVmIP destination vm ip
     * @param dstVni the vni of the destination network (l2vni)
     * @param dstVmGwMac destination VM gateway mac
     * @param dstVmMac destination VM mac
     * @param type the operation type of the flow rules
     */
    void programRouteRules(DeviceId deviceId, SegmentationId l3Vni,
                           IpAddress dstVmIP, SegmentationId dstVni,
                           MacAddress dstVmGwMac, MacAddress dstVmMac,
                           Objective.Operation type);

}
