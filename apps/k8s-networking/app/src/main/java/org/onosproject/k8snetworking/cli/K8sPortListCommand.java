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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPort;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes ports.
 */
@Service
@Command(scope = "onos", name = "k8s-ports",
        description = "Lists all kubernetes ports")
public class K8sPortListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s%-8s";

    @Argument(name = "networkId", description = "Network ID")
    private String networkId = null;

    @Override
    protected void doExecute() {
        K8sNetworkService service = get(K8sNetworkService.class);

        List<K8sPort> ports = Lists.newArrayList(service.ports());
        ports.sort(Comparator.comparing(K8sPort::networkId));

        if (!Strings.isNullOrEmpty(networkId)) {
            ports.removeIf(port -> !port.networkId().equals(networkId));
        }

        if (outputJson()) {
            print("%s", json(ports));
        } else {
            print(FORMAT, "ID", "Network", "MAC Address", "Fixed IPs");
            for (K8sPort port: ports) {
                K8sNetwork k8sNet = service.network(port.networkId());
                print(FORMAT, port.portId(),
                        k8sNet == null ? "" : k8sNet.name(),
                        port.macAddress(),
                        port.ipAddress() == null ? "" : port.ipAddress().toString());
            }
        }
    }

    private String json(List<K8sPort> ports) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (K8sPort port : ports) {
            result.add(jsonForEntity(port, K8sPort.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
