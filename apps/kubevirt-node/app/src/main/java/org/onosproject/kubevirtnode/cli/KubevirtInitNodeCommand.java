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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;

import static org.onosproject.kubevirtnode.api.KubevirtNodeState.COMPLETE;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.INIT;

/**
 * Initializes nodes for node service.
 */
@Service
@Command(scope = "onos", name = "kubevirt-init-node",
        description = "Initializes nodes for KubeVirt node service")
public class KubevirtInitNodeCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--all", description = "Apply this command to all nodes",
            required = false, multiValued = false)
    private boolean isAll = false;

    @Option(name = "-i", aliases = "--incomplete",
            description = "Apply this command to incomplete nodes",
            required = false, multiValued = false)
    private boolean isIncomplete = false;

    @Argument(index = 0, name = "hostnames", description = "Hostname(s) to apply this command",
            required = false, multiValued = true)
    @Completion(KubevirtHostnameCompleter.class)
    private String[] hostnames = null;

    @Override
    protected void doExecute() throws Exception {
        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);

        if ((!isAll && !isIncomplete && hostnames == null) ||
                (isAll && isIncomplete) ||
                (isIncomplete && hostnames != null) ||
                (hostnames != null && isAll)) {
            print("Please specify one of hostname, --all, and --incomplete options.");
            return;
        }

        if (isAll) {
            hostnames = service.nodes().stream()
                    .map(KubevirtNode::hostname).toArray(String[]::new);
        } else if (isIncomplete) {
            hostnames = service.nodes().stream()
                    .filter(node -> node.state() != COMPLETE)
                    .map(KubevirtNode::hostname).toArray(String[]::new);
        }

        for (String hostname : hostnames) {
            KubevirtNode node = service.node(hostname);
            if (node == null) {
                print("Unable to find %s", hostname);
                continue;
            }
            print("Initializing %s", hostname);
            KubevirtNode updated = node.updateState(INIT);
            service.updateNode(updated);
        }
        print("Done.");
    }
}
