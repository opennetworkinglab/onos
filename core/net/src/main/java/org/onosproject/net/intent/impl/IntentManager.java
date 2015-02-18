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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.impl.phase.CompilingFailed;
import org.onosproject.net.intent.impl.phase.FinalIntentProcessPhase;
import org.onosproject.net.intent.impl.phase.InstallRequest;
import org.onosproject.net.intent.impl.phase.IntentProcessPhase;
import org.onosproject.net.intent.impl.phase.WithdrawRequest;
import org.onosproject.net.intent.impl.phase.Withdrawn;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.INSTALLED;
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
    public static final String INTENT_ID_NULL = "Intent key cannot be null";

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

    // TODO: make this protected due to short term hack for ONOS-1051
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public FlowRuleService flowRuleService;


    private ExecutorService batchExecutor;
    private ExecutorService workerExecutor;

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
        batchExecutor = newSingleThreadExecutor(groupedThreads("onos/intent", "batch"));
        workerExecutor = newFixedThreadPool(NUM_THREADS, groupedThreads("onos/intent", "worker-%d"));
        idGenerator = coreService.getIdGenerator("intent-ids");
        Intent.bindIdGenerator(idGenerator);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        trackerService.unsetDelegate(topoDelegate);
        eventDispatcher.removeSink(IntentEvent.class);
        batchExecutor.shutdown();
        Intent.unbindIdGenerator(idGenerator);
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.INSTALL_REQ, null);
        store.addPending(data);
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.WITHDRAW_REQ, null);
        store.addPending(data);
    }

    @Override
    public Intent getIntent(Key key) {
        return store.getIntent(key);
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
    public IntentState getIntentState(Key intentKey) {
        checkNotNull(intentKey, INTENT_ID_NULL);
        return store.getIntentState(intentKey);
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        checkNotNull(intentKey, INTENT_ID_NULL);
        return store.getInstallableIntents(intentKey);
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
     * @param previousInstallables previous intent installables
     * @return result of compilation
     */
    // TODO: make this non-public due to short term hack for ONOS-1051
    public List<Intent> compileIntent(Intent intent, List<Intent> previousInstallables) {
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

    //TODO javadoc
    //FIXME
    // TODO: make this non-public due to short term hack for ONOS-1051
    public FlowRuleOperations coordinate(IntentData current, IntentData pending) {
        List<Intent> oldInstallables = (current != null) ? current.installables() : null;
        List<Intent> newInstallables = pending.installables();

        checkState(isNullOrEmpty(oldInstallables) ||
                   oldInstallables.size() == newInstallables.size(),
                   "Old and New Intent must have equivalent installable intents.");

        List<List<Set<FlowRuleOperation>>> plans = new ArrayList<>();
        for (int i = 0; i < newInstallables.size(); i++) {
            Intent newInstallable = newInstallables.get(i);
            registerSubclassInstallerIfNeeded(newInstallable);
            //TODO consider migrating installers to FlowRuleOperations
            /* FIXME
               - we need to do another pass on this method about that doesn't
               require the length of installables to be equal, and also doesn't
               depend on ordering
               - we should also reconsider when to start/stop tracking resources
             */
            if (isNullOrEmpty(oldInstallables)) {
                plans.add(getInstaller(newInstallable).install(newInstallable));
            } else {
                Intent oldInstallable = oldInstallables.get(i);
                checkState(oldInstallable.getClass().equals(newInstallable.getClass()),
                           "Installable Intent type mismatch.");
                trackerService.removeTrackedResources(pending.key(), oldInstallable.resources());
                plans.add(getInstaller(newInstallable).replace(oldInstallable, newInstallable));
            }
            trackerService.addTrackedResources(pending.key(), newInstallable.resources());
//            } catch (IntentException e) {
//                log.warn("Unable to update intent {} due to:", oldIntent.id(), e);
//                //FIXME... we failed. need to uninstall (if same) or revert (if different)
//                trackerService.removeTrackedResources(newIntent.id(), newInstallable.resources());
//                exception = e;
//                batches = uninstallIntent(oldIntent, oldInstallables);
//            }
        }

        return merge(plans).build(new FlowRuleOperationsContext() { // TODO move this out
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Completed installing: {}", pending.key());
                pending.setState(INSTALLED);
                store.write(pending);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed installation: {} {} on {}", pending.key(),
                         pending.intent(), ops);
                //TODO store.write(pending.setState(BROKEN));
                pending.setState(FAILED);
                store.write(pending);
            }
        });
    }

    /**
     * Generate a {@link FlowRuleOperations} instance from the specified intent data.
     *
     * @param current intent data stored in the store
     * @return flow rule operations
     */
    // TODO: make this non-public due to short term hack for ONOS-1051
    public FlowRuleOperations uninstallCoordinate(IntentData current, IntentData pending) {
        List<Intent> installables = current.installables();
        List<List<Set<FlowRuleOperation>>> plans = new ArrayList<>();
        for (Intent installable : installables) {
            plans.add(getInstaller(installable).uninstall(installable));
            trackerService.removeTrackedResources(pending.key(), installable.resources());
        }

        return merge(plans).build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Completed withdrawing: {}", pending.key());
                pending.setState(WITHDRAWN);
                pending.setInstallables(Collections.emptyList());
                store.write(pending);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed withdraw: {}", pending.key());
                pending.setState(FAILED);
                store.write(pending);
            }
        });
    }


    // TODO needs tests... or maybe it's just perfect
    private FlowRuleOperations.Builder merge(List<List<Set<FlowRuleOperation>>> plans) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        // Build a batch one stage at a time
        for (int stageNumber = 0;; stageNumber++) {
            // Get the sub-stage from each plan (List<Set<FlowRuleOperation>)
            for (Iterator<List<Set<FlowRuleOperation>>> itr = plans.iterator(); itr.hasNext();) {
                List<Set<FlowRuleOperation>> plan = itr.next();
                if (plan.size() <= stageNumber) {
                    // we have consumed all stages from this plan, so remove it
                    itr.remove();
                    continue;
                }
                // write operations from this sub-stage into the builder
                Set<FlowRuleOperation> stage = plan.get(stageNumber);
                for (FlowRuleOperation entry : stage) {
                    builder.operation(entry);
                }
            }
            // we are done with the stage, start the next one...
            if (plans.isEmpty()) {
                break; // we don't need to start a new stage, we are done.
            }
            builder.newStage();
        }
        return builder;
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

    private void buildAndSubmitBatches(Iterable<Key> intentKeys,
                                       boolean compileAllFailed) {
        // Attempt recompilation of the specified intents first.
        for (Key key : intentKeys) {
            Intent intent = store.getIntent(key);
            if (intent == null) {
                continue;
            }
            submit(intent);
        }

        if (compileAllFailed) {
            // If required, compile all currently failed intents.
            for (Intent intent : getIntents()) {
                IntentState state = getIntentState(intent.key());
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
        public void triggerCompile(Iterable<Key> intentKeys,
                                   boolean compileAllFailed) {
            buildAndSubmitBatches(intentKeys, compileAllFailed);
        }
    }

    private IntentProcessPhase createIntentUpdate(IntentData intentData) {
        IntentData current = store.getIntentData(intentData.key());
        switch (intentData.state()) {
            case INSTALL_REQ:
                return new InstallRequest(this, intentData, Optional.ofNullable(current));
            case WITHDRAW_REQ:
                if (current == null || isNullOrEmpty(current.installables())) {
                    return new Withdrawn(intentData, WITHDRAWN);
                } else {
                    return new WithdrawRequest(this, intentData, current);
                }
            default:
                // illegal state
                return new CompilingFailed(intentData);
        }
    }

    private Future<FinalIntentProcessPhase> submitIntentData(IntentData data) {
        return workerExecutor.submit(new IntentWorker(data));
    }

    private class IntentBatchPreprocess implements Runnable {

        // TODO make this configurable
        private static final int TIMEOUT_PER_OP = 500; // ms
        protected static final int MAX_ATTEMPTS = 3;

        protected final Collection<IntentData> data;

        // future holding current FlowRuleBatch installation result
        protected final long startTime = System.currentTimeMillis();
        protected final long endTime;

        private IntentBatchPreprocess(Collection<IntentData> data, long endTime) {
            this.data = checkNotNull(data);
            this.endTime = endTime;
        }

        public IntentBatchPreprocess(Collection<IntentData> data) {
            this(data, System.currentTimeMillis() + data.size() * TIMEOUT_PER_OP);
        }

        // FIXME compute reasonable timeouts
        protected long calculateTimeoutLimit() {
            return System.currentTimeMillis() + data.size() * TIMEOUT_PER_OP;
        }

        @Override
        public void run() {
            try {
                /*
                 1. wrap each intentdata in a runnable and submit
                 2. wait for completion of all the work
                 3. accumulate results and submit batch write of IntentData to store
                    (we can also try to update these individually)
                 */
                submitUpdates(waitForFutures(createIntentUpdates()));
            } catch (Exception e) {
                log.error("Error submitting batches:", e);
                // FIXME incomplete Intents should be cleaned up
                //       (transition to FAILED, etc.)

                // the batch has failed
                // TODO: maybe we should do more?
                log.error("Walk the plank, matey...");
                //FIXME
//            batchService.removeIntentOperations(data);
            }
        }

        private List<Future<FinalIntentProcessPhase>> createIntentUpdates() {
            return data.stream()
                    .map(IntentManager.this::submitIntentData)
                    .collect(Collectors.toList());
        }

        private List<FinalIntentProcessPhase> waitForFutures(List<Future<FinalIntentProcessPhase>> futures) {
            ImmutableList.Builder<FinalIntentProcessPhase> updateBuilder = ImmutableList.builder();
            for (Future<FinalIntentProcessPhase> future : futures) {
                try {
                    updateBuilder.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    //FIXME
                    log.warn("Future failed: {}", e);
                }
            }
            return updateBuilder.build();
        }

        private void submitUpdates(List<FinalIntentProcessPhase> updates) {
            store.batchWrite(updates.stream()
                                    .map(FinalIntentProcessPhase::data)
                                    .collect(Collectors.toList()));
        }
    }

    private final class IntentWorker implements Callable<FinalIntentProcessPhase> {

        private final IntentData data;

        private IntentWorker(IntentData data) {
            this.data = data;
        }

        @Override
        public FinalIntentProcessPhase call() throws Exception {
            IntentProcessPhase update = createIntentUpdate(data);
            Optional<IntentProcessPhase> currentPhase = Optional.of(update);
            IntentProcessPhase previousPhase = update;

            while (currentPhase.isPresent()) {
                previousPhase = currentPhase.get();
                currentPhase = previousPhase.execute();
            }
            return (FinalIntentProcessPhase) previousPhase;
        }
    }

    private class InternalBatchDelegate implements IntentBatchDelegate {
        @Override
        public void execute(Collection<IntentData> operations) {
            log.debug("Execute {} operation(s).", operations.size());
            log.trace("Execute operations: {}", operations);
            batchExecutor.execute(new IntentBatchPreprocess(operations));
            // TODO ensure that only one batch is in flight at a time
        }
    }
}
