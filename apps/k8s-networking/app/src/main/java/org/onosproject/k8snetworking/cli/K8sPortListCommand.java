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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPort;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.CLI_ID_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_IP_ADDRESSES_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_MAC_ADDRESS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.genFormatString;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes ports.
 */
@Service
@Command(scope = "onos", name = "k8s-ports",
        description = "Lists all kubernetes ports")
public class K8sPortListCommand extends AbstractShellCommand {

    @Argument(name = "networkId", description = "Network ID")
    private String networkId = null;

    @Override
    protected void doExecute() {
        K8sNetworkService service = get(K8sNetworkService.class);

        List<K8sPort> ports = Lists.newArrayList(service.ports());
        ports.sort(Comparator.comparing(K8sPort::networkId));

        String format = genFormatString(ImmutableList.of(CLI_ID_LENGTH,
                CLI_NAME_LENGTH, CLI_MAC_ADDRESS_LENGTH, CLI_IP_ADDRESSES_LENGTH));

        if (!Strings.isNullOrEmpty(networkId)) {
            ports.removeIf(port -> !port.networkId().equals(networkId));
        }

        if (outputJson()) {
            print("%s", json(ports));
        } else {
            print(format, "ID", "Network", "MAC Address", "Fixed IPs");
            for (K8sPort port: ports) {
                K8sNetwork k8sNet = service.network(port.networkId());
                print(format,
                        StringUtils.substring(port.portId(),
                                0, CLI_ID_LENGTH - CLI_MARGIN_LENGTH),
                        k8sNet == null ? "" : StringUtils.substring(k8sNet.name(),
                                0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(port.macAddress().toString(),
                                0, CLI_MAC_ADDRESS_LENGTH - CLI_MARGIN_LENGTH),
                        port.ipAddress() == null ? "" : port.ipAddress());
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
