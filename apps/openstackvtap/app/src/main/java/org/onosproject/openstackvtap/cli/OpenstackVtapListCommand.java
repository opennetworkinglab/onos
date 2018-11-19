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
package org.onosproject.openstackvtap.cli;

import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapService;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;

/**
 * Lists openstack vtap rules.
 */
@Service
@Command(scope = "onos", name = "openstack-vtap-list",
        description = "OpenstackVtap list")
public class OpenstackVtapListCommand extends AbstractShellCommand {

    private final OpenstackVtapService vtapService = get(OpenstackVtapService.class);
    private final OpenstackNodeService osNodeService = get(OpenstackNodeService.class);

    @Argument(index = 0, name = "type",
            description = "vtap type [any|all|rx|tx]",
            required = false, multiValued = false)
    @Completion(VtapTypeCompleter.class)
    String vtapType = "any";

    private static final String FORMAT = "ID { %s }: type [%s], srcIP [%s], dstIP [%s]";
    private static final String FORMAT_TX_NODES = "   tx openstack nodes: %s";
    private static final String FORMAT_RX_NODES = "   rx openstack nodes: %s";

    @Override
    protected void doExecute() {
        OpenstackVtap.Type type = getVtapTypeFromString(vtapType);
        Set<OpenstackVtap> openstackVtaps = vtapService.getVtaps(type);
        for (OpenstackVtap vtap : openstackVtaps) {
            print(FORMAT,
                    vtap.id().toString(),
                    vtap.type().toString(),
                    vtap.vtapCriterion().srcIpPrefix().toString(),
                    vtap.vtapCriterion().dstIpPrefix().toString());
            print(FORMAT_TX_NODES, osNodeNames(vtap.txDeviceIds()));
            print(FORMAT_RX_NODES, osNodeNames(vtap.rxDeviceIds()));
        }
    }

    private Set<String> osNodeNames(Set<DeviceId> deviceIds) {
        if (deviceIds == null) {
            return ImmutableSet.of();
        } else {
            return deviceIds.parallelStream()
                    .map(osNodeService::node)
                    .filter(Objects::nonNull)
                    .map(OpenstackNode::hostname)
                    .collect(Collectors.toSet());
        }
    }

}
