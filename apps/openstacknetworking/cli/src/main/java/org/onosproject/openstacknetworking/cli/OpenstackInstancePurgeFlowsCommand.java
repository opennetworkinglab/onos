/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostService;

import org.onosproject.openstacknetworking.OpenstackSwitchingService;
import org.onosproject.openstacknetworking.OpenstackSecurityGroupService;
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.onosproject.openstacknetworking.OpenstackFloatingIpService;

import static org.onosproject.openstacknetworking.Constants.*;

/**
 * Purge Flows of OpenstackInstance Data Plane.
 */

@Command(scope = "onos", name = "openstack-purge-flows",
        description = "Purge data plane flows of existing VM.")
public class OpenstackInstancePurgeFlowsCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--all",
            description = "HostIDs are all existing VM",
            required = false, multiValued = false)
    private Boolean allhost = false;

    @Argument(index = 0, name = "hostId", description = "HostID(s)",
            required = false, multiValued = true)
    private String[] hostids = null;

    @Override
    protected void execute() {
         HostService hostService = AbstractShellCommand.get(HostService.class);

         OpenstackSwitchingService switchingService = getService(OpenstackSwitchingService.class);
         OpenstackSecurityGroupService sgService = getService(OpenstackSecurityGroupService.class);
         OpenstackRoutingService routingService = getService(OpenstackRoutingService.class);
         OpenstackFloatingIpService floatingIpService = getService(OpenstackFloatingIpService.class);

         if (allhost) {
            switchingService.purgeVmFlow(null);
            sgService.purgeVmFlow(null);
            routingService.purgeVmFlow(null);
            floatingIpService.purgeVmFlow(null);

            hostService.getHosts().forEach(host -> {
                printHost(host);
            });
         } else if (hostids != null) {
            for (String hostid : hostids) {
                Host host = hostService.getHost(HostId.hostId(hostid));
                if (host == null) {
                    continue;
                }
                switchingService.purgeVmFlow(host);
                sgService.purgeVmFlow(host);
                routingService.purgeVmFlow(host);
                floatingIpService.purgeVmFlow(host);
                printHost(host);
            }
         }
    }

    private void printHost(Host host) {
        print("Purge data plane flows of VM(hostid=%s, vni=%s, ip=%s, mac=%s).",
              host.id(), host.annotations().value(VXLAN_ID),
              host.ipAddresses().stream().findFirst().get().getIp4Address(), host.mac());
    }
}
