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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacktelemetry.api.DefaultStatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRuleAdminService;

import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getProtocolTypeFromString;

/**
 * Adds vFlow telemetry rule.
 */
@Service
@Command(scope = "onos", name = "telemetry-add-vflow",
        description = "Adds a telemetry virtual flow rule")
public class TelemetryVflowAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "Source IP", description = "Source IP address",
            required = true, multiValued = false)
    private String srcIp = null;

    @Argument(index = 1, name = "Source port", description = "Source port number",
            required = true, multiValued = false)
    private String srcTpPort = null;

    @Argument(index = 2, name = "Destination IP", description = "Destination IP address",
            required = true, multiValued = false)
    private String dstIp = null;

    @Argument(index = 3, name = "Destination port", description = "Destination port number",
            required = true, multiValued = false)
    private String dstTpPort = null;

    @Argument(index = 4, name = "IP protocol", description = "IP protocol (TCP/UDP/ANY)",
            required = true, multiValued = false)
    private String ipProto = null;

    @Override
    protected void doExecute() {
        StatsFlowRuleAdminService statsService = get(StatsFlowRuleAdminService.class);

        StatsFlowRule statsFlowRule = DefaultStatsFlowRule.builder()
                .srcIpPrefix(IpPrefix.valueOf(srcIp))
                .dstIpPrefix(IpPrefix.valueOf(dstIp))
                .srcTpPort(TpPort.tpPort(Integer.valueOf(srcTpPort)))
                .dstTpPort(TpPort.tpPort(Integer.valueOf(dstTpPort)))
                .ipProtocol(getProtocolTypeFromString(ipProto))
                .build();

        statsService.createStatFlowRule(statsFlowRule);

        print("Added the stat flow rule.");
    }
}
