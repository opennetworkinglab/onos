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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openstacknetworking.api.Constants;

/**
 * Purges all existing network states.
 */
@Command(scope = "onos", name = "openstack-purge-rules",
        description = "Purges all flow rules installed by OpenStack networking app")
public class OpenstackPurgeRulesCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        FlowRuleService flowRuleService = AbstractShellCommand.get(FlowRuleService.class);
        CoreService coreService = AbstractShellCommand.get(CoreService.class);
        ApplicationId appId = coreService.getAppId(Constants.OPENSTACK_NETWORKING_APP_ID);
        if (appId == null) {
            error("Failed to purge OpenStack networking flow rules.");
            return;
        }
        flowRuleService.removeFlowRulesById(appId);
        print("Successfully purged flow rules installed by OpenStack networking application.");
    }
}
