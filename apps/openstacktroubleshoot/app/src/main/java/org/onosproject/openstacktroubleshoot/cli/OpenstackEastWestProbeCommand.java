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
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacktroubleshoot.api.OpenstackTroubleshootService;
import org.onosproject.openstacktroubleshoot.api.Reachability;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks the east-west VMs connectivity.
 */
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
    private String[] vmIps = null;

    @Override
    protected void execute() {
        OpenstackTroubleshootService tsService =
                get(OpenstackTroubleshootService.class);

        InstancePortService instPortService =
                get(InstancePortService.class);

        if (tsService == null) {
            error("Failed to troubleshoot openstack networking.");
            return;
        }

        if ((!isAll && vmIps == null) || (isAll && vmIps != null)) {
            print("Please specify one of VM IP address or -a option.");
            return;
        }

        if (isAll) {
            printHeader();
            Map<String, Reachability> map = tsService.probeEastWestBulk();
            map.values().forEach(this::printReachability);
        } else {
            if (vmIps.length > 2) {
                print("Too many VM IPs. The number of IP should be limited to 2.");
                return;
            }

            IpAddress srcIp = IpAddress.valueOf(vmIps[0]);
            InstancePort srcPort = instPort(instPortService, srcIp);

            if (srcPort == null) {
                print("Specified source IP is not existing.");
                return;
            }

            final Set<IpAddress> dstIps = Sets.newConcurrentHashSet();

            if (vmIps.length == 2) {
                dstIps.add(IpAddress.valueOf(vmIps[1]));
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
                    .forEach(port -> printReachability(tsService.probeEastWest(srcPort, port)));
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
}
