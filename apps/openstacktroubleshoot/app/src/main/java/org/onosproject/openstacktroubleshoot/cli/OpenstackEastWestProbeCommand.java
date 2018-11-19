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
package org.onosproject.openstacktroubleshoot.cli;

import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacktroubleshoot.api.OpenstackTroubleshootService;
import org.onosproject.openstacktroubleshoot.api.Reachability;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;

/**
 * Checks the east-west VMs connectivity.
 */
@Service
@Command(scope = "onos", name = "openstack-check-east-west",
        description = "Checks the east-west VMs connectivity")
public class OpenstackEastWestProbeCommand extends AbstractShellCommand {

    private static final String REACHABLE = "Reachable :)";
    private static final String UNREACHABLE = "Unreachable :(";
    private static final String ARROW = "->";

    private static final String FORMAT = "%-20s%-5s%-20s%-20s";

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all VMs",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Argument(index = 0, name = "vmIps", description = "VMs' IP addresses",
            required = false, multiValued = true)
    @Completion(ActiveVmIpCompleter.class)
    private String[] vmIps = null;

    private final ExecutorService probeExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "probe-handler", log));

    @Override
    protected void doExecute() {
        OpenstackTroubleshootService tsService = get(OpenstackTroubleshootService.class);
        InstancePortService instPortService = get(InstancePortService.class);
        MastershipService mastershipService = get(MastershipService.class);
        ClusterService clusterService = get(ClusterService.class);
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);

        if (tsService == null) {
            error("Failed to troubleshoot openstack networking.");
            return;
        }

        if ((!isAll && vmIps == null) || (isAll && vmIps != null)) {
            print("Please specify one of VM IP address or -a option.");
            return;
        }

        NodeId localNodeId = clusterService.getLocalNode().id();

        for (OpenstackNode node : osNodeService.completeNodes(COMPUTE)) {
            if (!localNodeId.equals(mastershipService.getMasterFor(node.intgBridge()))) {
                error("Current node is not the master for all compute nodes. " +
                        "Please enforce mastership first using openstack-reset-mastership -c !");
                return;
            }
        }

        if (isAll) {
            printHeader();
            // send ICMP PACKET_OUT to all connect VMs whose instance port state is ACTIVE
            Set<InstancePort> activePorts = instPortService.instancePorts().stream()
                    .filter(p -> p.state() == ACTIVE)
                    .collect(Collectors.toSet());

            activePorts.forEach(srcPort ->
                    activePorts.forEach(dstPort ->
                            printReachability(tsService.probeEastWest(srcPort, dstPort))
                    )
            );
        } else {
            if (vmIps.length > 2) {
                print("Too many VM IPs. The number of IP should be limited to 2.");
                return;
            }

            IpAddress srcIp = getIpAddress(vmIps[0]);

            if (srcIp == null) {
                return;
            }

            InstancePort srcPort = instPort(instPortService, srcIp);

            if (srcPort == null) {
                print("Specified source IP is not existing.");
                return;
            }

            final Set<IpAddress> dstIps = Sets.newConcurrentHashSet();

            if (vmIps.length == 2) {
                IpAddress dstIp = getIpAddress(vmIps[1]);

                if (dstIp == null) {
                    return;
                }

                dstIps.add(dstIp);
            }

            if (vmIps.length == 1) {
                dstIps.addAll(instPortService.instancePorts().stream()
                        .filter(p -> !p.ipAddress().equals(srcIp))
                        .filter(p -> p.state().equals(InstancePort.State.ACTIVE))
                        .map(InstancePort::ipAddress)
                        .collect(Collectors.toSet()));
            }

            printHeader();
            dstIps.stream()
                    .filter(ip -> instPort(instPortService, ip) != null)
                    .map(ip -> instPort(instPortService, ip))
                    .forEach(port -> probeExecutor.execute(() ->
                            printReachability(tsService.probeEastWest(srcPort, port))));
        }
    }

    private InstancePort instPort(InstancePortService service, IpAddress ip) {
        Optional<InstancePort> port = service.instancePorts().stream()
                .filter(p -> p.ipAddress().equals(ip)).findFirst();

        if (port.isPresent()) {
            return port.get();
        } else {
            print("Specified destination IP is not existing.");
            return null;
        }
    }

    private void printHeader() {
        print(FORMAT, "Source IP", "", "Destination IP", "Reachability");
    }

    private void printReachability(Reachability r) {
        String result = r.isReachable() ? REACHABLE : UNREACHABLE;
        print(FORMAT, r.srcIp().toString(), ARROW, r.dstIp().toString(), result);
    }

    private IpAddress getIpAddress(String ipString) {
        try {
            return IpAddress.valueOf(vmIps[0]);
        } catch (IllegalArgumentException e) {
            error("Invalid IP address string.");
            return null;
        }
    }
}
