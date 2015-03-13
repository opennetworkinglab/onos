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
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.impl.phase.FinalIntentProcessPhase;
import org.onosproject.net.intent.impl.phase.IntentProcessPhase;
import org.onosproject.net.intent.impl.phase.IntentWorker;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.INSTALL_REQ;
import static org.onosproject.net.intent.IntentState.WITHDRAW_REQ;
import static org.onosproject.net.intent.impl.phase.IntentProcessPhase.newInitialPhase;
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


    private ExecutorService batchExecutor;
    private ExecutorService workerExecutor;

    private final CompilerRegistry compilerRegistry = new CompilerRegistry();
    private final InstallerRegistry installerRegistry = new InstallerRegistry();
    private final InternalIntentProcessor processor = new InternalIntentProcessor();
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
    public void purge(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.PURGE_REQ, null);
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
    public boolean isLocal(Key intentKey) {
        return store.isMaster(intentKey);
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
        compilerRegistry.registerCompiler(cls, compiler);
    }

    @Override
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        compilerRegistry.unregisterCompiler(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers() {
        return compilerRegistry.getCompilers();
    }

    @Override
    public <T extends Intent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        installerRegistry.registerInstaller(cls, installer);
    }

    @Override
    public <T extends Intent> void unregisterInstaller(Class<T> cls) {
        installerRegistry.unregisterInstaller(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentInstaller<? extends Intent>> getInstallers() {
        return installerRegistry.getInstallers();
    }

    @Override
    public Iterable<Intent> getPending() {
        return store.getPending();
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

    private Future<FinalIntentProcessPhase> submitIntentData(IntentData data) {
        IntentData current = store.getIntentData(data.key());
        IntentProcessPhase initial = newInitialPhase(processor, data, current);
        return workerExecutor.submit(new IntentWorker(initial));
    }

    private class IntentBatchProcess implements Runnable {

        protected final Collection<IntentData> data;

        IntentBatchProcess(Collection<IntentData> data) {
            this.data = checkNotNull(data);
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
            accumulator.ready();
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

    private class InternalBatchDelegate implements IntentBatchDelegate {
        @Override
        public void execute(Collection<IntentData> operations) {
            log.debug("Execute {} operation(s).", operations.size());
            log.trace("Execute operations: {}", operations);
            batchExecutor.execute(new IntentBatchProcess(operations));
            // TODO ensure that only one batch is in flight at a time
        }
    }

    private class InternalIntentProcessor implements IntentProcessor {
        @Override
        public List<Intent> compile(Intent intent, List<Intent> previousInstallables) {
            return compilerRegistry.compile(intent, previousInstallables);
        }

        @Override
        public FlowRuleOperations coordinate(IntentData current, IntentData pending) {
            return installerRegistry.coordinate(current, pending, store, trackerService);
        }

        @Override
        public FlowRuleOperations uninstallCoordinate(IntentData current, IntentData pending) {
            return installerRegistry.uninstallCoordinate(current, pending, store, trackerService);
        }

        @Override
        public void applyFlowRules(FlowRuleOperations flowRules) {
            flowRuleService.apply(flowRules);
        }
    }
}
