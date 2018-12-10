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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;

import java.util.Optional;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.sendTraceRequestToNode;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.traceRequestString;

/**
 * Requests flow trace command.
 */
@Service
@Command(scope = "onos", name = "openstack-flow-trace",
        description = "Requests flow trace command")
public class OpenstackFlowTraceCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "src ip address", description = "src ip address",
            required = true, multiValued = false)
    @Completion(InstanceIpAddressCompleter.class)
    private String srcIp = null;

    @Argument(index = 1, name = "dst ip address", description = "dst ip address",
            required = true, multiValued = false)
    @Completion(InstanceIpAddressCompleter.class)
    private String dstIp = null;

    private static final String NO_ELEMENT =
                "There's no instance port information with given ip address";
    private static final String FLOW_TRACE_REQUEST_STRING_UPLINK =
                "Flow trace request string for uplink: ";
    private static final String FLOW_TRACE_REQUEST_STRING_DOWNLINK =
                "Flow trace request string for downlink: ";


    @Override
    protected void doExecute() {
        OpenstackNodeAdminService osNodeService = get(OpenstackNodeAdminService.class);
        InstancePortAdminService instancePortService = get(InstancePortAdminService.class);
        OpenstackNetworkAdminService osNetService = get(OpenstackNetworkAdminService.class);

        Optional<InstancePort> srcInstance = instancePortService.instancePorts().stream()
                .filter(port -> port.ipAddress().toString().equals(srcIp)).findAny();

        if (!srcInstance.isPresent()) {
            print(NO_ELEMENT);
            return;
        }

        OpenstackNode srcNode = osNodeService.node(srcInstance.get().deviceId());
        if (srcNode == null || srcNode.sshAuthInfo() == null) {
            log.error("Openstack node {} is null or has no SSH authentication information.\n" +
                            " Please refers to the sample network-cfg.json in " +
                            "OpenstackNode app to push SSH authentication information",
                            srcNode == null ? "" : srcNode.hostname());

            return;
        }

        if (dstIp.equals(osNetService.gatewayIp(srcInstance.get().portId()))) {
            dstIp = srcIp;
        }

        //print uplink flow trace result
        String requestStringUplink = traceRequestString(srcIp, dstIp, srcInstance.get(),
                osNetService, true);

        print(FLOW_TRACE_REQUEST_STRING_UPLINK + requestStringUplink);

        String requestStringDownlink = traceRequestString(srcIp, dstIp, srcInstance.get(),
                osNetService, false);
        print(FLOW_TRACE_REQUEST_STRING_DOWNLINK + requestStringDownlink);

        String traceResult = sendTraceRequestToNode(requestStringUplink + '\n'
                + requestStringDownlink, srcNode);
        print(traceResult);
    }
}
