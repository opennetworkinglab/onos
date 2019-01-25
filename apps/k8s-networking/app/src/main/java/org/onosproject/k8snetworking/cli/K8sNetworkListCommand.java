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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes networks.
 */
@Service
@Command(scope = "onos", name = "k8s-networks",
        description = "Lists all kubernetes networks")
public class K8sNetworkListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-20s%-16s";

    @Override
    protected void doExecute() {
        K8sNetworkService service = get(K8sNetworkService.class);
        List<K8sNetwork> networks = Lists.newArrayList(service.networks());
        networks.sort(Comparator.comparing(K8sNetwork::name));

        if (outputJson()) {
            print("%s", json(networks));
        } else {
            print(FORMAT, "ID", "Name", "Type", "SegId", "Gateway");

            for (K8sNetwork net: networks) {
                print(FORMAT, net.networkId(),
                        net.name(),
                        net.type().toString(),
                        net.segmentId(),
                        net.gatewayIp() == null ? "" : net.gatewayIp().toString());
            }
        }
    }

    private String json(List<K8sNetwork> networks) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (K8sNetwork network : networks) {
            result.add(jsonForEntity(network, K8sNetwork.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
