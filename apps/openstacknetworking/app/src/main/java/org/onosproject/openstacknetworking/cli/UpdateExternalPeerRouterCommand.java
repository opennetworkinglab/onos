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

import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;

import java.util.List;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Updates external peer router.
 */
@Service
@Command(scope = "onos", name = "openstack-update-peer-router",
        description = "Update external peer router")
public class UpdateExternalPeerRouterCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ip address", description = "ip address",
            required = true, multiValued = false)
    @Completion(IpAddressCompleter.class)
    private String ipAddress = null;

    @Argument(index = 1, name = "mac address", description = "mac address",
            required = true, multiValued = false)
    @Completion(MacAddressCompleter.class)
    private String macAddress = null;

    @Argument(index = 2, name = "vlan id", description = "vlan id",
            required = true, multiValued = false)
    @Completion(VlanIdCompleter.class)
    private String vlanId = null;

    private static final String FORMAT = "%-20s%-20s%-20s";
    private static final String NO_ELEMENT =
            "There's no external peer router information with given ip address";
    private static final String NONE = "None";

    @Override
    protected void doExecute() {
        OpenstackNetworkAdminService service = get(OpenstackNetworkAdminService.class);

        IpAddress externalPeerIpAddress = IpAddress.valueOf(
                IpAddress.Version.INET, Ip4Address.valueOf(ipAddress).toOctets());

        if (service.externalPeerRouters().isEmpty()) {
            print(NO_ELEMENT);
            return;
        } else if (service.externalPeerRouters().stream()
                .noneMatch(router -> router.ipAddress().toString().equals(ipAddress))) {
            print(NO_ELEMENT);
            return;
        }
        try {
            if (vlanId.equals(NONE)) {
                service.updateExternalPeerRouter(externalPeerIpAddress,
                        MacAddress.valueOf(macAddress),
                        VlanId.NONE);

            } else {
                service.updateExternalPeerRouter(externalPeerIpAddress,
                        MacAddress.valueOf(macAddress),
                        VlanId.vlanId(vlanId));
            }
        } catch (IllegalArgumentException e) {
            log.error("Exception occurred because of {}", e.toString());
        }


        print(FORMAT, "Router IP", "Mac Address", "VLAN ID");
        List<ExternalPeerRouter> routers = Lists.newArrayList(service.externalPeerRouters());

        for (ExternalPeerRouter router: routers) {
            print(FORMAT, router.ipAddress(),
                    router.macAddress().toString(),
                    router.vlanId());
        }
    }
}

