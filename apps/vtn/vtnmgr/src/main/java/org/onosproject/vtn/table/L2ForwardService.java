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
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtnrsc.SegmentationId;

/**
 * Applies L2 flows to the device. L2Forward table is Table(50).
 */
public interface L2ForwardService {

    /**
     * The local broadcast rule that message matches Table(50).
     * Match: broadcast mac and vnid.
     * Action: set output port.
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param inPort the ingress port of the host
     * @param localVmPorts the local ports of the network which connect host
     * @param localTunnelPorts the tunnel pors of the device
     * @param type the operation of the flow
     */
    void programLocalBcastRules(DeviceId deviceId,
                                SegmentationId segmentationId,
                                PortNumber inPort,
                                Iterable<PortNumber> localVmPorts,
                                Iterable<PortNumber> localTunnelPorts,
                                Objective.Operation type);

    /**
     * The tunnel broadcast rule that message matches Table(50).
     * Match: broadcast mac and vnid.
     * Action: output port.
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param localVmPorts the local ports of the network which connect host
     * @param localTunnelPorts the tunnel pors of the device
     * @param type the operation of the flow
     */
    void programTunnelBcastRules(DeviceId deviceId,
                                 SegmentationId segmentationId,
                                 Iterable<PortNumber> localVmPorts,
                                 Iterable<PortNumber> localTunnelPorts,
                                 Objective.Operation type);

    /**
     * The local out rule that message matches Table(50).
     * Match: local host mac and vnid.
     * Action: output local host port.
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param outPort the ingress port of the host
     * @param sourceMac the mac of the host
     * @param type the operation of the flow
     */
    void programLocalOut(DeviceId deviceId, SegmentationId segmentationId,
                         PortNumber outPort, MacAddress sourceMac,
                         Objective.Operation type);

    /**
     * The external out rule that message matches Table(50).
     * Match: external port mac and vnid.
     * Action: output external port.
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param outPort the ingress port of the external port
     * @param sourceMac the mac of the external port
     * @param type the operation of the flow
     */
    void programExternalOut(DeviceId deviceId, SegmentationId segmentationId,
                         PortNumber outPort, MacAddress sourceMac,
                         Objective.Operation type);

    /**
     * The tunnel out rule that message matches Table(50).
     * Match: host mac and vnid.
     * Action: output tunnel port.
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param tunnelOutPort the port of the tunnel
     * @param dstMac the mac of the host
     * @param type the operation of the flow
     * @param ipAddress the ipAddress of the node
     */
    void programTunnelOut(DeviceId deviceId, SegmentationId segmentationId,
                          PortNumber tunnelOutPort, MacAddress dstMac,
                          Objective.Operation type, IpAddress ipAddress);

}
