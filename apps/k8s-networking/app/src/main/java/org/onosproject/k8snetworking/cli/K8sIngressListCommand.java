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
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sIngressService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.CLI_IP_ADDRESS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAMESPACE_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.genFormatString;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes ingresses.
 */
@Service
@Command(scope = "onos", name = "k8s-ingresses",
        description = "Lists all kubernetes ingresses")
public class K8sIngressListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        K8sIngressService service = get(K8sIngressService.class);
        List<Ingress> ingresses = Lists.newArrayList(service.ingresses());
        ingresses.sort(Comparator.comparing(p -> p.getMetadata().getName()));

        String format = genFormatString(ImmutableList.of(CLI_NAME_LENGTH,
                CLI_NAMESPACE_LENGTH, CLI_IP_ADDRESS_LENGTH));

        if (outputJson()) {
            print("%s", json(ingresses));
        } else {
            print(format, "Name", "Namespace", "LB Addresses");

            for (Ingress ingress : ingresses) {

                List<String> lbIps = Lists.newArrayList();

                ingress.getStatus().getLoadBalancer()
                        .getIngress().forEach(i -> lbIps.add(i.getIp()));

                print(format,
                        StringUtils.substring(ingress.getMetadata().getName(),
                                0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(ingress.getMetadata().getNamespace(),
                                0, CLI_NAMESPACE_LENGTH - CLI_MARGIN_LENGTH),
                        lbIps.isEmpty() ? "" : lbIps);
            }
        }
    }

    private String json(List<Ingress> ingresses) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        try {
            for (Ingress ingress : ingresses) {
                ObjectNode json = (ObjectNode) new ObjectMapper()
                        .readTree(Serialization.asJson(ingress));
                result.add(json);
            }
            return prettyJson(mapper, result.toString());
        } catch (IOException e) {
            log.warn("Failed to parse ingress's JSON string.");
            return "";
        }
    }
}
