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
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sIngressService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes ingresses.
 */
@Service
@Command(scope = "onos", name = "k8s-ingresses",
        description = "Lists all kubernetes ingresses")
public class K8sIngressListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-50s%-15s%-30s";

    @Override
    protected void doExecute() {
        K8sIngressService service = get(K8sIngressService.class);
        List<Ingress> ingresses = Lists.newArrayList(service.ingresses());
        ingresses.sort(Comparator.comparing(p -> p.getMetadata().getName()));

        if (outputJson()) {
            print("%s", json(ingresses));
        } else {
            print(FORMAT, "Name", "Namespace", "LB Addresses");

            for (Ingress ingress : ingresses) {

                List<String> lbIps = Lists.newArrayList();

                ingress.getStatus().getLoadBalancer()
                        .getIngress().forEach(i -> lbIps.add(i.getIp()));

                print(FORMAT,
                        ingress.getMetadata().getName(),
                        ingress.getMetadata().getNamespace(),
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
