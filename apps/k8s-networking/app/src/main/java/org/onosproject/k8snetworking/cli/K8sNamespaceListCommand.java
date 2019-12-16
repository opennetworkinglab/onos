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
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snetworking.api.K8sNamespaceService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snetworking.api.Constants.CLI_LABELS_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.CLI_PHASE_LENGTH;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.genFormatString;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.prettyJson;

/**
 * Lists kubernetes namespaces.
 */
@Command(scope = "onos", name = "k8s-namespaces",
        description = "Lists all kubernetes namespaces")
public class K8sNamespaceListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        K8sNamespaceService service = get(K8sNamespaceService.class);
        List<Namespace> namespaces = Lists.newArrayList(service.namespaces());
        namespaces.sort(Comparator.comparing(n -> n.getMetadata().getName()));

        String format = genFormatString(ImmutableList.of(CLI_NAME_LENGTH,
                CLI_PHASE_LENGTH, CLI_LABELS_LENGTH));

        if (outputJson()) {
            print("%s", json(namespaces));
        } else {
            print(format, "Name", "Phase", "Labels");

            for (Namespace namespace : namespaces) {

                print(format,
                        StringUtils.substring(namespace.getMetadata().getName(),
                                0, CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        namespace.getStatus().getPhase(),
                        namespace.getMetadata() != null &&
                                namespace.getMetadata().getLabels() != null &&
                                !namespace.getMetadata().getLabels().isEmpty() ?
                                StringUtils.substring(namespace.getMetadata()
                                                .getLabels().toString(), 0,
                                        CLI_LABELS_LENGTH - CLI_MARGIN_LENGTH) : "");
            }
        }
    }

    private String json(List<Namespace> namespaces) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        try {
            for (Namespace namespace : namespaces) {
                ObjectNode json = (ObjectNode) new ObjectMapper()
                        .readTree(Serialization.asJson(namespace));
                result.add(json);
            }
            return prettyJson(mapper, result.toString());
        } catch (IOException e) {
            log.warn("Failed to parse Namespace's JSON string.");
            return "";
        }
    }
}
