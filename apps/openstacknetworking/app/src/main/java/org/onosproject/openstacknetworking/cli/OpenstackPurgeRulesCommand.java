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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openstacknetworking.api.Constants;

import static java.lang.Thread.sleep;
import static java.util.stream.StreamSupport.stream;
import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Purges all existing network states.
 */
@Service
@Command(scope = "onos", name = "openstack-purge-rules",
        description = "Purges all flow rules installed by OpenStack networking app")
public class OpenstackPurgeRulesCommand extends AbstractShellCommand {

    private static final long TIMEOUT_MS = 10000; // we wait 10s
    private static final long SLEEP_MS = 2000; // we wait 2s for init each node

    @Override
    protected void doExecute() {
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(Constants.OPENSTACK_NETWORKING_APP_ID);

        if (appId == null) {
            error("Failed to purge OpenStack networking flow rules.");
            return;
        }

        flowRuleService.removeFlowRulesById(appId);
        print("Successfully purged flow rules installed by OpenStack networking app.");

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        // we make sure all flow rules are removed from the store
        while (stream(flowRuleService.getFlowEntriesById(appId)
                                     .spliterator(), false).count() > 0) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during rule purging...");
            }

            if (stream(flowRuleService.getFlowEntriesById(appId)
                                      .spliterator(), false).count() == 0) {
                break;
            } else {
                flowRuleService.removeFlowRulesById(appId);
                print("Failed to purging flow rules, retrying rule purging...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            print("Successfully purged flow rules!");
        } else {
            error("Failed to purge flow rules.");
        }
    }
}
