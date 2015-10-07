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
package org.onosproject.vtnrsc.cli.subnet;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.subnet.SubnetService;

/**
 * Supports for querying a subnet.
 */
@Command(scope = "onos", name = "subnets", description = "Supports for querying a subnet")
public class SubnetQueryCommand extends AbstractShellCommand {

    @Option(name = "-i", aliases = "--id", description = "Subnet id", required = false,
            multiValued = false)
    String id = null;

    private static final String FMT = "subnetId=%s, networkId=%s, subnetName=%s,"
            + "tenantId=%s, cidr=%s, dhcpEnabled=%s, gatewayIp=%s," + "ipVersion=%s";

    @Override
    protected void execute() {
        SubnetService service = get(SubnetService.class);
        if (id != null) {
            Subnet subnet = service.getSubnet(SubnetId.subnetId(id));
            printSubnet(subnet);
        } else {
            Iterable<Subnet> subnets = service.getSubnets();
            if (subnets == null) {
                return;
            }
            for (Subnet subnet : subnets) {
                printSubnet(subnet);
            }
        }
    }

    private void printSubnet(Subnet subnet) {
        print(FMT, subnet.id(), subnet.networkId(), subnet.subnetName(),
              subnet.tenantId(), subnet.cidr(), subnet.dhcpEnabled(), subnet
                      .gatewayIp(), subnet.ipVersion());

    }
}
