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
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtnrsc.SegmentationId;

/**
 * Applies classifier flows to the device. Classifier table is Table(0).
 */
public interface ClassifierService {

    /**
     * The port rule that message from host matches Table(0) Match: host mac and
     * ingress port Action: set vnid and go to L2Forward Table(50).
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param inPort the ingress port of the host
     * @param srcMac the mac of the host
     * @param appId the application ID of the vtn
     * @param type the operation of the flow
     */
    void programLocalIn(DeviceId deviceId, SegmentationId segmentationId,
                        PortNumber inPort, MacAddress srcMac,
                        ApplicationId appId, Objective.Operation type);

    /**
     * The port rule that message from tunnel Table(0) Match: tunnel port and
     * vnid Action: go to L2Forward Table(50).
     *
     * @param deviceId Device Id
     * @param segmentationId the vnid of the host belong to
     * @param localTunnelPorts the tunnel pors of the device
     * @param type the operation of the flow
     */
    void programTunnelIn(DeviceId deviceId, SegmentationId segmentationId,
                         Iterable<PortNumber> localTunnelPorts,
                         Objective.Operation type);

    /**
     * Assemble the L3 Classifier table rules which are sended from external port.
     * Match: ipv4 type, ingress port and destination ip.
     * Action: go to DNAT Table(20).
     *
     * @param deviceId Device Id
     * @param inPort external port
     * @param dstIp floating ip
     * @param type the operation type of the flow rules
     */
    void programL3ExPortClassifierRules(DeviceId deviceId, PortNumber inPort,
                                        IpAddress dstIp,
                                        Objective.Operation type);

    /**
     * Assemble the L3 Classifier table rules which are sended from internal port.
     * Match: ingress port, source mac and destination mac.
     * Action: set vnid and go to L3Forward Table(30).
     *
     * @param deviceId Device Id
     * @param inPort the ingress port of the host
     * @param srcMac source mac
     * @param dstMac destination vm gateway mac
     * @param actionVni the vni of L3 network
     * @param type the operation type of the flow rules
     */
    void programL3InPortClassifierRules(DeviceId deviceId,
                                          PortNumber inPort, MacAddress srcMac,
                                          MacAddress dstMac,
                                          SegmentationId actionVni,
                                          Objective.Operation type);

    /**
     * Assemble the Arp Classifier table rules.
     * Match: arp type and destination ip.
     * Action: set vnid and go to ARP Table(10).
     *
     * @param deviceId Device Id
     * @param dstIp source gateway ip
     * @param actionVni the vni of the source network (l2vni)
     * @param type the operation type of the flow rules
     */
    void programArpClassifierRules(DeviceId deviceId, IpAddress dstIp,
                                   SegmentationId actionVni,
                                   Objective.Operation type);

    /**
     * Assemble the Arp Classifier table rules.
     * Match: arp type and destination ip.
     * Action: set vnid and go to ARP Table(10).
     *
     * @param deviceId Device Id
     * @param inPort the ingress port of the host
     * @param dstIp source gateway ip
     * @param actionVni the vni of the source network (l2vni)
     * @param type the operation type of the flow rules
     */
    void programArpClassifierRules(DeviceId deviceId, PortNumber inPort,
                                   IpAddress dstIp, SegmentationId actionVni,
                                   Objective.Operation type);

    /**
     * Assemble the Userdata Classifier table rules.
     * Match: subnet ip prefix and destination ip.
     * Action: add flow rule to specific ip for userdata.
     *
     * @param deviceId Device Id
     * @param ipPrefix source ip prefix
     * @param dstIp userdata ip
     * @param dstmac dst mac
     * @param actionVni the vni of the source network (l2vni)
     * @param type the operation type of the flow rules
     */
    void programUserdataClassifierRules(DeviceId deviceId, IpPrefix ipPrefix,
                                        IpAddress dstIp, MacAddress dstmac,
                                        SegmentationId actionVni,
                                        Objective.Operation type);

    /**
     * Assemble the export port Arp Classifier table rules.
     * Match: export port.
     * Action: upload packet to controller.
     *
     * @param exportPort export port of ovs
     * @param deviceId Device Id
     * @param type the operation type of the flow rules
     */
    void programExportPortArpClassifierRules(Port exportPort, DeviceId deviceId,
                                             Operation type);
}
