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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.impl.OpenstackRoutingSnatHandler;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;

/**
 * Configure stateful SNAT mode.
 */
@Service
@Command(scope = "onos", name = "openstack-config-stateful-snat",
        description = "Re-configure stateful SNAT mode (true | false)")
public class OpenstackConfigStatefulSnatCommand extends AbstractShellCommand {

    private static final String USE_STATEFUL_SNAT = "useStatefulSnat";

    @Argument(index = 0, name = "statefulSnat",
            description = "Stateful SNAT mode (true | false)",
            required = true, multiValued = false)
    @Completion(BooleanCompleter.class)
    boolean statefulSnat;

    @Override
    protected void doExecute() {

        configSnatMode(statefulSnat);

        ComponentConfigService service = get(ComponentConfigService.class);
        String snatComponent = OpenstackRoutingSnatHandler.class.getName();

        while (true) {
            boolean snatValue =
                    getPropertyValueAsBoolean(
                            service.getProperties(snatComponent), USE_STATEFUL_SNAT);

            if (statefulSnat == snatValue) {
                break;
            }
        }

        purgeRules();
        syncRules();
    }

    private void configSnatMode(boolean snatMode) {
        ComponentConfigService service = get(ComponentConfigService.class);
        String snatComponent = OpenstackRoutingSnatHandler.class.getName();

        service.setProperty(snatComponent, USE_STATEFUL_SNAT, String.valueOf(snatMode));
    }

    private void purgeRules() {
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(Constants.OPENSTACK_NETWORKING_APP_ID);
        if (appId == null) {
            error("Failed to purge OpenStack networking flow rules.");
            return;
        }
        flowRuleService.removeFlowRulesById(appId);
    }

    private void syncRules() {
        // All handlers in this application reacts the node complete event and
        // tries to re-configure flow rules for the complete node.
        OpenstackNodeService osNodeService = get(OpenstackNodeService.class);
        OpenstackNodeAdminService osNodeAdminService = get(OpenstackNodeAdminService.class);
        if (osNodeService == null) {
            error("Failed to re-install flow rules for OpenStack networking.");
            return;
        }
        osNodeService.completeNodes().forEach(osNode -> {
            OpenstackNode updated = osNode.updateState(NodeState.INIT);
            osNodeAdminService.updateNode(updated);
        });
    }
}
