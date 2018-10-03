/*
 * Copyright 2015-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.IpAssignment;

import java.util.Date;

import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_Requested;

/**
 * Registers a static MAC Address to IP Mapping with the DHCP Server.
 */
@Service
@Command(scope = "onos", name = "dhcp-set-static-mapping",
        description = "Registers a static MAC Address to IP Mapping with the DHCP Server")
public class DhcpSetStaticMapping extends AbstractShellCommand {

    @Argument(index = 0, name = "macAddr",
            description = "MAC Address of the client",
            required = true, multiValued = false)
    @Completion(MacIdCompleter.class)
    String macAddr = null;

    @Argument(index = 1, name = "ipAddr",
            description = "IP Address requested for static mapping",
            required = true, multiValued = false)
    @Completion(FreeIpCompleter.class)
    String ipAddr = null;

    private static final String DHCP_SUCCESS = "Static Mapping Successfully Added.";
    private static final String DHCP_FAILURE = "Static Mapping Failed. The IP maybe unavailable.";
    @Override
    protected void doExecute() {
        DhcpService dhcpService = AbstractShellCommand.get(DhcpService.class);

        try {
            MacAddress macID = MacAddress.valueOf(macAddr);
            Ip4Address ipAddress = Ip4Address.valueOf(ipAddr);

            IpAssignment ipAssignment = IpAssignment.builder()
                    .ipAddress(ipAddress)
                    .leasePeriod(dhcpService.getLeaseTime())
                    .timestamp(new Date())
                    .assignmentStatus(Option_Requested)
                    .build();

            if (dhcpService.setStaticMapping(macID, ipAssignment)) {
                print(DHCP_SUCCESS);
            } else {
                print(DHCP_FAILURE);
            }

        } catch (IllegalArgumentException e) {
            print(e.getMessage());
        }
    }
}
