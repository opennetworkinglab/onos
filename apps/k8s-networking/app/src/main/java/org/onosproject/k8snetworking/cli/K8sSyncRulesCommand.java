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
package org.onosproject.k8snetworking.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;

import static java.lang.Thread.sleep;
import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;
import static org.onosproject.k8snode.api.K8sNodeState.INIT;

/**
 * Synchronizes flow rules to provide connectivity for kubernetes pods.
 */
@Service
@Command(scope = "onos", name = "k8s-sync-rules",
        description = "Synchronizes all kubernetes flow rules")
public class K8sSyncRulesCommand extends AbstractShellCommand {

    private static final long SLEEP_MS = 10000; // we wait 10s for init each node
    private static final long TIMEOUT_MS = 30000; // we wait 30s

    private static final String SUCCESS_MSG = "Successfully synchronize flow rules for node %s!";
    private static final String FAIL_MSG = "Failed to synchronize flow rules for node %s.";

    @Override
    protected void doExecute() {

        K8sNodeAdminService adminService = get(K8sNodeAdminService.class);
        if (adminService == null) {
            error("Failed to re-install flow rules for kubernetes networking.");
            return;
        }

        adminService.completeNodes().forEach(node ->
                syncRulesBaseForNode(adminService, node));

        print("Successfully requested re-installing flow rules.");
    }

    private void syncRulesBaseForNode(K8sNodeAdminService adminService,
                                      K8sNode k8sNode) {
        K8sNode updated = k8sNode.updateState(INIT);
        adminService.updateNode(updated);

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        while (adminService.node(k8sNode.hostname()).state() != COMPLETE) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                error("Exception caused during node synchronization...");
            }

            if (adminService.node(k8sNode.hostname()).state() == COMPLETE) {
                break;
            } else {
                adminService.updateNode(updated);
                print("Failed to synchronize flow rules, retrying...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            print(SUCCESS_MSG, k8sNode.hostname());
        } else {
            error(FAIL_MSG, k8sNode.hostname());
        }
    }
}
