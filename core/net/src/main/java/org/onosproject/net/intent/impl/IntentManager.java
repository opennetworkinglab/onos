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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.namedThreads;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.INSTALLED;
import static org.onosproject.net.intent.IntentState.INSTALLING;
import static org.onosproject.net.intent.IntentState.INSTALL_REQ;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import static org.onosproject.net.intent.IntentState.WITHDRAW_REQ;
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

    private final IntentAccumulator accumulator = new IntentAccumulator(batchDelegate);

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        trackerService.setDelegate(topoDelegate);
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
        eventDispatcher.removeSink(IntentEvent.class);
        executor.shutdown();
        Intent.unbindIdGenerator(idGenerator);
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.INSTALL_REQ, null);
        //FIXME timestamp?
        store.addPending(data);
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.WITHDRAW_REQ, null);
        //FIXME timestamp?
        store.addPending(data);
    }

    @Override
    public void replace(IntentId oldIntentId, Intent newIntent) {
        throw new UnsupportedOperationException("replace is not implemented");
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
     * Compiles an intent recursively.
     *
     * @param intent intent
     * @return result of compilation
     */
    private List<Intent> compileIntent(Intent intent, List<Intent> previousInstallables) {
        if (intent.isInstallable()) {
            return ImmutableList.of(intent);
        }

        registerSubclassCompilerIfNeeded(intent);
        // FIXME: get previous resources
        List<Intent> installable = new ArrayList<>();
        for (Intent compiled : getCompiler(intent).compile(intent, previousInstallables, null)) {
            installable.addAll(compileIntent(compiled, previousInstallables));
        }
        return installable;
    }

    /**
     * Uninstalls all installable intents associated with the given intent.
     *
     * @param intent intent
     * @param installables installable intents
     * @return list of batches to uninstall intent
     */
    private List<FlowRuleBatchOperation> uninstallIntent(Intent intent, List<Intent> installables) {
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

        @Override
        public void process(IntentData data) {
            accumulator.add(data);
        }
    }

    private void buildAndSubmitBatches(Iterable<IntentId> intentIds,
                                       boolean compileAllFailed) {
        // Attempt recompilation of the specified intents first.
        for (IntentId id : intentIds) {
            Intent intent = store.getIntent(id);
            if (intent == null) {
                continue;
            }
            submit(intent);
        }

        if (compileAllFailed) {
            // If required, compile all currently failed intents.
            for (Intent intent : getIntents()) {
                IntentState state = getIntentState(intent.id());
                if (RECOMPILE.contains(state)) {
                    if (state == WITHDRAW_REQ) {
                        withdraw(intent);
                    } else {
                        submit(intent);
                    }
                }
            }
        }

        //FIXME
//        for (ApplicationId appId : batches.keySet()) {
//            if (batchService.isLocalLeader(appId)) {
//                execute(batches.get(appId).build());
//            }
//        }
    }

    // Topology change delegate
    private class InternalTopoChangeDelegate implements TopologyChangeDelegate {
        @Override
        public void triggerCompile(Iterable<IntentId> intentIds,
                                   boolean compileAllFailed) {
            buildAndSubmitBatches(intentIds, compileAllFailed);
        }
    }

    // TODO: simplify the branching statements
    private IntentUpdate createIntentUpdate(IntentData intentData) {
        IntentData currentState = store.getIntentData(intentData.key());
        switch (intentData.state()) {
            case INSTALL_REQ:
                return new InstallRequest(intentData.intent(), currentState);
            case WITHDRAW_REQ:
                return new WithdrawRequest(intentData.intent(), currentState);
            // fallthrough
            case COMPILING:
            case INSTALLING:
            case INSTALLED:
            case RECOMPILING:
            case WITHDRAWING:
            case WITHDRAWN:
            case FAILED:
            default:
                // illegal state
                return new DoNothing();
        }
    }

    // TODO pull out the IntentUpdate inner classes
    private class InstallRequest implements IntentUpdate {

        private final Intent intent;
        private final IntentData currentState;

        InstallRequest(Intent intent, IntentData currentState) {
            this.intent = checkNotNull(intent);
            this.currentState = currentState;
        }

        @Override
        public void writeBeforeExecution(BatchWrite batchWrite) {
            // TODO consider only "creating" intent if it does not exist
            // Note: We need to set state to INSTALL_REQ regardless.
            batchWrite.createIntent(intent);
        }

        @Override
        public Optional<IntentUpdate> execute() {
            return Optional.of(new Compiling(intent)); //FIXME
        }
    }

    private class WithdrawRequest implements IntentUpdate {

        private final Intent intent;
        private final IntentData currentState;

        WithdrawRequest(Intent intent, IntentData currentState) {
            this.intent = checkNotNull(intent);
            this.currentState = currentState;
        }

        @Override
        public void writeBeforeExecution(BatchWrite batchWrite) {
            batchWrite.setState(intent, WITHDRAW_REQ);
        }

        @Override
        public Optional<IntentUpdate> execute() {
            return Optional.of(new Withdrawing(intent, currentState.installables())); //FIXME
        }
    }

    private class Compiling implements IntentUpdate {

        private final Intent intent;

        Compiling(Intent intent) {
            this.intent = checkNotNull(intent);
        }

        @Override
        public Optional<IntentUpdate> execute() {
            try {
                return Optional.of(new Installing(intent, compileIntent(intent, null)));
            } catch (PathNotFoundException e) {
                log.debug("Path not found for intent {}", intent);
                return Optional.of(new CompilingFailed(intent));
            } catch (IntentException e) {
                log.warn("Unable to compile intent {} due to:", intent.id(), e);
                return Optional.of(new CompilingFailed(intent));
            }
        }
    }

    // TODO: better naming because install() method actually generate FlowRuleBatchOperations
    private class Installing implements IntentUpdate {

        private final Intent intent;
        private final List<Intent> installables;

        Installing(Intent intent, List<Intent> installables) {
            this.intent = checkNotNull(intent);
            this.installables = ImmutableList.copyOf(checkNotNull(installables));
        }

        @Override
        public Optional<IntentUpdate> execute() {
            Exception exception = null;

            List<FlowRuleBatchOperation> batches = Lists.newArrayList();
            for (Intent installable : installables) {
                registerSubclassInstallerIfNeeded(installable);
                trackerService.addTrackedResources(intent.id(), installable.resources());
                try {
                    batches.addAll(getInstaller(installable).install(installable));
                } catch (Exception e) { // TODO this should be IntentException
                    log.warn("Unable to install intent {} due to:", intent.id(), e);
                    trackerService.removeTrackedResources(intent.id(), installable.resources());
                    //TODO we failed; intent should be recompiled
                    exception = e;
                }
            }

            if (exception != null) {
                return Optional.of(new InstallingFailed(intent, installables, batches));
            }

            return Optional.of(new Installed(intent, installables, batches));
        }
    }

    private class Withdrawing implements IntentUpdate {

        private final Intent intent;
        private final List<Intent> installables;

        Withdrawing(Intent intent, List<Intent> installables) {
            this.intent = checkNotNull(intent);
            this.installables = ImmutableList.copyOf(installables);
        }

        @Override
        public Optional<IntentUpdate> execute() {
            List<FlowRuleBatchOperation> batches = uninstallIntent(intent, installables);

            return Optional.of(new Withdrawn(intent, installables, batches));
        }
    }

    private class Installed implements CompletedIntentUpdate {

        private final Intent intent;
        private final List<Intent> installables;
        private IntentState intentState;
        private final List<FlowRuleBatchOperation> batches;
        private int currentBatch = 0;

        Installed(Intent intent, List<Intent> installables, List<FlowRuleBatchOperation> batches) {
            this.intent = checkNotNull(intent);
            this.installables = ImmutableList.copyOf(checkNotNull(installables));
            this.batches = new LinkedList<>(checkNotNull(batches));
            this.intentState = INSTALLING;
        }

        @Override
        public void batchSuccess() {
            currentBatch++;
        }

        @Override
        public List<Intent> allInstallables() {
            return installables;
        }

        @Override
        public void writeAfterExecution(BatchWrite batchWrite) {
            switch (intentState) {
                case INSTALLING:
                    batchWrite.setState(intent, INSTALLED);
                    batchWrite.setInstallableIntents(intent.id(), this.installables);
                    break;
                case FAILED:
                    batchWrite.setState(intent, FAILED);
                    batchWrite.removeInstalledIntents(intent.id());
                    break;
                default:
                    break;
            }
        }

        @Override
        public FlowRuleBatchOperation currentBatch() {
            return currentBatch < batches.size() ? batches.get(currentBatch) : null;
        }

        @Override
        public void batchFailed() {
            for (int i = batches.size() - 1; i >= currentBatch; i--) {
                batches.remove(i);
            }
            intentState = FAILED;
            batches.addAll(uninstallIntent(intent, installables));

            // TODO we might want to try to recompile the new intent
        }
    }

    private class Withdrawn implements CompletedIntentUpdate {

        private final Intent intent;
        private final List<Intent> installables;
        private final List<FlowRuleBatchOperation> batches;
        private int currentBatch;

        Withdrawn(Intent intent, List<Intent> installables, List<FlowRuleBatchOperation> batches) {
            this.intent = checkNotNull(intent);
            this.installables = ImmutableList.copyOf(installables);
            this.batches = new LinkedList<>(batches);
            this.currentBatch = 0;
        }

        @Override
        public List<Intent> allInstallables() {
            return installables;
        }

        @Override
        public void batchSuccess() {
            currentBatch++;
        }

        @Override
        public void writeAfterExecution(BatchWrite batchWrite) {
            batchWrite.setState(intent, WITHDRAWN);
            batchWrite.removeInstalledIntents(intent.id());
            batchWrite.removeIntent(intent.id());
        }

        @Override
        public FlowRuleBatchOperation currentBatch() {
            return currentBatch < batches.size() ? batches.get(currentBatch) : null;
        }

        @Override
        public void batchFailed() {
            for (int i = batches.size() - 1; i >= currentBatch; i--) {
                batches.remove(i);
            }
            batches.addAll(uninstallIntent(intent, installables));
        }
    }

    private class InstallingFailed implements CompletedIntentUpdate {

        private final Intent intent;
        private final List<Intent> installables;
        private final List<FlowRuleBatchOperation> batches;
        private int currentBatch = 0;

        InstallingFailed(Intent intent, List<Intent> installables, List<FlowRuleBatchOperation> batches) {
            this.intent = checkNotNull(intent);
            this.installables = ImmutableList.copyOf(checkNotNull(installables));
            this.batches = new LinkedList<>(checkNotNull(batches));
        }

        @Override
        public List<Intent> allInstallables() {
            return installables;
        }

        @Override
        public void batchSuccess() {
            currentBatch++;
        }

        @Override
        public void writeAfterExecution(BatchWrite batchWrite) {
            batchWrite.setState(intent, FAILED);
            batchWrite.removeInstalledIntents(intent.id());
        }

        @Override
        public FlowRuleBatchOperation currentBatch() {
            return currentBatch < batches.size() ? batches.get(currentBatch) : null;
        }

        @Override
        public void batchFailed() {
            for (int i = batches.size() - 1; i >= currentBatch; i--) {
                batches.remove(i);
            }
            batches.addAll(uninstallIntent(intent, installables));

            // TODO we might want to try to recompile the new intent
        }
    }

    private class IntentBatchPreprocess implements Runnable {

        // TODO make this configurable
        private static final int TIMEOUT_PER_OP = 500; // ms
        protected static final int MAX_ATTEMPTS = 3;

        protected final Collection<IntentData> ops;

        // future holding current FlowRuleBatch installation result
        protected final long startTime = System.currentTimeMillis();
        protected final long endTime;

        private IntentBatchPreprocess(Collection<IntentData> ops, long endTime) {
            this.ops = checkNotNull(ops);
            this.endTime = endTime;
        }

        public IntentBatchPreprocess(Collection<IntentData> ops) {
            this(ops, System.currentTimeMillis() + ops.size() * TIMEOUT_PER_OP);
        }

        // FIXME compute reasonable timeouts
        protected long calculateTimeoutLimit() {
            return System.currentTimeMillis() + ops.size() * TIMEOUT_PER_OP;
        }

        @Override
        public void run() {
            try {
                List<IntentUpdate> updates = createIntentUpdates();

                // Write batch information
                BatchWrite batchWrite = createBatchWrite(updates);
                store.batchWrite(batchWrite);

                new IntentBatchApplyFirst(ops, processIntentUpdates(updates), endTime, 0, null).run();
            } catch (Exception e) {
                log.error("Error submitting batches:", e);
                // FIXME incomplete Intents should be cleaned up
                //       (transition to FAILED, etc.)

                // the batch has failed
                // TODO: maybe we should do more?
                log.error("Walk the plank, matey...");
                //FIXME
//            batchService.removeIntentOperations(ops);
            }
        }

        private List<IntentUpdate> createIntentUpdates() {
            return ops.stream()
                    .map(IntentManager.this::createIntentUpdate)
                    .collect(Collectors.toList());
        }

        private BatchWrite createBatchWrite(List<IntentUpdate> updates) {
            BatchWrite batchWrite = BatchWrite.newInstance();
            updates.forEach(update -> update.writeBeforeExecution(batchWrite));
            return batchWrite;
        }

        private List<CompletedIntentUpdate> processIntentUpdates(List<IntentUpdate> updates) {
            // start processing each Intents
            List<CompletedIntentUpdate> completed = new ArrayList<>();
            for (IntentUpdate update : updates) {
                Optional<IntentUpdate> phase = Optional.of(update);
                IntentUpdate previous = update;
                while (true) {
                    if (!phase.isPresent()) {
                        // FIXME: not type safe cast
                        completed.add((CompletedIntentUpdate) previous);
                        break;
                    }
                    previous = phase.get();
                    phase = previous.execute();
                }
            }

            return completed;
        }
    }

    // TODO: better naming
    private class IntentBatchApplyFirst extends IntentBatchPreprocess {

        protected final List<CompletedIntentUpdate> intentUpdates;
        protected final int installAttempt;
        protected Future<CompletedBatchOperation> future;

        IntentBatchApplyFirst(Collection<IntentData> operations, List<CompletedIntentUpdate> intentUpdates,
                              long endTime, int installAttempt, Future<CompletedBatchOperation> future) {
            super(operations, endTime);
            this.intentUpdates = ImmutableList.copyOf(intentUpdates);
            this.future = future;
            this.installAttempt = installAttempt;
        }

        @Override
        public void run() {
            Future<CompletedBatchOperation> future = applyNextBatch(intentUpdates);
            new IntentBatchProcessFutures(ops, intentUpdates, endTime, installAttempt, future).run();
        }

        /**
         * Builds and applies the next batch, and returns the future.
         *
         * @return Future for next batch
         */
        protected Future<CompletedBatchOperation> applyNextBatch(List<CompletedIntentUpdate> updates) {
            //TODO test this. (also, maybe save this batch)

            FlowRuleBatchOperation batch = createFlowRuleBatchOperation(updates);
            if (batch.size() > 0) {
                //FIXME apply batch might throw an exception
                return flowRuleService.applyBatch(batch);
            } else {
                // there are no flow rule batches; finalize the intent update
                BatchWrite batchWrite = createFinalizedBatchWrite(updates);

                store.batchWrite(batchWrite);
                return null;
            }
        }

        private FlowRuleBatchOperation createFlowRuleBatchOperation(List<CompletedIntentUpdate> intentUpdates) {
            FlowRuleBatchOperation batch = new FlowRuleBatchOperation(Collections.emptyList(), null, 0);
            for (CompletedIntentUpdate update : intentUpdates) {
                FlowRuleBatchOperation currentBatch = update.currentBatch();
                if (currentBatch != null) {
                    batch.addAll(currentBatch);
                }
            }
            return batch;
        }

        private BatchWrite createFinalizedBatchWrite(List<CompletedIntentUpdate> intentUpdates) {
            BatchWrite batchWrite = BatchWrite.newInstance();
            for (CompletedIntentUpdate update : intentUpdates) {
                update.writeAfterExecution(batchWrite);
            }
            return batchWrite;
        }

        protected void abandonShip() {
            // the batch has failed
            // TODO: maybe we should do more?
            log.error("Walk the plank, matey...");
            future = null;
            //FIXME
//            batchService.removeIntentOperations(ops);
        }
    }

    // TODO: better naming
    private class IntentBatchProcessFutures extends IntentBatchApplyFirst {

        IntentBatchProcessFutures(Collection<IntentData> operations, List<CompletedIntentUpdate> intentUpdates,
                                  long endTime, int installAttempt, Future<CompletedBatchOperation> future) {
            super(operations, intentUpdates, endTime, installAttempt, future);
        }

        @Override
        public void run() {
            try {
                Future<CompletedBatchOperation> future = processFutures();
                if (future == null) {
                    // there are no outstanding batches; we are done
                    //FIXME
                    return; //?
//                    batchService.removeIntentOperations(ops);
                } else if (System.currentTimeMillis() > endTime) {
                    // - cancel current FlowRuleBatch and resubmit again
                    retry();
                } else {
                    // we are not done yet, yield the thread by resubmitting ourselves
                    executor.submit(new IntentBatchProcessFutures(ops, intentUpdates, endTime, installAttempt, future));
                }
            } catch (Exception e) {
                log.error("Error submitting batches:", e);
                // FIXME incomplete Intents should be cleaned up
                //       (transition to FAILED, etc.)
                abandonShip();
            }
        }

        /**
         * Iterate through the pending futures, and remove them when they have completed.
         */
        private Future<CompletedBatchOperation> processFutures() {
            try {
                CompletedBatchOperation completed = future.get(100, TimeUnit.NANOSECONDS);
                updateBatches(completed);
                return applyNextBatch(intentUpdates);
            } catch (TimeoutException | InterruptedException te) {
                log.trace("Installation of intents are still pending: {}", ops);
                return future;
            } catch (ExecutionException e) {
                log.warn("Execution of batch failed: {}", ops, e);
                abandonShip();
                return future;
            }
        }

        private void updateBatches(CompletedBatchOperation completed) {
            if (completed.isSuccess()) {
                for (CompletedIntentUpdate update : intentUpdates) {
                    update.batchSuccess();
                }
            } else {
                // entire batch has been reverted...
                log.debug("Failed items: {}", completed.failedItems());
                log.debug("Failed ids: {}",  completed.failedIds());

                for (Long id : completed.failedIds()) {
                    IntentId targetId = IntentId.valueOf(id);
                    for (CompletedIntentUpdate update : intentUpdates) {
                        for (Intent intent : update.allInstallables()) {
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

        private void retry() {
            log.debug("Execution timed out, retrying.");
            if (future.cancel(true)) { // cancel success; batch is reverted
                // reset the timer
                long timeLimit = calculateTimeoutLimit();
                int attempts = installAttempt + 1;
                if (attempts == MAX_ATTEMPTS) {
                    log.warn("Install request timed out: {}", ops);
                    for (CompletedIntentUpdate update : intentUpdates) {
                        update.batchFailed();
                    }
                } else if (attempts > MAX_ATTEMPTS) {
                    abandonShip();
                    return;
                }
                Future<CompletedBatchOperation> future = applyNextBatch(intentUpdates);
                executor.submit(new IntentBatchProcessFutures(ops, intentUpdates, timeLimit, attempts, future));
            } else {
                log.error("Cancelling FlowRuleBatch failed.");
                abandonShip();
            }
        }
    }

    private class InternalBatchDelegate implements IntentBatchDelegate {
        @Override
        public void execute(Collection<IntentData> operations) {
            log.info("Execute {} operation(s).", operations.size());
            log.debug("Execute operations: {}", operations);
            executor.execute(new IntentBatchPreprocess(operations));
        }
    }
}
