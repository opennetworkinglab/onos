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

import com.google.common.collect.ImmutableSet;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.ProtectionConfig;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.domain.DomainIntent;
import org.onosproject.net.domain.DomainIntentOperations;
import org.onosproject.net.domain.DomainIntentOperationsContext;
import org.onosproject.net.domain.DomainIntentService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.flowobjective.ObjectiveError.INSTALLATIONTHRESHOLDEXCEEDED;
import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Auxiliary entity responsible for installing the intents into the environment.
 */
class IntentInstaller {

    private static final Logger log = getLogger(IntentInstaller.class);
    private static final long OBJECTIVE_RETRY_THRESHOLD = 5;

    private IntentStore store;
    private ObjectiveTrackerService trackerService;
    private FlowRuleService flowRuleService;
    private FlowObjectiveService flowObjectiveService;
    private NetworkConfigService networkConfigService;
    private DomainIntentService domainIntentService;

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
     * @param networkConfigService network configuration service
     * @param domainIntentService  domain intent service
     */
    void init(IntentStore intentStore, ObjectiveTrackerService trackerService,
              FlowRuleService flowRuleService, FlowObjectiveService flowObjectiveService,
              NetworkConfigService networkConfigService, DomainIntentService domainIntentService) {

        this.store = intentStore;
        this.trackerService = trackerService;
        //TODO Various services should be plugged to the intent installer instead of being hardcoded
        this.flowRuleService = flowRuleService;
        this.flowObjectiveService = flowObjectiveService;
        this.networkConfigService = networkConfigService;
        this.domainIntentService = domainIntentService;
    }

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
        // Hook for handling success at intent installation level.
        Consumer<IntentInstallationContext> successConsumer = (ctx) -> {
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
                // Intent has been withdrawn; we can clear the installables
                store.write(new IntentData(uninstallData, Collections.emptyList()));
            }
        };

        // Hook for handling errors at intent installation level
        Consumer<IntentInstallationContext> errorConsumer = (ctx) -> {
            // if toInstall was cause of error, then recompile (manage/increment counter, when exceeded -> CORRUPT)
            if (toInstall.isPresent()) {
                IntentData installData = toInstall.get();
                installData.setState(CORRUPT);
                installData.incrementErrorCount();
                store.write(installData);
            }
            // if toUninstall was cause of error, then CORRUPT (another job will clean this up)
            if (toUninstall.isPresent()) {
                IntentData uninstallData = toUninstall.get();
                uninstallData.setState(CORRUPT);
                uninstallData.incrementErrorCount();
                store.write(uninstallData);
            }
        };

        // Hooks at operation level
        Consumer<OperationContext> successOperationConsumer = (ctx) -> {
            ctx.intentContext.finishContext(ctx);
        };
        Consumer<OperationContext> errorOperationConsumer = (ctx) -> {
            if (ctx.toInstall.isPresent()) {
                IntentData installData = toInstall.get();
                log.warn("Failed installation operation for: {} {} due to {}",
                         installData.key(), installData.intent(), ctx.error());
            }
            if (ctx.toUninstall.isPresent()) {
                IntentData uninstallData = toUninstall.get();
                log.warn("Failed withdrawal operation for: {} {} due to {}",
                         uninstallData.key(), uninstallData.intent(), ctx.error());
            }
            ctx.intentContext.handleError(ctx);
        };

        // Create a context for tracking the backing operations for applying
        // the intents to the environment.
        IntentInstallationContext intentContext =
                new IntentInstallationContext(successConsumer, errorConsumer);
        Set<OperationContext> contexts = createContext(intentContext, toUninstall, toInstall);
        intentContext.pendingContexts = contexts;
        contexts.forEach(ctx -> {
            ctx.prepare(toUninstall, toInstall, successOperationConsumer, errorOperationConsumer);
            ctx.apply();
        });
    }

    // Context for applying and tracking multiple kinds of operation contexts
    // related to specific intent data.
    private final class IntentInstallationContext {
        private Set<OperationContext> pendingContexts = Sets.newHashSet();
        private Set<OperationContext> errorContexts = Sets.newHashSet();
        private Consumer<IntentInstallationContext> successConsumer;
        private Consumer<IntentInstallationContext> errorConsumer;

        private IntentInstallationContext(Consumer<IntentInstallationContext> succesConsumer,
                                          Consumer<IntentInstallationContext> errorConsumer) {
            this.successConsumer = succesConsumer;
            this.errorConsumer = errorConsumer;
        }

        private void handleError(OperationContext ctx) {
            errorContexts.add(ctx);
            finishContext(ctx);
        }

        private void finishContext(OperationContext ctx) {
            synchronized (pendingContexts) {
                pendingContexts.remove(ctx);
                if (pendingContexts.isEmpty()) {
                    if (errorContexts.isEmpty()) {
                        successConsumer.accept(IntentInstallationContext.this);
                    } else {
                        errorConsumer.accept(IntentInstallationContext.this);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pendingContexts", pendingContexts)
                    .add("errorContexts", errorContexts)
                    .toString();
        }
    }

    // --- Utilities to support FlowRule vs. FlowObjective vs. DomainIntent behavior ----

    // Creates the set of contexts appropriate for tracking operations of the
    // the specified intents.
    private Set<OperationContext> createContext(IntentInstallationContext intentContext,
                                                Optional<IntentData> toUninstall,
                                                Optional<IntentData> toInstall) {

        Set<OperationContext> contexts = Sets.newConcurrentHashSet();
        if (isInstallable(toUninstall, toInstall, FlowRuleIntent.class)) {
            contexts.add(new FlowRuleOperationContext(intentContext));
        }
        if (isInstallable(toUninstall, toInstall, FlowObjectiveIntent.class)) {
            contexts.add(new FlowObjectiveOperationContext(intentContext));
        }
        if (isInstallable(toUninstall, toInstall, ProtectionEndpointIntent.class)) {
            contexts.add(new ProtectionConfigOperationContext(intentContext));
        }
        if (isInstallable(toUninstall, toInstall, DomainIntent.class)) {
            contexts.add(new DomainIntentOperationContext(intentContext));
        }

        if (contexts.isEmpty()) {
            log.warn("{} did not contain installable Intents", intentContext);
            return ImmutableSet.of(new ErrorContext(intentContext));
        }

        return contexts;
    }

    /**
     * Tests if one of {@code toUninstall} or {@code toInstall} contains
     * installable Intent of type specified by {@code intentClass}.
     *
     * @param toUninstall IntentData to test
     * @param toInstall   IntentData to test
     * @param intentClass installable Intent class
     * @return true if at least one of IntentData contains installable specified.
     */
    private boolean isInstallable(Optional<IntentData> toUninstall, Optional<IntentData> toInstall,
                                  Class<? extends Intent> intentClass) {

        return Stream.concat(toInstall
                              .map(IntentData::installables)
                              .map(Collection::stream)
                              .orElse(Stream.empty()),
                             toUninstall
                              .map(IntentData::installables)
                              .map(Collection::stream)
                              .orElse(Stream.empty()))
                .anyMatch(i -> intentClass.isAssignableFrom(i.getClass()));
    }

    // Base context for applying and tracking operations related to installable intents.
    private abstract class OperationContext {
        protected IntentInstallationContext intentContext;
        protected Optional<IntentData> toUninstall;
        protected Optional<IntentData> toInstall;
        /**
         * Implementation of {@link OperationContext} should call this on success.
         */
        protected Consumer<OperationContext> successConsumer;
        /**
         * Implementation of {@link OperationContext} should call this on error.
         */
        protected Consumer<OperationContext> errorConsumer;

        protected OperationContext(IntentInstallationContext context) {
            this.intentContext = context;
        }

        /**
         * Applies the Intents specified by
         * {@link #prepareIntents(List, Direction)} call(s) prior to this call.
         */
        abstract void apply();

        /**
         * Returns error state of the context.
         * <p>
         * Used for error logging purpose.
         * Returned Object should have reasonable toString() implementation.
         * @return context state, describing current error state
         */
        abstract Object error();

        /**
         * Prepares Intent(s) to {@link #apply() apply} in this operation.
         * <p>
         * Intents specified by {@code intentsToApply} in a single call
         * can be applied to the Devices in arbitrary order.
         * But group of Intents specified in consecutive {@link #prepareIntents(List, Direction)}
         * calls must be applied in order. (e.g., guarded by barrier)
         *
         * @param intentsToApply {@link Intent}s to apply
         * @param direction of operation
         */
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
                           "Unsupported installable intents detected: %s", uninstallIntents);
                checkState(installIntents.stream().allMatch(this::isSupported),
                           "Unsupported installable intents detected: %s", installIntents);

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
                            return !flowRuleIntentChanged(((FlowRuleIntent) uIntent),
                                                          ((FlowRuleIntent) installIntent));
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
         * Determines whether there is any flow rule changed
         * (i.e., different set of flow rules or different treatments)
         * between FlowRuleIntents to be uninstalled and to be installed.
         *
         * @param uninstallIntent FlowRuleIntent to uninstall
         * @param installIntent FlowRuleIntent to install
         * @return true if flow rules which to be uninstalled
         * contains all flow rules which to be installed.
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
                       "Unsupported installable intents detected: %s", intentsToApply);

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
            return intent instanceof FlowRuleIntent ||
                   intent instanceof FlowObjectiveIntent ||
                   intent instanceof ProtectionEndpointIntent ||
                   intent instanceof DomainIntent;
        }

        protected ToStringHelper toStringHelper() {
            return MoreObjects.toStringHelper(this)
                    .add("intentContext", intentContext)
                    .add("toUninstall", toUninstall)
                    .add("toInstall", toInstall);
        }

        @Override
        public String toString() {
            return toStringHelper()
                    .toString();
        }
    }


    // Context for applying and tracking operations related to flow rule intents.
    private class FlowRuleOperationContext extends OperationContext {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        FlowRuleOperationsContext flowRuleOperationsContext;

        FlowRuleOperationContext(IntentInstallationContext context) {
            super(context);
        }

        @Override
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
                    .filter(x -> x instanceof FlowRuleIntent)
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

        @Override
        protected ToStringHelper toStringHelper() {
            return super.toStringHelper()
                    .omitNullValues()
                    .add("flowRuleOperationsContext", flowRuleOperationsContext);
        }
    }

    // Context for applying and tracking operations related to flow objective intents.
    private class FlowObjectiveOperationContext extends OperationContext {
        private static final String UNSUPPORT_OBJ = "unsupported objective {}";
        final List<ObjectiveContext> contexts = Lists.newArrayList();

        final Set<ObjectiveContext> pendingContexts = Sets.newConcurrentHashSet();

        // Second stage of pending contexts
        final Set<ObjectiveContext> nextPendingContexts = Sets.newConcurrentHashSet();
        final Set<ObjectiveContext> errorContexts = Sets.newConcurrentHashSet();

        FlowObjectiveOperationContext(IntentInstallationContext context) {
            super(context);
        }

        @Override
        public void prepareIntents(List<Intent> intentsToApply, Direction direction) {
            intentsToApply
                .stream()
                .filter(intent -> intent instanceof FlowObjectiveIntent)
                .map(intent -> buildObjectiveContexts((FlowObjectiveIntent) intent, direction))
                .flatMap(Collection::stream)
                .forEach(contexts::add);

            // Two stage for different direction context
            // We will apply REMOVE context first, and apply ADD context.
            contexts.forEach(context -> {
                switch (direction) {
                    case REMOVE:
                        pendingContexts.add(context);
                        break;
                    case ADD:
                        nextPendingContexts.add(context);
                        break;
                    default:
                        break;
                }
            });
        }

        // Builds the specified objective in the appropriate direction
        private Set<? extends ObjectiveContext> buildObjectiveContexts(FlowObjectiveIntent intent,
                                            Direction direction) {
            Set<FlowObjectiveInstallationContext> contexts = Sets.newHashSet();
            int size = intent.objectives().size();
            List<Objective> objectives = intent.objectives();
            List<DeviceId> deviceIds = intent.devices();

            if (direction == Direction.ADD) {
                for (int i = 0; i < size; i++) {
                    Objective objective = objectives.get(i);
                    DeviceId deviceId = deviceIds.get(i);
                    FlowObjectiveInstallationContext ctx =
                            buildObjectiveContext(objective, deviceId, direction);
                    contexts.add(ctx);
                }
                return contexts;
            } else {
                // we need to care about ordering here
                // basic idea is to chain objective contexts
                for (int i = 0; i < size; i++) {
                    Objective objective = intent.objectives().get(i);
                    DeviceId deviceId = intent.devices().get(i);

                    if (objective instanceof FilteringObjective) {
                        // don't need to care ordering of filtering objective
                        FlowObjectiveInstallationContext ctx =
                                buildObjectiveContext(objective, deviceId, direction);
                        contexts.add(ctx);
                    } else if (objective instanceof NextObjective) {
                        // need to removed after forwarding objective
                        // nothing to do here
                    } else if (objective instanceof ForwardingObjective) {
                        // forwarding objective, also find next objective if
                        // exist
                        FlowObjectiveInstallationContext fwdCtx =
                                buildObjectiveContext(objective, deviceId, direction);
                        ForwardingObjective fwd = (ForwardingObjective) objective;
                        NextObjective nxt = null;
                        Integer nextId = fwd.nextId();
                        if (nextId != null) {
                            for (int j = 0; j < size; j++) {
                                if (objectives.get(j).id() == nextId) {
                                    nxt = (NextObjective) objectives.get(j);
                                    break;
                                }
                            }
                            // if a next objective exists in the Intent
                            if (nxt != null) {
                                FlowObjectiveInstallationContext nxtCtx =
                                        buildObjectiveContext(nxt, deviceId, direction);
                                fwdCtx.nextContext(nxtCtx);
                            }
                        }
                        contexts.add(fwdCtx);
                    } else {
                        // possible here?
                        log.warn(UNSUPPORT_OBJ, objective);
                    }
                }
            }
            return contexts;
        }

        private FlowObjectiveInstallationContext buildObjectiveContext(Objective objective,
                                                                       DeviceId deviceId,
                                                                       Direction direction) {
            Objective.Builder builder = objective.copy();
            FlowObjectiveInstallationContext ctx = new FlowObjectiveInstallationContext();
            switch (direction) {
                case ADD:
                    objective = builder.add(ctx);
                    break;
                case REMOVE:
                    objective = builder.remove(ctx);
                    break;
                default:
                    break;
            }
            ctx.setObjective(objective, deviceId);
            return ctx;
        }

        @Override
        void apply() {
            // If there is no pending contexts, try apply second stage
            // pending contexts
            if (pendingContexts.isEmpty()) {
                pendingContexts.addAll(nextPendingContexts);
                nextPendingContexts.clear();
            }
            final Set<ObjectiveContext> contextsToApply = Sets.newHashSet(pendingContexts);
            contextsToApply.forEach(ctx -> {
                FlowObjectiveInstallationContext foiCtx =
                        (FlowObjectiveInstallationContext) ctx;

                flowObjectiveService.apply(foiCtx.deviceId, foiCtx.objective);
            });
        }

        @Override
        public Object error() {
            return errorContexts;
        }

        @Override
        protected ToStringHelper toStringHelper() {
            return super.toStringHelper()
                    .add("contexts", contexts)
                    .add("pendingContexts", pendingContexts)
                    .add("errorContexts", errorContexts);
        }

        private class FlowObjectiveInstallationContext implements ObjectiveContext {
            Objective objective;
            DeviceId deviceId;
            ObjectiveError error;
            AtomicInteger retry;
            FlowObjectiveInstallationContext nextContext;

            void setObjective(Objective objective, DeviceId deviceId) {
                // init function
                this.objective = objective;
                this.deviceId = deviceId;
                this.error = null;
                this.retry = new AtomicInteger(0);
                this.nextContext = null;
            }

            int retryTimes() {
                return this.retry.get();
            }

            void increaseRetryValue() {
                this.retry.incrementAndGet();
            }

            private void finished(ObjectiveError error) {

                synchronized (pendingContexts) {
                    if (error != null) {
                        this.error = error;
                        handleObjectiveError(this, error);
                    } else {
                        // apply next context if exist
                        if (nextContext != null) {
                            pendingContexts.add(nextContext);
                            flowObjectiveService.apply(nextContext.deviceId,
                                                       nextContext.objective);
                            pendingContexts.remove(this);
                        } else {
                            pendingContexts.remove(this);
                        }
                    }
                    if (!pendingContexts.isEmpty()) {
                        return;
                    }
                    // Apply second stage pending contexts if it is not empty
                    if (!nextPendingContexts.isEmpty()) {
                        pendingContexts.addAll(nextPendingContexts);
                        nextPendingContexts.clear();
                        final Set<ObjectiveContext> contextsToApply =
                                Sets.newHashSet(pendingContexts);
                        contextsToApply.forEach(ctx -> {
                            FlowObjectiveInstallationContext foiCtx =
                                    (FlowObjectiveInstallationContext) ctx;
                            flowObjectiveService.apply(foiCtx.deviceId,
                                                       foiCtx.objective);
                        });
                        return;
                    }
                    if (errorContexts.isEmpty()) {
                        successConsumer.accept(FlowObjectiveOperationContext.this);
                    } else {
                        errorConsumer.accept(FlowObjectiveOperationContext.this);
                    }
                }
            }

            @Override
            public void onSuccess(Objective objective) {
                finished(null);
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                finished(error);
            }

            @Override
            public String toString() {
                return String.format("(%s on %s for %s)", error, deviceId, objective);
            }

            public void nextContext(FlowObjectiveInstallationContext nextContext) {
                this.nextContext = nextContext;
            }
        }

        private void handleObjectiveError(FlowObjectiveInstallationContext ctx,
                                          ObjectiveError error) {
            log.debug("Got error(s) when install objective: {}, error: {}, retry: {}",
                      ctx.objective, ctx.error, ctx.retry);
            if (ctx.retryTimes() > OBJECTIVE_RETRY_THRESHOLD) {
                ctx.error = INSTALLATIONTHRESHOLDEXCEEDED;
                errorContexts.add(ctx);
                return;
            }
            // reset error
            ctx.error = null;
            // strategies for errors
            switch (error) {
                case GROUPEXISTS:
                    if (ctx.objective.op() == Objective.Operation.ADD) {
                        // Next group exists
                        // build new objective with new op ADD_TO_EXIST
                        NextObjective newObj =
                                ((NextObjective.Builder) ctx.objective.copy()).addToExisting(ctx);
                        ctx.setObjective(newObj, ctx.deviceId);
                        ctx.increaseRetryValue();
                        flowObjectiveService.apply(ctx.deviceId, ctx.objective);
                    } else {
                        pendingContexts.remove(ctx);
                        errorContexts.add(ctx);
                    }
                    break;
                case GROUPINSTALLATIONFAILED:
                    // Group install failed, retry again
                    ctx.increaseRetryValue();
                    flowObjectiveService.apply(ctx.deviceId, ctx.objective);
                    break;
                case GROUPMISSING:
                    if (ctx.objective.op() == Objective.Operation.ADD_TO_EXISTING) {
                        // Next group not exist, but we want to add new buckets
                        // build new objective with new op ADD
                        NextObjective newObj = (NextObjective) ctx.objective.copy().add(ctx);
                        ctx.setObjective(newObj, ctx.deviceId);
                        ctx.increaseRetryValue();
                        flowObjectiveService.apply(ctx.deviceId, ctx.objective);
                    } else if (ctx.objective.op() == Objective.Operation.REMOVE ||
                            ctx.objective.op() == Objective.Operation.REMOVE_FROM_EXISTING) {
                        // Already removed, no need to do anything
                        ctx.error = null;
                        pendingContexts.remove(ctx);
                        return;
                    } else {
                        // Next chaining group missing, try again.
                        ctx.increaseRetryValue();
                        flowObjectiveService.apply(ctx.deviceId, ctx.objective);
                    }
                    break;
                case FLOWINSTALLATIONFAILED:
                case GROUPREMOVALFAILED:
                case INSTALLATIONTIMEOUT:
                    // Retry
                    ctx.increaseRetryValue();
                    flowObjectiveService.apply(ctx.deviceId, ctx.objective);
                    break;
                default:
                    pendingContexts.remove(ctx);
                    errorContexts.add(ctx);
                    break;
            }
        }
    }

    // Context for applying and tracking operations related to domain intents.
    private class DomainIntentOperationContext extends OperationContext {
        DomainIntentOperations.Builder builder = DomainIntentOperations.builder();
        DomainIntentOperationsContext domainOperationsContext;

        DomainIntentOperationContext(IntentInstallationContext context) {
            super(context);
        }
        @Override
        void apply() {
            domainOperationsContext = new DomainIntentOperationsContext() {
                @Override
                public void onSuccess(DomainIntentOperations ops) {
                    successConsumer.accept(DomainIntentOperationContext.this);
                }

                @Override
                public void onError(DomainIntentOperations ops) {
                    errorConsumer.accept(DomainIntentOperationContext.this);
                }
            };
            DomainIntentOperations operations = builder.build(domainOperationsContext);

            if (log.isTraceEnabled()) {
                log.trace("submitting domain intent {} -> {}",
                          toUninstall.map(x -> x.key().toString()).orElse("<empty>"),
                          toInstall.map(x -> x.key().toString()).orElse("<empty>"));
            }
            domainIntentService.sumbit(operations);
        }

        @Override
        public void prepareIntents(List<Intent> intentsToApply, Direction direction) {
            List<DomainIntent> intents = intentsToApply.stream()
                    .filter(x -> x instanceof DomainIntent)
                    .map(x -> (DomainIntent) x)
                    .collect(Collectors.toList());

            for (DomainIntent intent : intents) {
                if (direction == Direction.ADD) {
                    builder.add(intent);
                } else {
                    builder.remove(intent);
                }
            }
        }

        @Override
        public Object error() {
            return domainOperationsContext;
        }
    }

    private class ErrorContext extends OperationContext {
        ErrorContext(IntentInstallationContext context) {
            super(context);
        }
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


    /**
     * Context for applying and tracking operations related to
     * {@link ProtectionEndpointIntent}.
     */
    @Beta
    private class ProtectionConfigOperationContext extends OperationContext {

        ProtectionConfigOperationContext(IntentInstallationContext context) {
            super(context);
        }

        /**
         * Stage of installable Intents which can be processed in parallel.
         */
        private final class Stage {
            // should it have progress state, how far it went?
            private final Collection<Pair<ProtectionEndpointIntent, Direction>> ops;

            Stage(Collection<Pair<ProtectionEndpointIntent, Direction>> ops) {
                this.ops = checkNotNull(ops);
            }

            CompletableFuture<Void> apply() {
                return ops.stream()
                    .map(op -> applyOp(op.getRight(), op.getLeft()))
                    .reduce(CompletableFuture.completedFuture(null),
                            (l, r) -> {
                                l.join();
                                return r;
                            });
            }

            private CompletableFuture<Void> applyOp(Direction dir, ProtectionEndpointIntent intent) {
                log.trace("applying {}: {}", dir, intent);
                if (dir == Direction.REMOVE) {
                    networkConfigService.removeConfig(intent.deviceId(), ProtectionConfig.class);
                } else if (dir == Direction.ADD) {
                    ProtectedTransportEndpointDescription description = intent.description();

                    // Can't do following. Will trigger empty CONFIG_ADDED
                    //ProtectionConfig cfg = networkConfigService.addConfig(intent.deviceId(),
                    //                                                      ProtectionConfig.class);
                    ProtectionConfig cfg = new ProtectionConfig(intent.deviceId());
                    cfg.fingerprint(description.fingerprint());
                    cfg.peer(description.peer());
                    cfg.paths(description.paths());
                    //cfg.apply();

                    networkConfigService.applyConfig(intent.deviceId(),
                                                     ProtectionConfig.class,
                                                     cfg.node());
                }
                // TODO Should monitor progress and complete only after it's
                // actually done.
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public String toString() {
                return ops.toString();
            }
        }

        /**
         * List of Stages which must be executed in order.
         */
        private final List<Stage> stages = new ArrayList<>();

        private final List<Stage> failed = new CopyOnWriteArrayList<>();

        @Override
        synchronized void apply() {
            for (Stage stage : stages) {
                log.trace("applying Stage {}", stage);
                CompletableFuture<Void> result = stage.apply();
                // wait for stage completion
                result.join();
                if (result.isCompletedExceptionally()) {
                    log.error("Stage {} failed", stage);
                    failed.add(stage);
                    errorConsumer.accept(ProtectionConfigOperationContext.this);
                    return;
                }
            }
            successConsumer.accept(ProtectionConfigOperationContext.this);
        }

        @Override
        Object error() {
            // Something to represent error state
            return failed;
        }

        @Override
        synchronized void prepareIntents(List<Intent> intentsToApply,
                                         Direction direction) {

            stages.add(new Stage(intentsToApply.stream()
                                 .filter(i -> i instanceof ProtectionEndpointIntent)
                                 .map(i -> Pair.of((ProtectionEndpointIntent) i, direction))
                                 .collect(Collectors.toList())));
        }

        @Override
        protected ToStringHelper toStringHelper() {
            return super.toStringHelper()
                    .add("stages", stages)
                    .add("failed", failed);
        }
    }
}
