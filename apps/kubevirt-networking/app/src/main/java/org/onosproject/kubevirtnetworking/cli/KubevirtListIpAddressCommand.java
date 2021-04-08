/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.cli;

import com.google.common.collect.ImmutableList;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.onosproject.kubevirtnetworking.api.Constants.CLI_IP_ADDRESS_AVAILABILITY;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_IP_ADDRESS_LENGTH;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.genFormatString;

/**
 * Lists all IP addresses.
 */
@Service
@Command(scope = "onos", name = "kubevirt-ips",
        description = "Lists all IP addresses")
public class KubevirtListIpAddressCommand extends AbstractShellCommand {

    @Argument(name = "networkId", description = "Network ID")
    @Completion(KubevirtNetworkIdCompleter.class)
    private String networkId = null;

    @Override
    protected void doExecute() throws Exception {
        KubevirtNetworkService service = get(KubevirtNetworkService.class);

        if (networkId == null) {
            error("No network identifier was specified");
            return;
        }

        KubevirtNetwork network = service.network(networkId);

        if (network == null) {
            print("No network was found with the given network ID");
            return;
        }

        KubevirtIpPool pool = network.ipPool();
        if (pool == null) {
            print("No IP pool was found with the given network ID");
            return;
        }

        String format = genFormatString(ImmutableList.of(
                CLI_IP_ADDRESS_LENGTH, CLI_IP_ADDRESS_AVAILABILITY));
        print(format, "IP Address", "Availability");

        List<IpAddress> sortedAllocatedIps = new ArrayList<>(pool.allocatedIps());
        Collections.sort(sortedAllocatedIps);
        for (IpAddress ip : sortedAllocatedIps) {
            print(format, ip.toString(), "[ X ]");
        }

        List<IpAddress> sortedAvailableIps = new ArrayList<>(pool.availableIps());
        Collections.sort(sortedAvailableIps);
        for (IpAddress ip : sortedAvailableIps) {
            print(format, ip.toString(), "[ O ]");
        }
    }
}
