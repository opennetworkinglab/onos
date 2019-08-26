/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.cli;

import com.google.common.collect.Maps;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sIpamService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPort;

import java.util.Map;
import java.util.Set;

import static org.onlab.packet.MacAddress.ZERO;

/**
 * Lists kubernetes IP addresses.
 */
@Command(scope = "onos", name = "k8s-ips",
        description = "Lists all kubernetes IP addresses")
public class K8sIpAddressListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-30s%-20s%-30s";

    @Argument(index = 0, name = "networkIds", description = "Network identifiers",
            required = false, multiValued = true)
    private String[] networkIds = null;

    @Option(name = "-a", aliases = "--available",
            description = "Available IP addresses",
            required = false, multiValued = false)
    private boolean available = false;

    @Option(name = "-r", aliases = "--reserved",
            description = "Allocated IP addresses",
            required = false, multiValued = false)
    private boolean reserved = false;

    @Override
    protected void doExecute() {
        K8sIpamService ipamService = get(K8sIpamService.class);
        K8sNetworkService networkService = get(K8sNetworkService.class);

        if (networkIds == null || networkIds.length == 0) {
            networkIds = networkService.networks().stream()
                    .map(K8sNetwork::networkId).toArray(String[]::new);
        }

        Map<String, Map<IpAddress, MacAddress>> ipMacs = Maps.newConcurrentMap();

        if (available && reserved) {
            error("Only one of list options (available | reserved) can be specified.");
            return;
        }

        if (!(available || reserved)) {
            error("At least one of list options (available | reserved) should be specified.");
            return;
        }

        for (String networkId : networkIds) {
            Map<IpAddress, MacAddress> tmpIpMacs = Maps.newConcurrentMap();
            if (available) {
                ipamService.availableIps(networkId)
                        .forEach(n -> tmpIpMacs.put(n, ZERO));
            }

            if (reserved) {
                Set<K8sPort> ports = networkService.ports(networkId);
                ipamService.allocatedIps(networkId).forEach(ip -> {
                    MacAddress mac = ports.stream()
                            .filter(p -> p.ipAddress().equals(ip))
                            .map(K8sPort::macAddress).findAny().orElse(ZERO);
                    tmpIpMacs.put(ip, mac);
                });
            }
            ipMacs.put(networkId, tmpIpMacs);
        }

        if (ipMacs.size() > 0) {
            print(FORMAT, "Network ID", "IP Address", "MAC Address");
            ipMacs.forEach((k, v) -> v.forEach((ip, mac) -> print(FORMAT, k, ip, mac)));
        } else {
            print("No IP addresses are available or reserved.");
        }
    }
}
