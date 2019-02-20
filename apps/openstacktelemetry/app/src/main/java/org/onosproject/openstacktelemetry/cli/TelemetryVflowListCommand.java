/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;

import java.util.List;

import static org.onlab.packet.IPv4.PROTOCOL_TCP;
import static org.onlab.packet.IPv4.PROTOCOL_UDP;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.IP_PROTO;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_SRC;
import static org.onosproject.openstacktelemetry.api.Constants.OPENSTACK_TELEMETRY_APP_ID;

/**
 * Lists telemetry vFlows.
 */
@Service
@Command(scope = "onos", name = "telemetry-vflows",
        description = "Lists all Telemetry virtual flows")
public class TelemetryVflowListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-20s%-10s%-20s%-10s%-10s";
    private static final String TCP = "TCP";
    private static final String UDP = "UDP";

    @Override
    protected void doExecute() {
        CoreService coreService = get(CoreService.class);
        FlowRuleService flowService = get(FlowRuleService.class);
        ApplicationId appId = coreService.getAppId(OPENSTACK_TELEMETRY_APP_ID);

        List<FlowEntry> flows =
                Lists.newArrayList(flowService.getFlowEntriesById(appId));

        print(FORMAT, "SrcIp", "SrcPort", "DstIp", "DstPort", "Protocol");

        for (FlowEntry entry : flows) {
            TrafficSelector selector = entry.selector();
            IpPrefix srcIp = ((IPCriterion) selector.getCriterion(IPV4_SRC)).ip();
            IpPrefix dstIp = ((IPCriterion) selector.getCriterion(IPV4_DST)).ip();

            TpPort srcPort = TpPort.tpPort(0);
            TpPort dstPort = TpPort.tpPort(0);
            String protocolStr = "ANY";

            Criterion ipProtocolCriterion = selector.getCriterion(IP_PROTO);

            if (ipProtocolCriterion != null) {
                short protocol = ((IPProtocolCriterion) selector.getCriterion(IP_PROTO)).protocol();

                if (protocol == PROTOCOL_TCP) {
                    srcPort = ((TcpPortCriterion) selector.getCriterion(TCP_SRC)).tcpPort();
                    dstPort = ((TcpPortCriterion) selector.getCriterion(TCP_DST)).tcpPort();
                    protocolStr = TCP;
                }

                if (protocol == PROTOCOL_UDP) {
                    srcPort = ((UdpPortCriterion) selector.getCriterion(UDP_SRC)).udpPort();
                    dstPort = ((UdpPortCriterion) selector.getCriterion(UDP_SRC)).udpPort();
                    protocolStr = UDP;
                }
            }

            print(FORMAT,
                    srcIp.toString(),
                    srcPort.toString(),
                    dstIp.toString(),
                    dstPort.toString(),
                    protocolStr);
        }
    }
}
