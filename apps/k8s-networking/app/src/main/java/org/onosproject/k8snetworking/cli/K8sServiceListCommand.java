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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sServiceService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.CLI_IP_ADDRESS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_PORTS_LENGTH;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.genFormatString;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes services.
 */
@Service
@Command(scope = "onos", name = "k8s-services",
        description = "Lists all kubernetes services")
public class K8sServiceListCommand extends AbstractShellCommand {

    private static final String PORT_PROTOCOL_SEPARATOR = "/";

    @Override
    protected void doExecute() {
        K8sServiceService service = get(K8sServiceService.class);
        List<io.fabric8.kubernetes.api.model.Service> services =
                Lists.newArrayList(service.services());
        services.sort(Comparator.comparing(s -> s.getMetadata().getName()));

        String format = genFormatString(ImmutableList.of(CLI_NAME_LENGTH,
                CLI_IP_ADDRESS_LENGTH, CLI_PORTS_LENGTH));

        if (outputJson()) {
            print("%s", json(services));
        } else {
            print(format, "Name", "Cluster IP", "Ports");

            for (io.fabric8.kubernetes.api.model.Service svc : services) {

                List<String> portWithProtocol = Lists.newArrayList();

                svc.getSpec().getPorts().forEach(p ->
                        portWithProtocol.add(p.getPort() +
                                PORT_PROTOCOL_SEPARATOR + p.getProtocol()));

                print(format,
                        StringUtils.substring(svc.getMetadata().getName(),
                                0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(svc.getSpec().getClusterIP(),
                                0, CLI_IP_ADDRESS_LENGTH - CLI_MARGIN_LENGTH),
                        portWithProtocol.isEmpty() ? "" : portWithProtocol);
            }
        }
    }

    private String json(List<io.fabric8.kubernetes.api.model.Service> services) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        try {
            for (io.fabric8.kubernetes.api.model.Service service : services) {
                ObjectNode json = (ObjectNode) new ObjectMapper()
                        .readTree(Serialization.asJson(service));
                result.add(json);
            }
            return prettyJson(mapper, result.toString());
        } catch (IOException e) {
            log.warn("Failed to parse service's JSON string.");
            return "";
        }
    }
}
