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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeService;

import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;
import static org.onosproject.k8snode.api.K8sNodeState.INIT;

/**
 * Initializes nodes for node service.
 */
@Service
@Command(scope = "onos", name = "k8s-node-init",
        description = "Initializes nodes for kubernetes node service")
public class K8sNodeInitCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all nodes",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Option(name = "-i", aliases = "--incomplete",
            description = "Apply this command to incomplete nodes",
            required = false, multiValued = false)
    private boolean isIncomplete = false;

    @Argument(index = 0, name = "hostnames", description = "Hostname(s) to apply this command",
            required = false, multiValued = true)
    @Completion(K8sHostnameCompleter.class)
    private String[] hostnames = null;

    @Override
    protected void doExecute() {
        K8sNodeService nodeService = get(K8sNodeService.class);
        K8sNodeAdminService nodeAdminService = get(K8sNodeAdminService.class);

        if ((!isAll && !isIncomplete && hostnames == null) ||
                (isAll && isIncomplete) ||
                (isIncomplete && hostnames != null) ||
                (hostnames != null && isAll)) {
            print("Please specify one of hostname, --all, and --incomplete options.");
            return;
        }

        if (isAll) {
            hostnames = nodeService.nodes().stream()
                    .map(K8sNode::hostname).toArray(String[]::new);
        } else if (isIncomplete) {
            hostnames = nodeService.nodes().stream()
                    .filter(node -> node.state() != COMPLETE)
                    .map(K8sNode::hostname).toArray(String[]::new);
        }

        for (String hostname : hostnames) {
            K8sNode node = nodeService.node(hostname);
            if (node == null) {
                print("Unable to find %s", hostname);
                continue;
            }
            print("Initializing %s", hostname);
            K8sNode updated = node.updateState(INIT);
            nodeAdminService.updateNode(updated);
        }
        print("Done.");
    }
}
