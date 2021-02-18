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
package org.onosproject.kubevirtnode.cli;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;

import java.util.Set;

import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.INIT;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.buildKubevirtNode;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.k8sClient;

/**
 * Synchronizes kubevirt node states.
 */
@Service
@Command(scope = "onos", name = "kubevirt-sync-state",
        description = "Synchronizes kubevirt node states.")
public class KubevirtSyncStateCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() throws Exception {
        KubevirtApiConfigService apiConfigService = get(KubevirtApiConfigService.class);

        print("Re-synchronizing Kubevirt node states..");
        KubevirtApiConfig config = apiConfigService.apiConfig();
        bootstrapKubevirtNodes(config);
        print("Done.");

    }

    private void bootstrapKubevirtNodes(KubevirtApiConfig config) {
        KubevirtNodeAdminService nodeAdminService = get(KubevirtNodeAdminService.class);

        Set<KubevirtNode> completeNodeSet = nodeAdminService.completeNodes();
        KubernetesClient k8sClient = k8sClient(config);

        if (k8sClient == null) {
            log.warn("Failed to connect to kubernetes API server");
            return;
        }

        for (Node node : k8sClient.nodes().list().getItems()) {
            KubevirtNode kubevirtNode = buildKubevirtNode(node);
            // we always provision VMs to worker nodes, so only need to install
            // flow rules in worker nodes
            if (kubevirtNode.type() == WORKER) {
                if (completeNodeSet.stream().map(KubevirtNode::hostname)
                        .filter(name -> name.equals(kubevirtNode.hostname()))
                        .findAny().isPresent()) {
                    print("Initializing %s because the node was COMPLETE state.",
                            kubevirtNode.hostname());
                    KubevirtNode updated = kubevirtNode.updateState(INIT);
                    nodeAdminService.updateNode(updated);
                } else {
                    nodeAdminService.updateNode(kubevirtNode);
                }
            }
        }
    }
}
