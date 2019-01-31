/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;

import static java.lang.Thread.sleep;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Re-installs flow rules for OpenStack networking.
 */
@Service
@Command(scope = "onos", name = "openstack-sync-rules",
        description = "Re-installs flow rules for OpenStack networking")
public class OpenstackSyncRulesCommand extends AbstractShellCommand {

    private static final long SLEEP_MS = 3000; // we wait 3s for init each node
    private static final long TIMEOUT_MS = 10000; // we wait 10s

    private static final String SUCCESS_MSG = "Successfully synchronize flow rules for node %s!";
    private static final String FAIL_MSG = "Failed to synchronize flow rules for node %s.";

    @Override
    protected void doExecute() {
        // All handlers in this application reacts the node complete event and
        // tries to re-configure flow rules for the complete node.
        OpenstackNodeAdminService osNodeService = get(OpenstackNodeAdminService.class);
        if (osNodeService == null) {
            error("Failed to re-install flow rules for OpenStack networking.");
            return;
        }

        // we first initialize the COMPUTE node, in order to feed all instance ports
        // by referring to ports' information obtained from neutron server
        osNodeService.completeNodes(COMPUTE).forEach(osNode ->
                syncRulesBaseForNode(osNodeService, osNode));
        osNodeService.completeNodes(GATEWAY).forEach(osNode ->
                syncRulesBaseForNode(osNodeService, osNode));

        print("Successfully requested re-installing flow rules.");
    }

    private void syncRulesBaseForNode(OpenstackNodeAdminService osNodeService,
                                      OpenstackNode osNode) {
        OpenstackNode updated = osNode.updateState(NodeState.INIT);
        osNodeService.updateNode(updated);

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        while (osNodeService.node(osNode.hostname()).state() != COMPLETE) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                error("Exception caused during node synchronization...");
            }

            if (osNodeService.node(osNode.hostname()).state() == COMPLETE) {
                break;
            } else {
                osNodeService.updateNode(updated);
                print("Failed to synchronize flow rules, retrying...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            print(SUCCESS_MSG, osNode.hostname());
        } else {
            error(FAIL_MSG, osNode.hostname());
        }
    }
}
