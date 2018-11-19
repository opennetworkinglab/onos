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

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.InstancePort.State.ACTIVE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Checks the north-south VM connectivity.
 */
@Service
@Command(scope = "onos", name = "openstack-check-north-south",
        description = "Checks the north-south VMs connectivity")
public class OpenstackNorthSouthProbeCommand extends AbstractShellCommand {

    private static final String REACHABLE = "Reachable :)";
    private static final String UNREACHABLE = "Unreachable :(";
    private static final String ARROW = "->";

    private static final String FORMAT = "%-20s%-5s%-20s%-20s";

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all VMs",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Argument(index = 0, name = "vmIps", description = "VMs' IP addresses",
            required = false, multiValued = true)
    @Completion(ActiveFloatingIpCompleter.class)
    private String[] vmIps = null;

    private final ExecutorService probeExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "probe-handler", log));

    @Override
    protected void doExecute() {
        OpenstackTroubleshootService tsService = get(OpenstackTroubleshootService.class);
        InstancePortService instPortService = get(InstancePortService.class);
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);
        MastershipService mastershipService = get(MastershipService.class);
        ClusterService clusterService = get(ClusterService.class);

        if (tsService == null || osNodeService == null ||
                instPortService == null || mastershipService == null) {
            error("Failed to troubleshoot openstack networking.");
            return;
        }

        if ((!isAll && vmIps == null) || (isAll && vmIps != null)) {
            print("Please specify one of VM IP address or -a option.");
            return;
        }

        NodeId localNodeId = clusterService.getLocalNode().id();

        for (OpenstackNode gw : osNodeService.completeNodes(GATEWAY)) {
            if (!localNodeId.equals(mastershipService.getMasterFor(gw.intgBridge()))) {
                error("Current node is not the master for all gateway nodes. " +
                        "Please enforce mastership first using openstack-reset-mastership -c !");
                return;
            }
        }

        if (isAll) {
            printHeader();

            // send ICMP PACKET_OUT to all connect VMs whose instance port state is ACTIVE
            instPortService.instancePorts().stream()
                    .filter(p -> p.state() == ACTIVE)
                    .filter(p -> instPortService.floatingIp(p.portId()) != null)
                    .forEach(port -> printReachability(tsService.probeNorthSouth(port)));
        } else {

            final Set<InstancePort> ports = Sets.newConcurrentHashSet();

            for (String ip : vmIps) {
                instPortService.instancePorts().stream()
                        .filter(p -> p.state().equals(InstancePort.State.ACTIVE))
                        .filter(p -> instPortService.floatingIp(p.portId()) != null)
                        .filter(p -> ip.equals(instPortService.floatingIp(p.portId()).toString()))
                        .forEach(ports::add);
            }

            printHeader();
            ports.forEach(port -> probeExecutor.execute(() ->
                    printReachability(tsService.probeNorthSouth(port))));
        }
    }

    private void printHeader() {
        print(FORMAT, "Source IP", "", "Destination IP", "Reachability");
    }

    private void printReachability(Reachability r) {
        if (r == null) {
            return;
        }
        String result = r.isReachable() ? REACHABLE : UNREACHABLE;
        print(FORMAT, r.srcIp().toString(), ARROW, r.dstIp().toString(), result);
    }
}
