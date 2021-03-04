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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;

import java.util.List;

import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.prettyJson;

/**
 * Show a node info registered to the service.
 */
@Service
@Command(scope = "onos", name = "kubevirt-node",
        description = "Displays a node details")
public class KubevirtShowNodeCommand extends AbstractShellCommand {

    @Option(name = "--name",
            description = "Filter kubevirt node by specific name", multiValued = true)
    @Completion(KubevirtHostnameCompleter.class)
    private List<String> names;

    @Override
    protected void doExecute() throws Exception {
        KubevirtNodeService service = get(KubevirtNodeService.class);

        if (names == null || names.size() == 0) {
            print("Need to specify at least one node name using --name option.");
            return;
        }

        for (String name : names) {
            KubevirtNode node = service.node(name);
            if (node == null) {
                print("Unable to find %s", name);
                continue;
            }

            if (outputJson()) {
                print("%s", json(node));
            } else {
                printNode(node);
            }
        }
    }

    private void printNode(KubevirtNode node) {
        print("Name: %s", node.hostname());
        print("  Type: %s", node.type());
        print("  State: %s", node.state());
        print("  Management IP: %s", node.managementIp().toString());
        print("  Data IP: %s", node.dataIp().toString());
        print("  Integration Bridge: %s", node.intgBridge());
        print("  Tunneling Bridge: %s", node.tunBridge());

        int counter = 1;
        for (KubevirtPhyInterface intf: node.phyIntfs()) {
            print("  Physical interfaces #%d:", counter);
            print("    Network: %s", intf.network());
            print("    Interface: %s", intf.intf());
            counter++;
        }
    }

    private String json(KubevirtNode node) {
        return prettyJson(new ObjectMapper(),
                jsonForEntity(node, KubevirtNode.class).toString());
    }
}
