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

import com.google.common.collect.Lists;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.intent.IntentInstaller.Direction.ADD;
import static org.onosproject.net.intent.IntentInstaller.Direction.REMOVE;
import static org.onosproject.net.intent.IntentState.INSTALLED;
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

        List<FlowRuleIntent> uninstallIntents = Lists.newArrayList(context.intentsToUninstall());
        List<FlowRuleIntent> installIntents = Lists.newArrayList(context.intentsToInstall());

        if (!toInstall.isPresent() && !toUninstall.isPresent()) {
            intentInstallCoordinator.intentInstallSuccess(context);
            return;
        } else if (!toInstall.isPresent()) {
            // Uninstall only
            trackIntentResources(toUninstall.get(), uninstallIntents, REMOVE);
        } else if (!toUninstall.isPresent()) {
            // Install only
            trackIntentResources(toInstall.get(), installIntents, ADD);
        } else {
            IntentData uninstall = toUninstall.get();
            IntentData install = toInstall.get();
            // Filter out same intents and intents with same flow rules
            Iterator<FlowRuleIntent> iterator = installIntents.iterator();
            while (iterator.hasNext()) {
                FlowRuleIntent installIntent = iterator.next();
                uninstallIntents.stream().filter(uIntent -> {
                    if (uIntent.equals(installIntent)) {
                        return true;
                    } else {
                        return !flowRuleIntentChanged(uIntent, installIntent);
                    }
                }).findFirst().ifPresent(common -> {
                    uninstallIntents.remove(common);
                    if (INSTALLED.equals(uninstall.state())) {
                        // only remove the install intent if the existing
                        // intent (i.e. the uninstall one) is already
                        // installed or installing
                        iterator.remove();
                    }
                });
            }
            trackIntentResources(uninstall, uninstallIntents, REMOVE);
            trackIntentResources(install, installIntents, ADD);
        }

        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        builder.newStage();

        toUninstall.ifPresent(intentData -> {
            uninstallIntents.stream().map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream).forEach(builder::remove);
        });

        toInstall.ifPresent(intentData -> {
            installIntents.stream().map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream).forEach(builder::add);
        });

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
            default:
                trackerService.removeTrackedResources(intentData.key(), intentData.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.removeTrackedResources(intentData.intent().key(),
                                                                                     installable.resources()));
                break;
        }
    }

    /**
     * Determines whether there is any flow rule changed
     * (i.e., different set of flow rules or different treatments)
     * between FlowRuleIntents to be uninstalled and to be installed.
     *
     * @param uninstallIntent FlowRuleIntent to uninstall
     * @param installIntent   FlowRuleIntent to install
     * @return true if flow rules which to be uninstalled contains all flow
     *         rules which to be installed; false otherwise
     */
    private boolean flowRuleIntentChanged(FlowRuleIntent uninstallIntent,
                                          FlowRuleIntent installIntent) {
        Collection<FlowRule> flowRulesToUninstall = uninstallIntent.flowRules();
        Collection<FlowRule> flowRulesToInstall = installIntent.flowRules();

        // Check if any flow rule changed
        for (FlowRule flowRuleToInstall : flowRulesToInstall) {
            if (flowRulesToUninstall.stream().noneMatch(flowRuleToInstall::exactMatch)) {
                return true;
            }
        }
        return false;
    }

}