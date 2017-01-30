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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Auxiliary entity responsible for installing the intents into the environment.
 */
class IntentInstaller {

    private static final Logger log = getLogger(IntentInstaller.class);

    private IntentStore store;
    private ObjectiveTrackerService trackerService;
    private FlowRuleService flowRuleService;
    private FlowObjectiveService flowObjectiveService;
    private NetworkConfigService networkConfigService;

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
     */
    void init(IntentStore intentStore, ObjectiveTrackerService trackerService,
              FlowRuleService flowRuleService, FlowObjectiveService flowObjectiveService,
              NetworkConfigService networkConfigService) {
        this.store = intentStore;
        this.trackerService = trackerService;
        //TODO Various services should be plugged to the intent installer instead of being hardcoded
        this.flowRuleService = flowRuleService;
        this.flowObjectiveService = flowObjectiveService;
        this.networkConfigService = networkConfigService;
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

    // --- Utilities to support various installable Intent ----

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
                   intent instanceof ProtectionEndpointIntent;
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
        List<FlowObjectiveInstallationContext> contexts = Lists.newLinkedList();
        final Set<ObjectiveContext> pendingContexts = Sets.newHashSet();
        final Set<ObjectiveContext> errorContexts = Sets.newConcurrentHashSet();

        FlowObjectiveOperationContext(IntentInstallationContext context) {
            super(context);
        }

        @Override
        public void prepareIntents(List<Intent> intentsToApply, Direction direction) {
            intentsToApply.stream()
                    .filter(x -> x instanceof FlowObjectiveIntent)
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
            pendingContexts.addAll(contexts);
            contexts.forEach(objectiveContext ->
                flowObjectiveService.apply(objectiveContext.deviceId,
                                           objectiveContext.objective)
            );
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
