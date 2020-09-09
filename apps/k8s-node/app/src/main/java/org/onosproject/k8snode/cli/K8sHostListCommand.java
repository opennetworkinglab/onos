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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostService;
import org.onosproject.k8snode.api.K8sRouterBridge;
import org.onosproject.k8snode.api.K8sTunnelBridge;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.k8snode.util.K8sNodeUtil.genFormatString;
import static org.onosproject.k8snode.util.K8sNodeUtil.prettyJson;

/**
 * Lists all host registered to the service.
 */
@Service
@Command(scope = "onos", name = "k8s-hosts",
        description = "Lists all hosts registered in kubernetes host service")
public class K8sHostListCommand extends AbstractShellCommand {

    private static final int HOST_IP_LENGTH = 15;
    private static final int TUNBRS_LENGTH = 40;
    private static final int RTRBRS_LENGTH = 40;
    private static final int STATUS_LENGTH = 15;

    @Override
    protected void doExecute() {
        K8sHostService hostService = get(K8sHostService.class);
        List<K8sHost> hosts = Lists.newArrayList(hostService.hosts());
        hosts.sort(Comparator.comparing(K8sHost::hostIp));

        String format = genFormatString(
                ImmutableList.of(HOST_IP_LENGTH, TUNBRS_LENGTH, RTRBRS_LENGTH, STATUS_LENGTH));

        if (outputJson()) {
            print("%s", json(hosts));
        } else {
            print(format, "Host IP", "Tunnel Bridges", "Router Bridges", "State");
            for (K8sHost host : hosts) {
                print(format,
                        host.hostIp().toString(),
                        host.tunBridges().stream().map(K8sTunnelBridge::name)
                                .collect(Collectors.toSet()).toString(),
                        host.routerBridges().stream().map(K8sRouterBridge::name)
                                .collect(Collectors.toSet()).toString(),
                        host.state().toString());
            }
            print("Total %s hosts", hosts.size());
        }
    }

    private String json(List<K8sHost> hosts) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (K8sHost host: hosts) {
            result.add(jsonForEntity(host, K8sHost.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
