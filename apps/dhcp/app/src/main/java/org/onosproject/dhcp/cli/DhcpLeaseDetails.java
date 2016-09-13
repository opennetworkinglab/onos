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
package org.onosproject.dhcp.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.dhcp.DhcpService;

/**
 * Lists all the default lease parameters offered by the DHCP Server.
 */
@Command(scope = "onos", name = "dhcp-lease",
        description = "Lists all the default lease parameters offered by the DHCP Server")
public class DhcpLeaseDetails extends AbstractShellCommand {
    private static final String DHCP_LEASE_FORMAT = "Lease Time: %ds\nRenewal Time: %ds\nRebinding Time: %ds";

    @Override
    protected void execute() {
        DhcpService dhcpService = AbstractShellCommand.get(DhcpService.class);
        int leaseTime = dhcpService.getLeaseTime();
        int renewTime = dhcpService.getRenewalTime();
        int rebindTime = dhcpService.getRebindingTime();

        print(DHCP_LEASE_FORMAT, leaseTime, renewTime, rebindTime);
    }
}
