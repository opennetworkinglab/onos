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

package org.onosproject.net.intent.impl.installer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.IntentInstallCoordinator;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.impl.IntentManager;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.intent.IntentInstaller.Direction.ADD;
import static org.onosproject.net.intent.IntentInstaller.Direction.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for FlowRuleIntent.
 */
@Component(immediate = true)
public class FlowRuleIntentInstaller implements IntentInstaller<FlowRuleIntent> {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentInstallCoordinator intentInstallCoordinator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Activate
    public void activate() {
        intentExtensionService.registerInstaller(FlowRuleIntent.class, this);
    }

    @Deactivate
    public void deactivated() {
        intentExtensionService.unregisterInstaller(FlowRuleIntent.class);
    }

    protected final Logger log = getLogger(IntentManager.class);

    @Override
    public void apply(IntentOperationContext<FlowRuleIntent> context) {
        Optional<IntentData> toUninstall = context.toUninstall();
        Optional<IntentData> toInstall = context.toInstall();

        if (!toInstall.isPresent() && !toUninstall.isPresent()) {
            // Nothing to do.
            intentInstallCoordinator.intentInstallSuccess(context);
            return;
        }

        List<FlowRuleIntent> uninstallIntents = context.intentsToUninstall();
        List<FlowRuleIntent> installIntents = context.intentsToInstall();

        List<FlowRule> flowRulesToUninstall;
        List<FlowRule> flowRulesToInstall;

        if (toUninstall.isPresent()) {
            // Remove tracked resource from both Intent and installable Intents.
            trackIntentResources(toUninstall.get(), uninstallIntents, REMOVE);

            // Retrieves all flow rules from all flow rule Intents.
            flowRulesToUninstall = uninstallIntents.stream()
                    .map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            // No flow rules to be uninstalled.
            flowRulesToUninstall = Collections.emptyList();
        }

        if (toInstall.isPresent()) {
            // Track resource from both Intent and installable Intents.
            trackIntentResources(toInstall.get(), installIntents, ADD);

            // Retrieves all flow rules from all flow rule Intents.
            flowRulesToInstall = installIntents.stream()
                    .map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            // No flow rules to be installed.
            flowRulesToInstall = Collections.emptyList();
        }

        List<FlowRule> dontUninstall;
        List<FlowRule> dontTouch;

        // If both uninstall/install list contained equal (=match conditions are equal) FlowRules,
        // omit it from remove list, since it will/should be overwritten by install
        dontUninstall = flowRulesToUninstall.stream()
                .filter(flowRule -> flowRulesToInstall.stream().anyMatch(flowRule::equals))
                .collect(Collectors.toList());

        // If both contained exactMatch-ing FlowRules, remove from both list,
        // since it will result in no-op.
        dontTouch = flowRulesToInstall.stream()
                .filter(flowRule -> flowRulesToUninstall.stream().anyMatch(flowRule::exactMatch))
                .collect(Collectors.toList());

        flowRulesToUninstall.removeAll(dontUninstall);
        flowRulesToUninstall.removeAll(dontTouch);
        flowRulesToInstall.removeAll(dontTouch);

        if (flowRulesToInstall.isEmpty() && flowRulesToUninstall.isEmpty()) {
            // There is no flow rules to install/uninstall
            intentInstallCoordinator.intentInstallSuccess(context);
            return;
        }

        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        // Add flows
        flowRulesToInstall.forEach(builder::add);
        // Remove flows
        flowRulesToUninstall.forEach(builder::remove);

        FlowRuleOperationsContext flowRuleOperationsContext = new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                intentInstallCoordinator.intentInstallSuccess(context);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                intentInstallCoordinator.intentInstallFailed(context);
            }
        };

        FlowRuleOperations operations = builder.build(flowRuleOperationsContext);
        log.debug("applying intent {} -> {} with {} rules: {}",
                  toUninstall.map(x -> x.key().toString()).orElse("<empty>"),
                  toInstall.map(x -> x.key().toString()).orElse("<empty>"),
                  operations.stages().stream().mapToLong(Set::size).sum(),
                  operations.stages());
        flowRuleService.apply(operations);
    }

    /**
     * Track or un-track network resource of a Intent and it's installable
     * Intents.
     *
     * @param intentData the Intent data
     * @param intentsToApply the list of flow rule Intents from the Intent
     * @param direction the direction to determine track or un-track
     */
    private void trackIntentResources(IntentData intentData, List<FlowRuleIntent> intentsToApply, Direction direction) {
        switch (direction) {
            case ADD:
                trackerService.addTrackedResources(intentData.key(), intentData.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.addTrackedResources(intentData.key(),
                                                                                  installable.resources()));
                break;
            case REMOVE:
                trackerService.removeTrackedResources(intentData.key(), intentData.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.removeTrackedResources(intentData.intent().key(),
                                                                                     installable.resources()));
                break;
            default:
                log.warn("Unknown resource tracking direction.");
                break;
        }
    }
}