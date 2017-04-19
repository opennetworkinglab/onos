/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.openstacknode.OpenstackNodeService;

/**
 * Re-installs flow rules for OpenStack networking.
 */
@Command(scope = "onos", name = "openstack-sync-rules",
        description = "Re-installs flow rules for OpenStack networking")
public class OpenstackSyncRulesCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        // All handlers in this application reacts the node complete event and
        // tries to re-configure flow rules for the complete node.
        OpenstackNodeService nodeService = AbstractShellCommand.get(OpenstackNodeService.class);
        if (nodeService == null) {
            error("Failed to re-install flow rules for OpenStack networking.");
            return;
        }
        nodeService.completeNodes().forEach(nodeService::processCompleteState);
        print("Successfully requested re-installing flow rules.");
    }
}
