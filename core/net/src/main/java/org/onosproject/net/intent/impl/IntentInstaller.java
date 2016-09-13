/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentStore;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
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


    // FIXME: Refactor to accept both FlowObjectiveIntent and FlowRuleIntents
    // FIXME: Intent Manager should have never become dependent on a specific intent type(s).
    // This will be addressed in intent domains work; not now.

    /**
     * Applies the specified intent updates to the environment by uninstalling
     * and installing the intents and updating the store references appropriately.
     *
     * @param toUninstall optional intent to uninstall
     * @param toInstall   optional intent to install
     */
    void apply(Optional<IntentData> toUninstall, Optional<IntentData> toInstall) {
        // Hook for handling success
        Consumer<OperationContext> successConsumer = (ctx) -> {
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
        };

        // Hook for handling errors
        Consumer<OperationContext> errorConsumer = (ctx) -> {
            // if toInstall was cause of error, then recompile (manage/increment counter, when exceeded -> CORRUPT)
            if (toInstall.isPresent()) {
                IntentData installData = toInstall.get();
                log.warn("Failed installation: {} {} due to {}",
                         installData.key(), installData.intent(), ctx.error());
                installData.setState(CORRUPT);
                installData.incrementErrorCount();
                store.write(installData);
            }
            // if toUninstall was cause of error, then CORRUPT (another job will clean this up)
            if (toUninstall.isPresent()) {
                IntentData uninstallData = toUninstall.get();
                log.warn("Failed withdrawal: {} {} due to {}",
                         uninstallData.key(), uninstallData.intent(), ctx.error());
                uninstallData.setState(CORRUPT);
                uninstallData.incrementErrorCount();
                store.write(uninstallData);
            }
        };

        // Create a context for tracking the backing operations for applying
        // the intents to the environment.
        OperationContext context = createContext(toUninstall, toInstall);

        context.prepare(toUninstall, toInstall, successConsumer, errorConsumer);
        context.apply();
    }

    // ------ Utilities to support FlowRule vs. FlowObjective behavior -------

    // Creates the context appropriate for tracking operations of the
    // the specified intents.
    private OperationContext createContext(Optional<IntentData> toUninstall,
                                           Optional<IntentData> toInstall) {
        if (isInstallable(toUninstall, toInstall, FlowRuleIntent.class)) {
            return new FlowRuleOperationContext();
        }
        if (isInstallable(toUninstall, toInstall, FlowObjectiveIntent.class)) {
            return new FlowObjectiveOperationContext();
        }
        return new ErrorContext();
    }

    private boolean isInstallable(Optional<IntentData> toUninstall, Optional<IntentData> toInstall,
                                  Class<? extends Intent> intentClass) {
        boolean notBothNull = false;
        if (toInstall.isPresent()) {
            notBothNull = true;
            if (!toInstall.get().installables().stream()
                    .allMatch(i -> intentClass.isAssignableFrom(i.getClass()))) {
                return false;
            }
        }
        if (toUninstall.isPresent()) {
            notBothNull = true;
            if (!toUninstall.get().installables().stream()
                    .allMatch(i -> intentClass.isAssignableFrom(i.getClass()))) {
                return false;
            }
        }
        return notBothNull;
    }

    // Base context for applying and tracking operations related to installable intents.
    private abstract class OperationContext {
        protected Optional<IntentData> toUninstall;
        protected Optional<IntentData> toInstall;
        protected Consumer<OperationContext> successConsumer;
        protected Consumer<OperationContext> errorConsumer;

        abstract void apply();

        abstract Object error();

        abstract void prepareIntents(List<Intent> intentsToApply, Direction direction);

        void prepare(Optional<IntentData> toUninstall, Optional<IntentData> toInstall,
                     Consumer<OperationContext> successConsumer,
                     Consumer<OperationContext> errorConsumer) {
            this.toUninstall = toUninstall;
            this.toInstall = toInstall;
            this.successConsumer = successConsumer;
            this.errorConsumer = errorConsumer;
            prepareIntentData(toUninstall, toInstall);
        }

        private void prepareIntentData(Optional<IntentData> uninstallData,
                                       Optional<IntentData> installData) {
            if (!installData.isPresent() && !uninstallData.isPresent()) {
                return;
            } else if (!installData.isPresent()) {
                prepareIntentData(uninstallData, Direction.REMOVE);
            } else if (!uninstallData.isPresent()) {
                prepareIntentData(installData, Direction.ADD);
            } else {
                IntentData uninstall = uninstallData.get();
                IntentData install = installData.get();
                List<Intent> uninstallIntents = Lists.newArrayList(uninstall.installables());
                List<Intent> installIntents = Lists.newArrayList(install.installables());

                checkState(uninstallIntents.stream().allMatch(this::isSupported),
                           "Unsupported installable intents detected");
                checkState(installIntents.stream().allMatch(this::isSupported),
                           "Unsupported installable intents detected");

                //TODO: Filter FlowObjective intents
                // Filter out same intents and intents with same flow rules
                Iterator<Intent> iterator = installIntents.iterator();
                while (iterator.hasNext()) {
                    Intent installIntent = iterator.next();
                    uninstallIntents.stream().filter(uIntent -> {
                        if (uIntent.equals(installIntent)) {
                            return true;
                        } else if (uIntent instanceof FlowRuleIntent && installIntent instanceof FlowRuleIntent) {
                            //FIXME we can further optimize this by doing the filtering on a flow-by-flow basis
                            //      (direction can be implied from intent state)
                            return ((FlowRuleIntent) uIntent).flowRules()
                                    .containsAll(((FlowRuleIntent) installIntent).flowRules());
                        } else {
                            return false;
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

                final IntentData newUninstall = new IntentData(uninstall, uninstallIntents);
                final IntentData newInstall = new IntentData(install, installIntents);

                trackerService.removeTrackedResources(newUninstall.key(), newUninstall.intent().resources());
                uninstallIntents.forEach(installable ->
                                                 trackerService.removeTrackedResources(newUninstall.intent().key(),
                                                                                       installable.resources()));
                trackerService.addTrackedResources(newInstall.key(), newInstall.intent().resources());
                installIntents.forEach(installable ->
                                               trackerService.addTrackedResources(newInstall.key(),
                                                                                  installable.resources()));
                prepareIntents(uninstallIntents, Direction.REMOVE);
                prepareIntents(installIntents, Direction.ADD);
            }
        }

        /**
         * Applies the specified intent data, if present, to the network using the
         * specified context.
         *
         * @param intentData optional intent data; no-op if not present
         * @param direction  indicates adding or removal
         */
        private void prepareIntentData(Optional<IntentData> intentData, Direction direction) {
            if (!intentData.isPresent()) {
                return;
            }

            IntentData data = intentData.get();
            List<Intent> intentsToApply = data.installables();
            checkState(intentsToApply.stream().allMatch(this::isSupported),
                       "Unsupported installable intents detected");

            if (direction == Direction.ADD) {
                trackerService.addTrackedResources(data.key(), data.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.addTrackedResources(data.key(),
                                                                                  installable.resources()));
            } else {
                trackerService.removeTrackedResources(data.key(), data.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.removeTrackedResources(data.intent().key(),
                                                                                     installable.resources()));
            }

            prepareIntents(intentsToApply, direction);
        }

        private boolean isSupported(Intent intent) {
            return intent instanceof FlowRuleIntent || intent instanceof FlowObjectiveIntent;
        }
    }


    // Context for applying and tracking operations related to flow rule intent.
    private class FlowRuleOperationContext extends OperationContext {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        FlowRuleOperationsContext flowRuleOperationsContext;

        void apply() {
            flowRuleOperationsContext = new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    successConsumer.accept(FlowRuleOperationContext.this);
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    errorConsumer.accept(FlowRuleOperationContext.this);
                }
            };
            FlowRuleOperations operations = builder.build(flowRuleOperationsContext);

            if (log.isTraceEnabled()) {
                log.trace("applying intent {} -> {} with {} rules: {}",
                          toUninstall.map(x -> x.key().toString()).orElse("<empty>"),
                          toInstall.map(x -> x.key().toString()).orElse("<empty>"),
                          operations.stages().stream().mapToLong(Set::size).sum(),
                          operations.stages());
            }

            flowRuleService.apply(operations);
        }

        @Override
        public void prepareIntents(List<Intent> intentsToApply, Direction direction) {
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

        @Override
        public Object error() {
            return flowRuleOperationsContext;
        }
    }

    // Context for applying and tracking operations related to flow objective intents.
    private class FlowObjectiveOperationContext extends OperationContext {
        List<FlowObjectiveInstallationContext> contexts = Lists.newLinkedList();
        final Set<ObjectiveContext> pendingContexts = Sets.newHashSet();
        final Set<ObjectiveContext> errorContexts = Sets.newConcurrentHashSet();

        @Override
        public void prepareIntents(List<Intent> intentsToApply, Direction direction) {
            intentsToApply.stream()
                    .flatMap(x -> buildObjectiveContexts((FlowObjectiveIntent) x, direction).stream())
                    .forEach(contexts::add);
        }

        // Builds the specified objective in the appropriate direction
        private List<FlowObjectiveInstallationContext> buildObjectiveContexts(FlowObjectiveIntent intent,
                                                                              Direction direction) {
            int size = intent.objectives().size();
            List<FlowObjectiveInstallationContext> contexts = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                DeviceId deviceId = intent.devices().get(i);
                Objective.Builder builder = intent.objectives().get(i).copy();
                FlowObjectiveInstallationContext context = new FlowObjectiveInstallationContext();

                final Objective objective;
                switch (direction) {
                    case ADD:
                        objective = builder.add(context);
                        break;
                    case REMOVE:
                        objective = builder.remove(context);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported direction " + direction);
                }
                context.setObjective(objective, deviceId);
                contexts.add(context);
            }
            return contexts;
        }

        @Override
        void apply() {
            contexts.forEach(objectiveContext -> {
                pendingContexts.add(objectiveContext);
                flowObjectiveService.apply(objectiveContext.deviceId,
                                           objectiveContext.objective);
            });
        }

        @Override
        public Object error() {
            return errorContexts;
        }

        private class FlowObjectiveInstallationContext implements ObjectiveContext {
            Objective objective;
            DeviceId deviceId;
            ObjectiveError error;

            void setObjective(Objective objective, DeviceId deviceId) {
                this.objective = objective;
                this.deviceId = deviceId;
            }

            @Override
            public void onSuccess(Objective objective) {
                finish();
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                this.error = error;
                errorContexts.add(this);
                finish();
            }

            private void finish() {
                synchronized (pendingContexts) {
                    pendingContexts.remove(this);
                    if (pendingContexts.isEmpty()) {
                        if (errorContexts.isEmpty()) {
                            successConsumer.accept(FlowObjectiveOperationContext.this);
                        } else {
                            errorConsumer.accept(FlowObjectiveOperationContext.this);
                        }
                    }
                }
            }

            @Override
            public String toString() {
                return String.format("(%s on %s for %s)", error, deviceId, objective);
            }
        }
    }

    private class ErrorContext extends OperationContext {
        @Override
        void apply() {
            throw new UnsupportedOperationException("Unsupported installable intent");
        }

        @Override
        Object error() {
            return null;
        }

        @Override
        void prepareIntents(List<Intent> intentsToApply, Direction direction) {
        }
    }
}
