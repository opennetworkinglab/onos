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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
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
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.openstack4j.model.network.Port;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getOvsdbClient;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.ifaceNameFromOsPortId;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.ovsdb.rfc.table.Interface.InterfaceColumn.EXTERNALIDS;

/**
 * Recovers the openvswitch tap ports.
 */
@Service
@Command(scope = "onos", name = "openstack-recover-ports",
        description = "Recovers VM's tap ports detached from OpenvSwitch.")
public class OpenstackRecoverPortsCommand extends AbstractShellCommand {

    private static final int OVS_DB_PORT = 6640;

    private static final String ATTACHED_MAC = "attached-mac";
    private static final String IFACE_ID = "iface-id";
    private static final String IFACE_STATUS = "iface-status";
    private static final String VM_ID = "vm-id";

    private static final String PORT_NAME = "portName";

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all nodes",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Argument(index = 0, name = "hostnames", description = "Hostname(s) to apply this command",
            required = false, multiValued = true)
    @Completion(OpenstackComputeNodeCompleter.class)
    private String[] hostnames = null;

    @Override
    protected void doExecute() {
        OvsdbController controller = get(OvsdbController.class);
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

        for (String hostname : hostnames) {
            networkService.ports().forEach(p -> {
                if (hostname.equals(p.getHostId())) {
                    OpenstackNode osNode = nodeService.node(p.getHostId());
                    if (osNode != null) {
                        Set<String> recoveredPortNames =
                                recoverOvsPort(controller, OVS_DB_PORT, osNode, p,
                                        deviceService.getPorts(osNode.intgBridge()));
                        recoveredPortNames.forEach(pn -> print(pn + " is recovered!"));
                    }
                }
            });
        }
    }

    /**
     * Recovers the openvswitch port from conf.db corruption.
     *
     * @param controller    ovsdb controller
     * @param ovsdbPort     ovsdb port number
     * @param node          openstack node
     * @param osPort        an openstack port
     * @param ovsPorts      set of openvswitch ports
     *
     * @return a set of recovered port name
     */
    private Set<String> recoverOvsPort(OvsdbController controller, int ovsdbPort,
                                       OpenstackNode node, Port osPort,
                                       List<org.onosproject.net.Port> ovsPorts) {
        OvsdbClientService client = getOvsdbClient(node, ovsdbPort, controller);

        if (client == null) {
            return ImmutableSet.of();
        }

        Set<String> portNames = ovsPorts.stream()
                .filter(ovsPort -> ovsPort.annotations() != null ||
                        ovsPort.annotations().keys().contains(PORT_NAME))
                .map(ovsPort -> ovsPort.annotations().value(PORT_NAME))
                .collect(Collectors.toSet());

        String tapPort = ifaceNameFromOsPortId(osPort.getId());
        Set<String> recoveredPortNames = Sets.newConcurrentHashSet();
        if (!portNames.contains(tapPort)) {
            Map<String, String> extIdMap =
                    ImmutableMap.of(ATTACHED_MAC, osPort.getMacAddress(),
                            IFACE_ID, osPort.getId(), IFACE_STATUS,
                            StringUtils.lowerCase(osPort.getState().name()),
                            VM_ID, osPort.getDeviceId());

            OvsdbInterface ovsIface = OvsdbInterface.builder()
                    .name(tapPort)
                    .options(ImmutableMap.of())
                    .data(ImmutableMap.of(EXTERNALIDS, extIdMap))
                    .build();
            client.createInterface(INTEGRATION_BRIDGE, ovsIface);
            recoveredPortNames.add(tapPort);
        }
        return ImmutableSet.copyOf(recoveredPortNames);
    }
}
