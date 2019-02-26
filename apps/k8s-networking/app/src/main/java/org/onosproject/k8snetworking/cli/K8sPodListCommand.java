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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sPodService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes pods.
 */
@Service
@Command(scope = "onos", name = "k8s-pods",
        description = "Lists all kubernetes pods")
public class K8sPodListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-50s%-15s%-15s%-30s";

    @Override
    protected void doExecute() {
        K8sPodService service = get(K8sPodService.class);
        List<Pod> pods = Lists.newArrayList(service.pods());
        pods.sort(Comparator.comparing(p -> p.getMetadata().getName()));

        if (outputJson()) {
            print("%s", json(pods));
        } else {
            print(FORMAT, "Name", "Namespace", "IP Address", "Containers");

            for (Pod pod : pods) {

                List<String> containers = Lists.newArrayList();

                pod.getSpec().getContainers().forEach(c -> containers.add(c.getName()));

                print(FORMAT,
                        pod.getMetadata().getName(),
                        pod.getMetadata().getNamespace(),
                        pod.getStatus().getPodIP(),
                        containers.isEmpty() ? "" : containers);
            }
        }
    }

    private String json(List<Pod> pods) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        try {
            for (Pod pod : pods) {
                ObjectNode json = (ObjectNode) new ObjectMapper()
                        .readTree(Serialization.asJson(pod));
                result.add(json);
            }
            return prettyJson(mapper, result.toString());
        } catch (IOException e) {
            log.warn("Failed to parse POD's JSON string.");
            return "";
        }
    }
}
