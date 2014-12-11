/*
 * Copyright 2014 Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentBatchService;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentOperation;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onosproject.net.intent.IntentState.*;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An implementation of Intent Manager.
 */
@Component(immediate = true)
@Service
public class IntentManager
        implements IntentService, IntentExtensionService {
    private static final Logger log = getLogger(IntentManager.class);

    public static final String INTENT_NULL = "Intent cannot be null";
    public static final String INTENT_ID_NULL = "Intent ID cannot be null";

    private static final int NUM_THREADS = 12;

    private static final EnumSet<IntentState> RECOMPILE
            = EnumSet.of(INSTALL_REQ, FAILED, WITHDRAW_REQ);
    private static final EnumSet<IntentState> NON_PARKED_OR_FAILED
            = EnumSet.complementOf(EnumSet.of(INSTALL_REQ, INSTALLED, WITHDRAW_REQ, WITHDRAWN));


    // Collections for compiler, installer, and listener are ONOS instance local
    private final ConcurrentMap<Class<? extends Intent>,
            IntentCompiler<? extends Intent>> compilers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends Intent>,
            IntentInstaller<? extends Intent>> installers = new ConcurrentHashMap<>();

    private final AbstractListenerRegistry<IntentEvent, IntentListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentBatchService batchService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;


    private ExecutorService executor;

    private final IntentStoreDelegate delegate = new InternalStoreDelegate();
    private final TopologyChangeDelegate topoDelegate = new InternalTopoChangeDelegate();
    private final IntentBatchDelegate batchDelegate = new InternalBatchDelegate();
    private IdGenerator idGenerator;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        trackerService.setDelegate(topoDelegate);
        batchService.setDelegate(batchDelegate);
        eventDispatcher.addSink(IntentEvent.class, listenerRegistry);
        executor = newFixedThreadPool(NUM_THREADS, namedThreads("onos-intent-%d"));
        idGenerator = coreService.getIdGenerator("intent-ids");
        Intent.bindIdGenerator(idGenerator);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        trackerService.unsetDelegate(topoDelegate);
        batchService.unsetDelegate(batchDelegate);
        eventDispatcher.removeSink(IntentEvent.class);
        executor.shutdown();
        Intent.unbindIdGenerator(idGenerator);
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        execute(IntentOperations.builder(intent.appId())
                        .addSubmitOperation(intent).build());
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        execute(IntentOperations.builder(intent.appId())
                        .addWithdrawOperation(intent.id()).build());
    }

    @Override
    public void replace(IntentId oldIntentId, Intent newIntent) {
        checkNotNull(oldIntentId, INTENT_ID_NULL);
        checkNotNull(newIntent, INTENT_NULL);
        execute(IntentOperations.builder(newIntent.appId())
                .addReplaceOperation(oldIntentId, newIntent)
                .build());
    }

    @Override
    public void execute(IntentOperations operations) {
        if (operations.operations().isEmpty()) {
            return;
        }
        batchService.addIntentOperations(operations);
    }

    @Override
    public Iterable<Intent> getIntents() {
        return store.getIntents();
    }

    @Override
    public long getIntentCount() {
        return store.getIntentCount();
    }

    @Override
    public Intent getIntent(IntentId id) {
        checkNotNull(id, INTENT_ID_NULL);
        return store.getIntent(id);
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        checkNotNull(id, INTENT_ID_NULL);
        return store.getIntentState(id);
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        checkNotNull(intentId, INTENT_ID_NULL);
        return store.getInstallableIntents(intentId);
    }

    @Override
    public void addListener(IntentListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(IntentListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    public <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler) {
        compilers.put(cls, compiler);
    }

    @Override
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        compilers.remove(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers() {
        return ImmutableMap.copyOf(compilers);
    }

    @Override
    public <T extends Intent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        installers.put(cls, installer);
    }

    @Override
    public <T extends Intent> void unregisterInstaller(Class<T> cls) {
        installers.remove(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentInstaller<? extends Intent>> getInstallers() {
        return ImmutableMap.copyOf(installers);
    }

    /**
     * Returns the corresponding intent compiler to the specified intent.
     *
     * @param intent intent
     * @param <T>    the type of intent
     * @return intent compiler corresponding to the specified intent
     */
    private <T extends Intent> IntentCompiler<T> getCompiler(T intent) {
        @SuppressWarnings("unchecked")
        IntentCompiler<T> compiler = (IntentCompiler<T>) compilers.get(intent.getClass());
        if (compiler == null) {
            throw new IntentException("no compiler for class " + intent.getClass());
        }
        return compiler;
    }

    /**
     * Returns the corresponding intent installer to the specified installable intent.
     *
     * @param intent intent
     * @param <T>    the type of installable intent
     * @return intent installer corresponding to the specified installable intent
     */
    private <T extends Intent> IntentInstaller<T> getInstaller(T intent) {
        @SuppressWarnings("unchecked")
        IntentInstaller<T> installer = (IntentInstaller<T>) installers.get(intent.getClass());
        if (installer == null) {
            throw new IntentException("no installer for class " + intent.getClass());
        }
        return installer;
    }

    /**
     * Compiles the specified intent.
     *
     * @param update intent update
     */
    private void executeCompilingPhase(IntentUpdate update) {
        Intent intent = update.newIntent();
        // Indicate that the intent is entering the compiling phase.
        update.setInflightState(intent, COMPILING);

        try {
            // Compile the intent into installable derivatives.
            List<Intent> installables = compileIntent(intent, update);

            // If all went well, associate the resulting list of installable
            // intents with the top-level intent and proceed to install.
            update.setInstallables(installables);
        } catch (PathNotFoundException e) {
            log.debug("Path not found for intent {}", intent);
            update.setInflightState(intent, FAILED);
        } catch (IntentException e) {
            log.warn("Unable to compile intent {} due to:", intent.id(), e);

            // If compilation failed, mark the intent as failed.
            update.setInflightState(intent, FAILED);
        }
    }

    /**
     * Compiles an intent recursively.
     *
     * @param intent intent
     * @return result of compilation
     */
    private List<Intent> compileIntent(Intent intent, IntentUpdate update) {
        if (intent.isInstallable()) {
            return ImmutableList.of(intent);
        }

        registerSubclassCompilerIfNeeded(intent);
        List<Intent> previous = update.oldInstallables();
        // FIXME: get previous resources
        List<Intent> installable = new ArrayList<>();
        for (Intent compiled : getCompiler(intent).compile(intent, previous, null)) {
            installable.addAll(compileIntent(compiled, update));
        }
        return installable;
    }

    /**
     * Installs all installable intents associated with the specified top-level
     * intent.
     *
     * @param update intent update
     */
    private void executeInstallingPhase(IntentUpdate update) {
        if (update.newInstallables() == null) {
            //no failed intents allowed past this point...
            return;
        }
        // Indicate that the intent is entering the installing phase.
        update.setInflightState(update.newIntent(), INSTALLING);

        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (Intent installable : update.newInstallables()) {
            registerSubclassInstallerIfNeeded(installable);
            trackerService.addTrackedResources(update.newIntent().id(),
                                               installable.resources());
            try {
                batches.addAll(getInstaller(installable).install(installable));
            } catch (Exception e) { // TODO this should be IntentException
                log.warn("Unable to install intent {} due to:", update.newIntent().id(), e);
                trackerService.removeTrackedResources(update.newIntent().id(),
                                                      installable.resources());
                //TODO we failed; intent should be recompiled
                update.setInflightState(update.newIntent(), FAILED);
            }
        }
        update.addBatches(batches);
    }

    /**
     * Uninstalls the specified intent by uninstalling all of its associated
     * installable derivatives.
     *
     * @param update intent update
     */
    private void executeWithdrawingPhase(IntentUpdate update) {
        if (!update.oldIntent().equals(update.newIntent())) {
            update.setInflightState(update.oldIntent(), WITHDRAWING);
        } // else newIntent is FAILED
        update.addBatches(uninstallIntent(update.oldIntent(), update.oldInstallables()));
    }

    /**
     * Uninstalls all installable intents associated with the given intent.
     *
     * @param intent intent
     * @param installables installable intents
     * @return list of batches to uninstall intent
     */
    private List<FlowRuleBatchOperation> uninstallIntent(Intent intent, List<Intent> installables) {
        if (installables == null) {
            return Collections.emptyList();
        }
        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (Intent installable : installables) {
            trackerService.removeTrackedResources(intent.id(),
                                                  installable.resources());
            try {
                batches.addAll(getInstaller(installable).uninstall(installable));
            } catch (IntentException e) {
                log.warn("Unable to uninstall intent {} due to:", intent.id(), e);
                // TODO: this should never happen. but what if it does?
            }
        }
        return batches;
    }

    /**
     * Withdraws the old intent and installs the new intent as one operation.
     *
     * @param update intent update
     */
    private void executeReplacementPhase(IntentUpdate update) {
        checkArgument(update.oldInstallables().size() == update.newInstallables().size(),
                      "Old and New Intent must have equivalent installable intents.");
        if (!update.oldIntent().equals(update.newIntent())) {
            // only set the old intent's state if it is different
            update.setInflightState(update.oldIntent(), WITHDRAWING);
        }
        update.setInflightState(update.newIntent(), INSTALLING);

        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (int i = 0; i < update.oldInstallables().size(); i++) {
            Intent oldInstallable = update.oldInstallables().get(i);
            Intent newInstallable = update.newInstallables().get(i);
            //FIXME revisit this
//            if (oldInstallable.equals(newInstallable)) {
//                continue;
//            }
            checkArgument(oldInstallable.getClass().equals(newInstallable.getClass()),
                          "Installable Intent type mismatch.");
            trackerService.removeTrackedResources(update.oldIntent().id(), oldInstallable.resources());
            trackerService.addTrackedResources(update.newIntent().id(), newInstallable.resources());
            try {
                batches.addAll(getInstaller(newInstallable).replace(oldInstallable, newInstallable));
            } catch (IntentException e) {
                log.warn("Unable to update intent {} due to:", update.oldIntent().id(), e);
                //FIXME... we failed. need to uninstall (if same) or revert (if different)
                trackerService.removeTrackedResources(update.newIntent().id(), newInstallable.resources());
                update.setInflightState(update.newIntent(), FAILED);
                batches = uninstallIntent(update.oldIntent(), update.oldInstallables());
            }
        }
        update.addBatches(batches);
    }

    /**
     * Registers an intent compiler of the specified intent if an intent compiler
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent compiler for a parent type is found, this method
     * registers the found intent compiler.
     *
     * @param intent intent
     */
    private void registerSubclassCompilerIfNeeded(Intent intent) {
        if (!compilers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the Intent class descendants
                if (Intent.class.isAssignableFrom(cls)) {
                    IntentCompiler<?> compiler = compilers.get(cls);
                    if (compiler != null) {
                        compilers.put(intent.getClass(), compiler);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    /**
     * Registers an intent installer of the specified intent if an intent installer
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent installer for a parent type is found, this method
     * registers the found intent installer.
     *
     * @param intent intent
     */
    private void registerSubclassInstallerIfNeeded(Intent intent) {
        if (!installers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the Intent class descendants
                if (Intent.class.isAssignableFrom(cls)) {
                    IntentInstaller<?> installer = installers.get(cls);
                    if (installer != null) {
                        installers.put(intent.getClass(), installer);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements IntentStoreDelegate {
        @Override
        public void notify(IntentEvent event) {
            eventDispatcher.post(event);
        }
    }

    private void buildAndSubmitBatches(Iterable<IntentId> intentIds,
                                       boolean compileAllFailed) {
        Map<ApplicationId, IntentOperations.Builder> batches = Maps.newHashMap();
        // Attempt recompilation of the specified intents first.
        for (IntentId id : intentIds) {
            Intent intent = store.getIntent(id);
            if (intent == null) {
                continue;
            }
            IntentOperations.Builder builder = batches.get(intent.appId());
            if (builder == null) {
                builder = IntentOperations.builder(intent.appId());
                batches.put(intent.appId(), builder);
            }
            builder.addUpdateOperation(id);
        }

        if (compileAllFailed) {
            // If required, compile all currently failed intents.
            for (Intent intent : getIntents()) {
                IntentState state = getIntentState(intent.id());
                if (RECOMPILE.contains(state)) {
                    IntentOperations.Builder builder = batches.get(intent.appId());
                    if (builder == null) {
                        builder = IntentOperations.builder(intent.appId());
                        batches.put(intent.appId(), builder);
                    }
                    if (state == WITHDRAW_REQ) {
                        builder.addWithdrawOperation(intent.id());
                    } else {
                        builder.addUpdateOperation(intent.id());
                    }
                }
            }
        }

        for (ApplicationId appId : batches.keySet()) {
            if (batchService.isLocalLeader(appId)) {
                execute(batches.get(appId).build());
            }
        }
    }

    // Topology change delegate
    private class InternalTopoChangeDelegate implements TopologyChangeDelegate {
        @Override
        public void triggerCompile(Iterable<IntentId> intentIds,
                                   boolean compileAllFailed) {
            buildAndSubmitBatches(intentIds, compileAllFailed);
        }
    }

    // TODO move this inside IntentUpdate?
    /**
     * TODO. rename this...
     * @param update intent update
     */
    private void processIntentUpdate(IntentUpdate update) {

        // check to see if the intent needs to be compiled or recompiled
        if (update.newIntent() != null) {
            executeCompilingPhase(update);
        }

        if (update.oldInstallables() != null && update.newInstallables() != null) {
            executeReplacementPhase(update);
        } else if (update.newInstallables() != null) {
            executeInstallingPhase(update);
        } else if (update.oldInstallables() != null) {
            executeWithdrawingPhase(update);
        } else {
            if (update.oldIntent() != null &&
                !update.oldIntent().equals(update.newIntent())) {
                // removing failed intent
                update.setInflightState(update.oldIntent(), WITHDRAWING);
            }
//            if (update.newIntent() != null) {
//                // TODO assert that next state is failed
//            }
        }
    }

    // TODO comments...
    private class IntentUpdate {
        private final Intent oldIntent;
        private final Intent newIntent;
        private final Map<Intent, IntentState> stateMap = Maps.newHashMap();

        private final List<Intent> oldInstallables;
        private List<Intent> newInstallables;
        private final List<FlowRuleBatchOperation> batches = Lists.newLinkedList();
        private int currentBatch = 0; // TODO: maybe replace with an iterator

        IntentUpdate(IntentOperation op) {
            switch (op.type()) {
                case SUBMIT:
                    newIntent = op.intent();
                    oldIntent = null;
                    break;
                case WITHDRAW:
                    newIntent = null;
                    oldIntent = store.getIntent(op.intentId());
                    break;
                case REPLACE:
                    newIntent = op.intent();
                    oldIntent = store.getIntent(op.intentId());
                    break;
                case UPDATE:
                    oldIntent = store.getIntent(op.intentId());
                    newIntent = oldIntent;
                    break;
                default:
                    oldIntent = null;
                    newIntent = null;
                    break;
            }
            // fetch the old intent's installables from the store
            if (oldIntent != null) {
                oldInstallables = store.getInstallableIntents(oldIntent.id());
            } else {
                oldInstallables = null;
                if (newIntent == null) {
                    log.info("Ignoring {} for missing Intent {}", op.type(), op.intentId());
                }
            }
        }

        void init(BatchWrite batchWrite) {
            if (newIntent != null) {
                // TODO consider only "creating" intent if it does not exist
                // Note: We need to set state to INSTALL_REQ regardless.
                batchWrite.createIntent(newIntent);
            } else if (oldIntent != null && !oldIntent.equals(newIntent)) {
                batchWrite.setState(oldIntent, WITHDRAW_REQ);
            }
        }

        Intent oldIntent() {
            return oldIntent;
        }

        Intent newIntent() {
            return newIntent;
        }

        List<Intent> oldInstallables() {
            return oldInstallables;
        }

        List<Intent> newInstallables() {
            return newInstallables;
        }

        void setInstallables(List<Intent> installables) {
            newInstallables = installables;
        }

        boolean isComplete() {
            return currentBatch >= batches.size();
        }

        FlowRuleBatchOperation currentBatch() {
            return !isComplete() ? batches.get(currentBatch) : null;
        }

        void batchSuccess() {
            // move on to next Batch
            currentBatch++;
        }

        void batchFailed() {
            // the current batch has failed, so recompile
            // remove the current batch and all remaining
            for (int i = currentBatch; i < batches.size(); i++) {
                batches.remove(i);
            }
            if (oldIntent != null) {
                executeWithdrawingPhase(this); // remove the old intent
            }
            if (newIntent != null) {
                setInflightState(newIntent, FAILED);
                batches.addAll(uninstallIntent(newIntent, newInstallables()));
            }

            // TODO we might want to try to recompile the new intent
        }

        private void finalizeStates(BatchWrite batchWrite) {
            // events to be triggered on successful write
            for (Intent intent : stateMap.keySet()) {
                switch (getInflightState(intent)) {
                    case INSTALLING:
                        batchWrite.setState(intent, INSTALLED);
                        batchWrite.setInstallableIntents(newIntent.id(), newInstallables);
                        break;
                    case WITHDRAWING:
                        batchWrite.setState(intent, WITHDRAWN);
                        batchWrite.removeInstalledIntents(intent.id());
                        batchWrite.removeIntent(intent.id());
                        break;
                    case FAILED:
                        batchWrite.setState(intent, FAILED);
                        batchWrite.removeInstalledIntents(intent.id());
                        break;

                    // FALLTHROUGH to default from here
                    case INSTALL_REQ:
                    case COMPILING:
                    case RECOMPILING:
                    case WITHDRAW_REQ:
                    case WITHDRAWN:
                    case INSTALLED:
                    default:
                        //FIXME clean this up (we shouldn't ever get here)
                        log.warn("Bad state: {} for {}", getInflightState(intent), intent);
                        break;
                }
            }
        }

        void addBatches(List<FlowRuleBatchOperation> batches) {
            this.batches.addAll(batches);
        }

        IntentState getInflightState(Intent intent) {
            return stateMap.get(intent);
        }

        // set transient state during intent update process
        void setInflightState(Intent intent, IntentState newState) {
            // This method should be called for
            // transition to non-parking or Failed only
            if (!NON_PARKED_OR_FAILED.contains(newState)) {
                log.error("Unexpected transition to {}", newState);
            }

            IntentState oldState = stateMap.get(intent);
            log.debug("intent id: {}, old state: {}, new state: {}",
                    intent.id(), oldState, newState);

            stateMap.put(intent, newState);
        }
    }

    private class IntentInstallMonitor implements Runnable {

        // TODO make this configurable through a configuration file using @Property mechanism
        // These fields needs to be moved to the enclosing class and configurable through a configuration file
        private static final int TIMEOUT_PER_OP = 500; // ms
        private static final int MAX_ATTEMPTS = 3;

        private final IntentOperations ops;
        private final List<IntentUpdate> intentUpdates = Lists.newArrayList();

        private final Duration timeoutPerOperation;
        private final int maxAttempts;

        // future holding current FlowRuleBatch installation result
        private Future<CompletedBatchOperation> future;
        private long startTime = System.currentTimeMillis();
        private long endTime;
        private int installAttempt;

        public IntentInstallMonitor(IntentOperations ops) {
            this(ops, Duration.ofMillis(TIMEOUT_PER_OP), MAX_ATTEMPTS);
        }

        public IntentInstallMonitor(IntentOperations ops, Duration timeoutPerOperation, int maxAttempts) {
            this.ops = checkNotNull(ops);
            this.timeoutPerOperation = checkNotNull(timeoutPerOperation);
            checkArgument(maxAttempts > 0, "maxAttempts must be larger than 0, but %s", maxAttempts);
            this.maxAttempts = maxAttempts;

            resetTimeoutLimit();
        }

        private void resetTimeoutLimit() {
            // FIXME compute reasonable timeouts
            this.endTime = System.currentTimeMillis()
                           + ops.operations().size() * timeoutPerOperation.toMillis();
        }

        private void buildIntentUpdates() {
            BatchWrite batchWrite = BatchWrite.newInstance();

            // create context and record new request to store
            for (IntentOperation op : ops.operations()) {
                IntentUpdate update = new IntentUpdate(op);
                update.init(batchWrite);
                intentUpdates.add(update);
            }

            if (!batchWrite.isEmpty()) {
                store.batchWrite(batchWrite);
            }

            // start processing each Intents
            for (IntentUpdate update : intentUpdates) {
                processIntentUpdate(update);
            }
            future = applyNextBatch();
        }

        /**
         * Builds and applies the next batch, and returns the future.
         *
         * @return Future for next batch
         */
        private Future<CompletedBatchOperation> applyNextBatch() {
            //TODO test this. (also, maybe save this batch)
            FlowRuleBatchOperation batch = new FlowRuleBatchOperation(Collections.emptyList());
            for (IntentUpdate update : intentUpdates) {
                if (!update.isComplete()) {
                    batch.addAll(update.currentBatch());
                }
            }
            if (batch.size() > 0) {
                //FIXME apply batch might throw an exception
                return flowRuleService.applyBatch(batch);
            } else {
                // there are no flow rule batches; finalize the intent update
                BatchWrite batchWrite = BatchWrite.newInstance();
                for (IntentUpdate update : intentUpdates) {
                    update.finalizeStates(batchWrite);
                }
                if (!batchWrite.isEmpty()) {
                    store.batchWrite(batchWrite);
                }
                return null;
            }
        }

        private void updateBatches(CompletedBatchOperation completed) {
            if (completed.isSuccess()) {
                for (IntentUpdate update : intentUpdates) {
                    update.batchSuccess();
                }
            } else {
                // entire batch has been reverted...
                log.debug("Failed items: {}", completed.failedItems());
                log.debug("Failed ids: {}",  completed.failedIds());

                for (Long id : completed.failedIds()) {
                    IntentId targetId = IntentId.valueOf(id);
                    for (IntentUpdate update : intentUpdates) {
                        List<Intent> installables = Lists.newArrayList(update.newInstallables());
                        if (update.oldInstallables() != null) {
                            installables.addAll(update.oldInstallables());
                        }
                        for (Intent intent : installables) {
                            if (intent.id().equals(targetId)) {
                                update.batchFailed();
                                break;
                            }
                        }
                    }
                    // don't increment the non-failed items, as they have been reverted.
                }
            }
        }

        private void abandonShip() {
            // the batch has failed
            // TODO: maybe we should do more?
            log.error("Walk the plank, matey...");
            future = null;
            batchService.removeIntentOperations(ops);
        }

        /**
         * Iterate through the pending futures, and remove them when they have completed.
         */
        private void processFutures() {
            if (future == null) {
                // we are done if the future is null
                return;
            }
            try {
                CompletedBatchOperation completed = future.get(100, TimeUnit.NANOSECONDS);
                updateBatches(completed);
                future = applyNextBatch();
            } catch (TimeoutException | InterruptedException te) {
                log.trace("Installation of intents are still pending: {}", ops);
            } catch (ExecutionException e) {
                log.warn("Execution of batch failed: {}", ops, e);
                abandonShip();
            }
        }

        private void retry() {
            log.debug("Execution timed out, retrying.");
            if (future.cancel(true)) { // cancel success; batch is reverted
                // reset the timer
                resetTimeoutLimit();
                installAttempt++;
                if (installAttempt == maxAttempts) {
                    log.warn("Install request timed out: {}", ops);
                    for (IntentUpdate update : intentUpdates) {
                        update.batchFailed();
                    }
                } else if (installAttempt > maxAttempts) {
                    abandonShip();
                    return;
                } // else just resubmit the work
                future = applyNextBatch();
                executor.submit(this);
            } else {
                log.error("Cancelling FlowRuleBatch failed.");
                // FIXME
                // cancel failed... batch is broken; shouldn't happen!
                // we could manually reverse everything
                // ... or just core dump and send email to Ali
                abandonShip();
            }
        }

        boolean isComplete() {
            return future == null;
        }

        @Override
        public void run() {
            try {
                if (intentUpdates.isEmpty()) {
                    // this should only be called on the first iteration
                    // note: this a "expensive", so it is not done in the constructor

                    // - creates per Intent installation context (IntentUpdate)
                    // - write Intents to store
                    // - process (compile, install, etc.) each Intents
                    // - generate FlowRuleBatch for this phase
                    buildIntentUpdates();
                }

                // - peek if current FlowRuleBatch is complete
                // -- If complete OK:
                //       step each IntentUpdate forward
                //           If phase left: generate next FlowRuleBatch
                //           If no more phase: write parking states
                // -- If complete FAIL:
                //       Intent which failed: transition Intent to FAILED
                //       Other Intents: resubmit same FlowRuleBatch for this phase
                processFutures();
                if (isComplete()) {
                    // there are no outstanding batches; we are done
                    batchService.removeIntentOperations(ops);
                } else if (endTime < System.currentTimeMillis()) {
                    // - cancel current FlowRuleBatch and resubmit again
                    retry();
                } else {
                    // we are not done yet, yield the thread by resubmitting ourselves
                    executor.submit(this);
                }
            } catch (Exception e) {
                log.error("Error submitting batches:", e);
                // FIXME incomplete Intents should be cleaned up
                //       (transition to FAILED, etc.)
                abandonShip();
            }
        }
    }

    private class InternalBatchDelegate implements IntentBatchDelegate {
        @Override
        public void execute(IntentOperations operations) {
            log.info("Execute {} operation(s).", operations.operations().size());
            log.debug("Execute operations: {}", operations.operations());
            //FIXME: perhaps we want to track this task so that we can cancel it.
            executor.execute(new IntentInstallMonitor(operations));
        }

        @Override
        public void cancel(IntentOperations operations) {
            //FIXME: implement this
            log.warn("NOT IMPLEMENTED -- Cancel operations: {}", operations);
        }
    }

}
