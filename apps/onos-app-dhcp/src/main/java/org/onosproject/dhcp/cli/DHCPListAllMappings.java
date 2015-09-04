/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.dhcp.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.dhcp.DHCPService;

import java.util.Map;

/**
 * Lists all the MacAddress to IP Address mappings held by the DHCP Server.
 */
@Command(scope = "onos", name = "dhcp-list",
        description = "Lists all the MAC to IP mappings held by the DHCP Server")
public class DHCPListAllMappings extends AbstractShellCommand {

    private static final String DHCP_MAPPING_FORMAT = "MAC ID: %s -> IP ASSIGNED %s";
    @Override
    protected void execute() {

        DHCPService dhcpService = AbstractShellCommand.get(DHCPService.class);
        Map<MacAddress, Ip4Address> allocationMap = dhcpService.listMapping();

        for (Map.Entry<MacAddress, Ip4Address> entry : allocationMap.entrySet()) {
            print(DHCP_MAPPING_FORMAT, entry.getKey().toString(), entry.getValue().toString());
        }
    }
}
