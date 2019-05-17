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
package org.onosproject.openstacknetworking.cli;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Port;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.ifaceNameFromOsPortId;

/**
 * Shows the list of the openvswitch ports detached from real interfaces.
 */
@Service
@Command(scope = "onos", name = "openstack-detached-ports",
        description = "Shows the detached VM's tap port list.")
public class OpenstackDetachedPortListCommand extends AbstractShellCommand {

    private static final String PORT_NAME = "portName";
    private static final String FORMAT = "%-25s%-25s%-25s";

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all nodes",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Argument(index = 0, name = "hostnames", description = "Hostname(s) to apply this command",
            required = false, multiValued = true)
    @Completion(OpenstackComputeNodeCompleter.class)
    private String[] hostnames = null;

    @Override
    protected void doExecute() {
        OpenstackNodeService nodeService = get(OpenstackNodeService.class);
        OpenstackNetworkService networkService = get(OpenstackNetworkService.class);
        DeviceService deviceService = get(DeviceService.class);

        if (isAll) {
            hostnames = nodeService.completeNodes().stream()
                    .map(OpenstackNode::hostname).toArray(String[]::new);
        }

        if (hostnames == null) {
            print("Please specify one of hostname or --all options.");
            return;
        }

        print(FORMAT, "Hostname", "Integration Bridge", "Detached Port");

        for (String hostname : hostnames) {
            networkService.ports().forEach(p -> {
                if (hostname.equals(p.getHostId())) {
                    OpenstackNode osNode = nodeService.node(p.getHostId());
                    if (osNode != null) {
                        Set<String> detachedPortNames =
                                detachedOvsPort(p,
                                        deviceService.getPorts(osNode.intgBridge()));
                        detachedPortNames.forEach(dp ->
                            print(FORMAT, hostname, osNode.intgBridge().toString(), dp)
                        );
                    }
                }
            });
        }
    }

    private Set<String> detachedOvsPort(Port osPort,
                                        List<org.onosproject.net.Port> ovsPorts) {
        Set<String> portNames = ovsPorts.stream()
                .filter(ovsPort -> ovsPort.annotations() != null ||
                        ovsPort.annotations().keys().contains(PORT_NAME))
                .map(ovsPort -> ovsPort.annotations().value(PORT_NAME))
                .collect(Collectors.toSet());

        String tapPort = ifaceNameFromOsPortId(osPort.getId());
        Set<String> detachedPortNames = Sets.newConcurrentHashSet();

        if (!portNames.contains(tapPort)) {
            detachedPortNames.add(tapPort);
        }

        return ImmutableSet.copyOf(detachedPortNames);
    }
}
