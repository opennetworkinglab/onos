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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnetworking.api.KubevirtPodService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.onosproject.kubevirtnetworking.api.Constants.CLI_IP_ADDRESS_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_LONG_NAME_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_NAMESPACE_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_STATUS_LENGTH;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.genFormatString;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.prettyJson;

/**
 * Lists kubevirt pods.
 */
@Service
@Command(scope = "onos", name = "kubevirt-pods",
        description = "Lists all kubevirt pods")
public class KubevirtListPodCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() throws Exception {
        KubevirtPodService service = get(KubevirtPodService.class);
        List<Pod> pods = Lists.newArrayList(service.pods());
        pods.sort(Comparator.comparing(p -> p.getMetadata().getName()));

        String format = genFormatString(ImmutableList.of(CLI_LONG_NAME_LENGTH,
                CLI_NAMESPACE_LENGTH, CLI_STATUS_LENGTH, CLI_IP_ADDRESS_LENGTH));

        if (outputJson()) {
            print("%s", json(pods));
        } else {
            print(format, "Name", "Namespace", "Status", "IP Address");

            for (Pod pod : pods) {

                List<String> containers = Lists.newArrayList();

                pod.getSpec().getContainers().forEach(c -> containers.add(c.getName()));

                print(format,
                        StringUtils.substring(pod.getMetadata().getName(),
                                0, CLI_LONG_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(pod.getMetadata().getNamespace(),
                                0, CLI_NAMESPACE_LENGTH - CLI_MARGIN_LENGTH),
                        pod.getStatus().getPhase(),
                        StringUtils.substring(pod.getStatus().getPodIP(),
                                0, CLI_IP_ADDRESS_LENGTH - CLI_MARGIN_LENGTH));
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
