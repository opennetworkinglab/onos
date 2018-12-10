/*
 * Copyright 2018-present Open Networking Foundation
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
import org.onosproject.openstacknetworking.impl.OpenstackRoutingArpHandler;
import org.onosproject.openstacknetworking.impl.OpenstackSwitchingArpHandler;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.checkArpMode;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValue;

/**
 * Configure ARP mode.
 */
@Service
@Command(scope = "onos", name = "openstack-config-arp-mode",
        description = "Re-configure ARP mode (proxy | broadcast)")
public class OpenstackConfigArpModeCommand extends AbstractShellCommand {

    private static final String ARP_MODE_NAME = "arpMode";

    @Argument(index = 0, name = "arpMode",
            description = "ARP mode (proxy | broadcast)",
            required = true, multiValued = false)
    @Completion(ArpModeCompleter.class)
    String arpMode = null;

    @Override
    protected void doExecute() {

        if (checkArpMode(arpMode)) {
            configArpMode(arpMode);

            ComponentConfigService service = get(ComponentConfigService.class);
            String switchingComponent = OpenstackSwitchingArpHandler.class.getName();
            String routingComponent = OpenstackRoutingArpHandler.class.getName();

            // we check the arpMode configured in each component, and purge and
            // reinstall all rules only if the arpMode is changed to the configured one
            while (true) {
                String switchingValue =
                        getPropertyValue(
                                service.getProperties(switchingComponent), ARP_MODE_NAME);
                String routingValue =
                        getPropertyValue(
                                service.getProperties(routingComponent), ARP_MODE_NAME);

                if (arpMode.equals(switchingValue) && arpMode.equals(routingValue)) {
                    break;
                }
            }

            purgeRules();
            syncRules();
        }
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

    private void configArpMode(String arpMode) {
        ComponentConfigService service = get(ComponentConfigService.class);
        String switchingComponent = OpenstackSwitchingArpHandler.class.getName();
        String routingComponent = OpenstackRoutingArpHandler.class.getName();

        if (!isNullOrEmpty(arpMode)) {
            service.setProperty(switchingComponent, ARP_MODE_NAME, arpMode);
            service.setProperty(routingComponent, ARP_MODE_NAME, arpMode);
        }
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
