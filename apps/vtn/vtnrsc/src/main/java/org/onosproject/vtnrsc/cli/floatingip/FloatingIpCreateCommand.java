/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.cli.floatingip;

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIp.Status;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;

import com.google.common.collect.Sets;

/**
 * Supports for create a floating IP.
 */
@Command(scope = "onos", name = "floatingip-create",
        description = "Supports for creating a floating IP")
public class FloatingIpCreateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "id", description = "The floating IP identifier",
            required = true, multiValued = false)
    String id = null;

    @Argument(index = 1, name = "networkId", description = "The network identifier of floating IP",
            required = true, multiValued = false)
    String networkId = null;

    @Argument(index = 2, name = "tenantId", description = "The tenant identifier of floating IP",
            required = true, multiValued = false)
    String tenantId = null;

    @Argument(index = 3, name = "routerId", description = "The router identifier of floating IP",
            required = true, multiValued = false)
    String routerId = null;

    @Argument(index = 4, name = "fixedIp", description = "The fixed IP of floating IP",
            required = true, multiValued = false)
    String fixedIp = null;

    @Argument(index = 5, name = "floatingIp", description = "The floating IP of floating IP",
            required = true, multiValued = false)
    String floatingIp = null;

    @Option(name = "-p", aliases = "--portId", description = "The port identifier of floating IP",
            required = false, multiValued = false)
    String portId = null;

    @Option(name = "-s", aliases = "--status", description = "The status of floating IP",
            required = false, multiValued = false)
    String status = null;

    @Override
    protected void execute() {
        FloatingIpService service = get(FloatingIpService.class);
        try {
            FloatingIp floatingIpObj = new DefaultFloatingIp(
                                                          FloatingIpId.of(id),
                                                          TenantId.tenantId(tenantId),
                                                          TenantNetworkId.networkId(networkId),
                                                          VirtualPortId.portId(portId),
                                                          RouterId.valueOf(routerId),
                                                          floatingIp == null ? null : IpAddress.valueOf(floatingIp),
                                                          fixedIp == null ? null : IpAddress.valueOf(fixedIp),
                                                          status == null ? Status.ACTIVE
                                                                         : Status.valueOf(status));
            Set<FloatingIp> floatingIpSet = Sets.newHashSet(floatingIpObj);
            service.createFloatingIps(floatingIpSet);
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }
}
