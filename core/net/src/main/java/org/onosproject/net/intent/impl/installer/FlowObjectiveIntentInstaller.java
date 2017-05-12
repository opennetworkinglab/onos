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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstallCoordinator;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.impl.IntentManager;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.onosproject.net.flowobjective.ObjectiveError.INSTALLATIONTHRESHOLDEXCEEDED;
import static org.onosproject.net.intent.IntentInstaller.Direction.ADD;
import static org.onosproject.net.intent.IntentInstaller.Direction.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for FlowObjectiveIntent.
 */
@Component(immediate = true)
public class FlowObjectiveIntentInstaller implements IntentInstaller<FlowObjectiveIntent> {
    private static final int OBJECTIVE_RETRY_THRESHOLD = 5;
    private static final String UNSUPPORT_OBJ = "unsupported objective {}";
    private final Logger log = getLogger(IntentManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentInstallCoordinator intentInstallCoordinator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Activate
    public void activate() {
        intentExtensionService.registerInstaller(FlowObjectiveIntent.class, this);
    }

    @Deactivate
    public void deactivated() {
        intentExtensionService.unregisterInstaller(FlowObjectiveIntent.class);
    }

    @Override
    public void apply(IntentOperationContext<FlowObjectiveIntent> intentOperationContext) {
        Objects.requireNonNull(intentOperationContext);
        Optional<IntentData> toUninstall = intentOperationContext.toUninstall();
        Optional<IntentData> toInstall = intentOperationContext.toInstall();

        List<FlowObjectiveIntent> uninstallIntents = intentOperationContext.intentsToUninstall();
        List<FlowObjectiveIntent> installIntents = intentOperationContext.intentsToInstall();

        if (!toInstall.isPresent() && !toUninstall.isPresent()) {
            intentInstallCoordinator.intentInstallSuccess(intentOperationContext);
            return;
        }

        if (toUninstall.isPresent()) {
            IntentData intentData = toUninstall.get();
            trackerService.removeTrackedResources(intentData.key(), intentData.intent().resources());
            uninstallIntents.forEach(installable ->
                                             trackerService.removeTrackedResources(intentData.intent().key(),
                                                                                   installable.resources()));
        }

        if (toInstall.isPresent()) {
            IntentData intentData = toInstall.get();
            trackerService.addTrackedResources(intentData.key(), intentData.intent().resources());
            installIntents.forEach(installable ->
                                           trackerService.addTrackedResources(intentData.key(),
                                                                              installable.resources()));
        }

        FlowObjectiveIntentInstallationContext intentInstallationContext =
                new FlowObjectiveIntentInstallationContext(intentOperationContext);

        uninstallIntents.stream()
                .map(intent -> buildObjectiveContexts(intent, REMOVE))
                .flatMap(Collection::stream)
                .forEach(context -> {
                    context.intentInstallationContext(intentInstallationContext);
                    intentInstallationContext.addContext(context);
                    intentInstallationContext.addPendingContext(context);
                });

        installIntents.stream()
                .map(intent -> buildObjectiveContexts(intent, ADD))
                .flatMap(Collection::stream)
                .forEach(context -> {
                    context.intentInstallationContext(intentInstallationContext);
                    intentInstallationContext.addContext(context);
                    intentInstallationContext.addNextPendingContext(context);
                });

        intentInstallationContext.apply();
    }

    /**
     * Builds all objective contexts for a given flow objective Intent with given
     * operation.
     *
     * @param intent the flow objective Intent
     * @param direction the operation of this Intent
     * @return all objective context of the Intent with given operation
     */
    private Set<FlowObjectiveInstallationContext> buildObjectiveContexts(FlowObjectiveIntent intent,
                                                                         Direction direction) {
        Objects.requireNonNull(intent);
        Objects.requireNonNull(direction);
        Set<FlowObjectiveInstallationContext> contexts = Sets.newHashSet();
        int size = intent.objectives().size();
        List<Objective> objectives = intent.objectives();
        List<DeviceId> deviceIds = intent.devices();

        if (direction == ADD) {
            // Install objectives
            // The flow objective system will handle the installation order
            for (int i = 0; i < size; i++) {
                Objective objective = objectives.get(i);
                DeviceId deviceId = deviceIds.get(i);
                FlowObjectiveInstallationContext ctx = buildObjectiveContext(objective, deviceId, direction);
                contexts.add(ctx);
            }
            return contexts;
        } else {
            // Uninstall objecitves
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
        Objects.requireNonNull(objective);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(direction);
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

    /**
     * Installation context for flow objective.
     * Manages installation state of a flow objective.
     */
    class FlowObjectiveInstallationContext implements ObjectiveContext {
        private Objective objective;
        private DeviceId deviceId;
        private ObjectiveError error;
        private AtomicInteger retry;
        private FlowObjectiveInstallationContext nextContext;
        private FlowObjectiveIntentInstallationContext intentInstallationContext;

        /**
         * Set flow objective Intent installation context to this context.
         *
         * @param intentInstallationContext the Intent installation context
         */
        public void intentInstallationContext(FlowObjectiveIntentInstallationContext intentInstallationContext) {
            Objects.requireNonNull(intentInstallationContext);
            this.intentInstallationContext = intentInstallationContext;

            // Set Intent installation context to the next context if exists.
            if (nextContext != null) {
                nextContext.intentInstallationContext(intentInstallationContext);
            }
        }

        /**
         * Sets next flow objective installation context.
         *
         * @param nextContext the next flow objective installation context
         */
        public void nextContext(FlowObjectiveInstallationContext nextContext) {
            Objects.requireNonNull(nextContext);
            this.nextContext = nextContext;
        }

        /**
         * Sets objective and device id to this context; reset error states.
         *
         * @param objective the objective
         * @param deviceId the device id
         */
        void setObjective(Objective objective, DeviceId deviceId) {
            Objects.requireNonNull(objective);
            Objects.requireNonNull(deviceId);
            this.objective = objective;
            this.deviceId = deviceId;
            this.error = null;
            this.retry = new AtomicInteger(0);
        }

        /**
         * Gets the number of retries.
         *
         * @return the retry count
         */
        int retryTimes() {
            return this.retry.get();
        }

        /**
         * Increases the number of retries.
         */
        void increaseRetryValue() {
            this.retry.incrementAndGet();
        }

        /**
         * Completed this context.
         *
         * @param error the error of this context if exist; null otherwise
         */
        private void finished(ObjectiveError error) {
            synchronized (intentInstallationContext) {
                if (error != null) {
                    this.error = error;
                    intentInstallationContext.handleObjectiveError(this, error);
                } else {
                    // apply next context if exist
                    if (nextContext != null) {
                        intentInstallationContext.addPendingContext(nextContext);
                        flowObjectiveService.apply(nextContext.deviceId,
                                                   nextContext.objective);
                        intentInstallationContext.removePendingContext(this);
                    } else {
                        intentInstallationContext.removePendingContext(this);
                    }
                }
                if (!intentInstallationContext.pendingContexts().isEmpty()) {
                    return;
                }
                // Apply second stage pending contexts if it is not empty
                if (!intentInstallationContext.nextPendingContexts().isEmpty()) {
                    intentInstallationContext.moveNextPendingToPending();
                    final Set<ObjectiveContext> contextsToApply =
                            Sets.newHashSet(intentInstallationContext.pendingContexts());
                    contextsToApply.forEach(ctx -> {
                        FlowObjectiveInstallationContext foiCtx = (FlowObjectiveInstallationContext) ctx;
                        flowObjectiveService.apply(foiCtx.deviceId,
                                                   foiCtx.objective);
                    });
                    return;
                }
                if (intentInstallationContext.errorContexts().isEmpty()) {
                    intentInstallCoordinator.intentInstallSuccess(intentInstallationContext.intentOperationContext());
                } else {
                    intentInstallCoordinator.intentInstallFailed(intentInstallationContext.intentOperationContext());
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
    }

    /**
     * Installation context for FlowObjective Intent.
     * Manages states of pending and error flow objective contexts.
     */
    class FlowObjectiveIntentInstallationContext {
        private final IntentOperationContext<FlowObjectiveIntent> intentOperationContext;
        final List<ObjectiveContext> contexts = Lists.newArrayList();
        final Set<ObjectiveContext> errorContexts = Sets.newConcurrentHashSet();
        final Set<ObjectiveContext> pendingContexts = Sets.newConcurrentHashSet();

        // Second stage of pending contexts
        final Set<ObjectiveContext> nextPendingContexts = Sets.newConcurrentHashSet();

        /**
         * Creates a flow objective installation context.
         *
         * @param intentOperationContext the flow objective installation context
         */
        public FlowObjectiveIntentInstallationContext(
                IntentOperationContext<FlowObjectiveIntent> intentOperationContext) {
            Objects.requireNonNull(intentOperationContext);
            this.intentOperationContext = intentOperationContext;
        }

        /**
         * Gets Intent operation context of this context.
         *
         * @return the Intent operation context
         */
        public IntentOperationContext<FlowObjectiveIntent> intentOperationContext() {
            return intentOperationContext;
        }

        /**
         * Applies all contexts to flow objective service.
         */
        public void apply() {
            if (pendingContexts.isEmpty()) {
                moveNextPendingToPending();
            }
            final Set<ObjectiveContext> contextsToApply = pendingContexts();
            contextsToApply.forEach(ctx -> {
                FlowObjectiveInstallationContext foiCtx =
                        (FlowObjectiveInstallationContext) ctx;
                flowObjectiveService.apply(foiCtx.deviceId, foiCtx.objective);
            });
        }

        /**
         * Gets all error contexts.
         *
         * @return the error contexts
         */
        public Set<ObjectiveContext> errorContexts() {
            return ImmutableSet.copyOf(errorContexts);
        }

        /**
         * Gets all pending contexts.
         *
         * @return the pending contexts
         */
        public Set<ObjectiveContext> pendingContexts() {
            return ImmutableSet.copyOf(pendingContexts);
        }

        /**
         * Gets all pending contexts of next stage.
         *
         * @return the pending contexts for next stage
         */
        public Set<ObjectiveContext> nextPendingContexts() {
            return ImmutableSet.copyOf(nextPendingContexts);
        }

        /**
         * Adds a context.
         *
         * @param context the context
         */
        public void addContext(ObjectiveContext context) {
            Objects.requireNonNull(context);
            contexts.add(context);
        }

        /**
         * Adds a context to pending context of next stage.
         *
         * @param context the context
         */
        public void addNextPendingContext(ObjectiveContext context) {
            Objects.requireNonNull(context);
            nextPendingContexts.add(context);
        }

        /**
         * Adds a context to pending context.
         *
         * @param context the context
         */
        public void addPendingContext(ObjectiveContext context) {
            Objects.requireNonNull(context);
            pendingContexts.add(context);
        }

        /**
         * Removes the pending context.
         *
         * @param context the context
         */
        public void removePendingContext(ObjectiveContext context) {
            Objects.requireNonNull(context);
            pendingContexts.remove(context);
        }

        /**
         * Moves pending context from next stage to current stage.
         */
        public void moveNextPendingToPending() {
            pendingContexts.addAll(nextPendingContexts);
            nextPendingContexts.clear();
        }

        /**
         * Handles error of objective context.
         *
         * @param ctx the objective context
         * @param error the error
         */
        public void handleObjectiveError(FlowObjectiveInstallationContext ctx,
                                         ObjectiveError error) {
            Objects.requireNonNull(ctx);
            Objects.requireNonNull(error);
            log.debug("Got error(s) when install objective: {}, error: {}, retry: {}",
                      ctx.objective, ctx.error, ctx.retry);
            if (ctx.retryTimes() > OBJECTIVE_RETRY_THRESHOLD) {
                ctx.error = INSTALLATIONTHRESHOLDEXCEEDED;
                pendingContexts.remove(ctx);
                errorContexts.add(ctx);
                return;
            }
            // reset error
            ctx.error = null;
            // strategies for errors
            switch (error) {
                case GROUPEXISTS:
                    if (ctx.objective.op() == Objective.Operation.ADD &&
                            ctx.objective instanceof NextObjective) {
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
}
