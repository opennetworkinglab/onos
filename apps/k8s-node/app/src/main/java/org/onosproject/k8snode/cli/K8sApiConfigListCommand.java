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
package org.onosproject.k8snode.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.k8snode.util.K8sNodeUtil.prettyJson;

/**
 * Lists all kubernetes API server configs registered to the service.
 */
@Service
@Command(scope = "onos", name = "k8s-api-configs",
        description = "Lists all kubernetes API server configs registered to the service")
public class K8sApiConfigListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-10s%-25s%-10s%-10s";

    @Override
    protected void doExecute() {
        K8sApiConfigService configService = get(K8sApiConfigService.class);
        List<K8sApiConfig> configs = Lists.newArrayList(configService.apiConfigs());
        configs.sort(Comparator.comparing(K8sApiConfig::ipAddress));

        if (outputJson()) {
            print("%s", json(configs));
        } else {
            print(FORMAT, "Scheme", "IpAddress", "Port", "State");
            for (K8sApiConfig config : configs) {
                print(FORMAT, config.scheme().name(), config.ipAddress().toString(),
                        config.port(), config.state().name());
            }
            print("Total %s API configs", configService.apiConfigs().size());
        }
    }

    private String json(List<K8sApiConfig> configs) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (K8sApiConfig config : configs) {
            result.add(jsonForEntity(config, K8sApiConfig.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
