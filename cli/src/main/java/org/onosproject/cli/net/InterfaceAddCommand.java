/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.cli.net;

import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceAdminService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Set;

/**
 * Adds a new interface configuration.
 */
@Command(scope = "onos", name = "add-interface",
        description = "Adds a new configured interface")
public class InterfaceAddCommand extends AbstractShellCommand {

    @Option(name = "-c", aliases = "--connectPoint",
            description = "Device port that the interface is associated with",
            required = true, multiValued = false)
    private String connectPoint = null;

    @Option(name = "-m", aliases = "--mac",
            description = "MAC address of the interface",
            required = true, multiValued = false)
    private String mac = null;

    @Option(name = "-i", aliases = "--ip",
            description = "IP address configured on the interface\n" +
            "(e.g. 10.0.1.1/24). Can be specified multiple times.",
            required = false, multiValued = true)
    private String[] ips = null;

    @Option(name = "-v", aliases = "--vlan",
            description = "VLAN configured on the interface",
            required = false, multiValued = false)
    private String vlan = null;

    @Override
    protected void execute() {
        InterfaceAdminService interfaceService = get(InterfaceAdminService.class);

        Set<InterfaceIpAddress> ipAddresses = Sets.newHashSet();
        if (ips != null) {
            for (String strIp : ips) {
                ipAddresses.add(InterfaceIpAddress.valueOf(strIp));
            }
        }

        VlanId vlanId = vlan == null ? VlanId.NONE : VlanId.vlanId(Short.parseShort(vlan));

        Interface intf = new Interface(ConnectPoint.deviceConnectPoint(connectPoint),
                ipAddresses, MacAddress.valueOf(mac), vlanId);

        interfaceService.add(intf);
    }

}
