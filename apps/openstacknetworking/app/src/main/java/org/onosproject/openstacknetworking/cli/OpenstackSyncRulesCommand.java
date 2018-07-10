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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import static java.lang.Thread.sleep;

/**
 * Re-installs flow rules for OpenStack networking.
 */
@Command(scope = "onos", name = "openstack-sync-rules",
        description = "Re-installs flow rules for OpenStack networking")
public class OpenstackSyncRulesCommand extends AbstractShellCommand {

    private static final long SLEEP_MS = 3000; // we wait 3s for init each node

    @Override
    protected void execute() {
        // All handlers in this application reacts the node complete event and
        // tries to re-configure flow rules for the complete node.
        OpenstackNodeService osNodeService = AbstractShellCommand.get(OpenstackNodeService.class);
        OpenstackNodeAdminService osNodeAdminService = AbstractShellCommand.get(OpenstackNodeAdminService.class);
        if (osNodeService == null) {
            error("Failed to re-install flow rules for OpenStack networking.");
            return;
        }
        osNodeService.completeNodes().forEach(osNode -> {
            OpenstackNode updated = osNode.updateState(NodeState.INIT);
            osNodeAdminService.updateNode(updated);

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }

            if (osNodeService.node(osNode.hostname()).state() == NodeState.COMPLETE) {
                print("Finished sync rules for node %s", osNode.hostname());
            } else {
                error("Failed to sync rules for node %s", osNode.hostname());
            }
        });
        print("Successfully requested re-installing flow rules.");
    }
}
