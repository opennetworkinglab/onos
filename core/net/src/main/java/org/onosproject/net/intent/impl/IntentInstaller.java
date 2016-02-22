/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.net.intent.impl;

import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentStore;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Auxiliary entity responsible for installing the intents into the environment.
 */
class IntentInstaller {

    private static final Logger log = getLogger(IntentManager.class);

    private IntentStore store;
    private ObjectiveTrackerService trackerService;
    private FlowRuleService flowRuleService;
    private FlowObjectiveService flowObjectiveService;

    private enum Direction {
        ADD,
        REMOVE
    }

    /**
     * Initializes the installer with references to required services.
     *
     * @param intentStore          intent store
     * @param trackerService       objective tracking service
     * @param flowRuleService      flow rule service
     * @param flowObjectiveService flow objective service
     */
    void init(IntentStore intentStore, ObjectiveTrackerService trackerService,
              FlowRuleService flowRuleService, FlowObjectiveService flowObjectiveService) {
        this.store = intentStore;
        this.trackerService = trackerService;
        this.flowRuleService = flowRuleService;
        this.flowObjectiveService = flowObjectiveService;
    }

    private void applyIntentData(Optional<IntentData> intentData,
                                 FlowRuleOperations.Builder builder,
                                 Direction direction) {
        if (!intentData.isPresent()) {
            return;
        }
        IntentData data = intentData.get();

        List<Intent> intentsToApply = data.installables();
        if (!intentsToApply.stream().allMatch(x -> x instanceof FlowRuleIntent)) {
            throw new IllegalStateException("installable intents must be FlowRuleIntent");
        }

        if (direction == Direction.ADD) {
            trackerService.addTrackedResources(data.key(), data.intent().resources());
            intentsToApply.forEach(installable ->
                                           trackerService.addTrackedResources(data.key(), installable.resources()));
        } else {
            trackerService.removeTrackedResources(data.key(), data.intent().resources());
            intentsToApply.forEach(installable ->
                                           trackerService.removeTrackedResources(data.intent().key(),
                                                                                 installable.resources()));
        }

        // FIXME do FlowRuleIntents have stages??? Can we do uninstall work in parallel? I think so.
        builder.newStage();

        List<Collection<FlowRule>> stages = intentsToApply.stream()
                .map(x -> (FlowRuleIntent) x)
                .map(FlowRuleIntent::flowRules)
                .collect(Collectors.toList());

        for (Collection<FlowRule> rules : stages) {
            if (direction == Direction.ADD) {
                rules.forEach(builder::add);
            } else {
                rules.forEach(builder::remove);
            }
        }

    }

    // FIXME: Refactor to accept both FlowObjectiveIntent and FlowRuleIntents
    // Note: Intent Manager should have never become dependent on a specific
    // intent type.

    /**
     * Applies the specified intent updates to the environment by uninstalling
     * and installing the intents and updating the store references appropriately.
     *
     * @param toUninstall optional intent to uninstall
     * @param toInstall   optional intent to install
     */
    void apply(Optional<IntentData> toUninstall, Optional<IntentData> toInstall) {
        // need to consider if FlowRuleIntent is only one as installable intent or not

        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        applyIntentData(toUninstall, builder, Direction.REMOVE);
        applyIntentData(toInstall, builder, Direction.ADD);

        FlowRuleOperations operations = builder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                if (toInstall.isPresent()) {
                    IntentData installData = toInstall.get();
                    log.debug("Completed installing: {}", installData.key());
                    installData.setState(INSTALLED);
                    store.write(installData);
                } else if (toUninstall.isPresent()) {
                    IntentData uninstallData = toUninstall.get();
                    log.debug("Completed withdrawing: {}", uninstallData.key());
                    switch (uninstallData.request()) {
                        case INSTALL_REQ:
                            uninstallData.setState(FAILED);
                            break;
                        case WITHDRAW_REQ:
                        default: //TODO "default" case should not happen
                            uninstallData.setState(WITHDRAWN);
                            break;
                    }
                    store.write(uninstallData);
                }
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                // if toInstall was cause of error, then recompile (manage/increment counter, when exceeded -> CORRUPT)
                if (toInstall.isPresent()) {
                    IntentData installData = toInstall.get();
                    log.warn("Failed installation: {} {} on {}",
                             installData.key(), installData.intent(), ops);
                    installData.setState(CORRUPT);
                    installData.incrementErrorCount();
                    store.write(installData);
                }
                // if toUninstall was cause of error, then CORRUPT (another job will clean this up)
                if (toUninstall.isPresent()) {
                    IntentData uninstallData = toUninstall.get();
                    log.warn("Failed withdrawal: {} {} on {}",
                             uninstallData.key(), uninstallData.intent(), ops);
                    uninstallData.setState(CORRUPT);
                    uninstallData.incrementErrorCount();
                    store.write(uninstallData);
                }
            }
        });

        if (log.isTraceEnabled()) {
            log.trace("applying intent {} -> {} with {} rules: {}",
                      toUninstall.map(x -> x.key().toString()).orElse("<empty>"),
                      toInstall.map(x -> x.key().toString()).orElse("<empty>"),
                      operations.stages().stream().mapToLong(Set::size).sum(),
                      operations.stages());
        }

        flowRuleService.apply(operations);
    }
}
