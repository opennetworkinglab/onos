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
 * Supports for update a floating IP.
 */
@Command(scope = "onos", name = "floatingip-update",
        description = "Supports for updating a floating IP")
public class FloatingIpUpdateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "id", description = "The floating IP identifier",
            required = true, multiValued = false)
    String id = null;

    @Option(name = "-n", aliases = "--networkId", description = "The network identifier of floating IP",
            required = false, multiValued = false)
    String networkId = null;

    @Option(name = "-t", aliases = "--tenantId", description = "The tenant identifier of floating IP",
            required = false, multiValued = false)
    String tenantId = null;

    @Option(name = "-r", aliases = "--routerId", description = "The router identifier of floating IP",
            required = false, multiValued = false)
    String routerId = null;

    @Option(name = "-p", aliases = "--portId", description = "The port identifier of floating IP",
            required = false, multiValued = false)
    String portId = null;

    @Option(name = "-s", aliases = "--status", description = "The status of floating IP",
            required = false, multiValued = false)
    String status = null;

    @Option(name = "-i", aliases = "--fixedIp", description = "The fixed IP of floating IP",
            required = false, multiValued = false)
    String fixedIp = null;

    @Option(name = "-l", aliases = "--floatingIp", description = "The floating IP of floating IP",
            required = false, multiValued = false)
    String floatingIp = null;

    @Override
    protected void execute() {
        FloatingIpService service = get(FloatingIpService.class);
        FloatingIpId floatingIpId = FloatingIpId.of(id);
        FloatingIp floatingIpStore = get(FloatingIpService.class).getFloatingIp(floatingIpId);
        try {
            FloatingIp floatingIpObj = new DefaultFloatingIp(
                                                          floatingIpId,
                                                          tenantId == null ? floatingIpStore.tenantId()
                                                                           :  TenantId.tenantId(tenantId),
                                                          networkId == null ? floatingIpStore.networkId()
                                                                            : TenantNetworkId.networkId(networkId),
                                                          portId == null ? floatingIpStore.portId()
                                                                         : VirtualPortId.portId(portId),
                                                          routerId == null ? floatingIpStore.routerId()
                                                                           : RouterId.valueOf(routerId),
                                                          floatingIp == null ? floatingIpStore.floatingIp()
                                                                             : IpAddress.valueOf(floatingIp),
                                                          fixedIp == null ? floatingIpStore.fixedIp()
                                                                          : IpAddress.valueOf(fixedIp),
                                                          status == null ? floatingIpStore.status()
                                                                         : Status.valueOf(status));
            Set<FloatingIp> floatingIpSet = Sets.newHashSet(floatingIpObj);
            service.updateFloatingIps(floatingIpSet);
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }
}
