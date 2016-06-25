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
package org.onosproject.vtnrsc.cli.virtualport;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

/**
 * Supports for querying virtualPorts.
 */
@Command(scope = "onos", name = "virtualports", description = "Supports for querying virtualPorts.")
public class VirtualPortQueryCommand extends AbstractShellCommand {

    @Option(name = "-v", aliases = "--vPortId", description = "virtualPort ID.", required = false,
            multiValued = false)
    String vPortId;

    @Option(name = "-n", aliases = "--networkId", description = "network ID.", required = false,
            multiValued = false)
    String networkId;

    @Option(name = "-d", aliases = "--deviceId", description = "device ID.", required = false,
            multiValued = false)
    String deviceId;

    @Option(name = "-t", aliases = "--tenantId", description = "tenant ID.", required = false,
            multiValued = false)
    String tenantId;

    private static final String FMT = "virtualPortId=%s, networkId=%s, name=%s,"
            + " tenantId=%s, deviceId=%s, adminStateUp=%s, state=%s,"
            + " macAddress=%s, deviceOwner=%s, fixedIp=%s, bindingHostId=%s,"
            + " bindingvnicType=%s, bindingvifType=%s, bindingvnicDetails=%s,"
            + " allowedAddress=%s, securityGroups=%s";

    @Override
    protected void execute() {
        VirtualPortService service = get(VirtualPortService.class);
        if (vPortId != null && networkId == null && deviceId == null && tenantId == null) {
            VirtualPort port = service.getPort(VirtualPortId.portId(vPortId));
            printPort(port);
        } else if (vPortId == null && networkId != null && deviceId == null && tenantId == null) {
            Collection<VirtualPort> ports = service.getPorts(TenantNetworkId.networkId(networkId));
            printPorts(ports);
        } else if (vPortId == null && networkId == null && deviceId != null && tenantId == null) {
            Collection<VirtualPort> ports = service.getPorts(DeviceId.deviceId(deviceId));
            printPorts(ports);
        } else if (vPortId == null && networkId == null && deviceId == null && tenantId != null) {
            Collection<VirtualPort> ports = service.getPorts(DeviceId.deviceId(tenantId));
            printPorts(ports);
        } else if (vPortId == null && networkId == null && deviceId == null && tenantId == null) {
            Collection<VirtualPort> ports = service.getPorts();
            printPorts(ports);
        } else {
            print("cannot input more than one parameter");
        }

    }

    private void printPorts(Collection<VirtualPort> ports) {
        for (VirtualPort port : ports) {
            printPort(port);
        }
    }

    private void printPort(VirtualPort port) {
        print(FMT, port.portId(), port.networkId(), port.name(), port.tenantId(), port.deviceId(),
              port.adminStateUp(), port.state(), port.macAddress(), port.deviceOwner(), port
                      .fixedIps(), port.bindingHostId(), port.bindingVnicType(),
              port.bindingVifType(), port.bindingVifDetails(), port.allowedAddressPairs(),
              port.securityGroups());
    }
}
