/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;

import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.prettyJson;

/**
 * Lists all KubeVirt API server configs registered to the service.
 */
@Service
@Command(scope = "onos", name = "kubevirt-api-configs",
        description = "Lists all KubeVirt API server configs registered to the service")
public class KubevirtListApiConfigsCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-10s%-20s%-10s%-25s%-10s%-20s%-20s";

    @Override
    protected void doExecute() throws Exception {
        KubevirtApiConfigService service = get(KubevirtApiConfigService.class);
        KubevirtApiConfig config = service.apiConfig();

        if (outputJson()) {
            print("%s", json(config));
        } else {
            print(FORMAT, "Scheme", "Server IP", "Port", "Controller IP", "State", "Datacenter ID", "Cluster ID");
            String controllerIp = "N/A";
            if (config != null) {
                if (config.controllerIp() != null) {
                    controllerIp = config.controllerIp().toString();
                }
                print(FORMAT, config.scheme().name(), config.ipAddress().toString(),
                        config.port(), controllerIp, config.state().name(), config.datacenterId(), config.clusterId());
            } else {
                print("Kubevirt config not found!");
            }

        }
    }

    private String json(KubevirtApiConfig config) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = jsonForEntity(config, KubevirtApiConfig.class);
        return prettyJson(mapper, node.toString());
    }
}
