/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentExtensionService;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.intent.IntentState.*;
import static org.onosproject.net.intent.constraint.PartialFailureConstraint.intentAllowsPartialFailure;
import static org.onosproject.net.intent.impl.phase.IntentProcessPhase.newInitialPhase;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;


/**
 * An implementation of intent service.
 */
@Component(immediate = true)
@Service
public class IntentManager
        extends AbstractListenerManager<IntentEvent, IntentListener>
        implements IntentService, IntentExtensionService {

    private static final Logger log = getLogger(IntentManager.class);

    public static final String INTENT_NULL = "Intent cannot be null";
    public static final String INTENT_ID_NULL = "Intent key cannot be null";

    private static final int NUM_THREADS = 12;

    private static final EnumSet<IntentState> RECOMPILE
            = EnumSet.of(INSTALL_REQ, FAILED, WITHDRAW_REQ);
    private static final EnumSet<IntentState> WITHDRAW
            = EnumSet.of(WITHDRAW_REQ, WITHDRAWING, WITHDRAWN);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    private ExecutorService batchExecutor;
    private ExecutorService workerExecutor;

    private final CompilerRegistry compilerRegistry = new CompilerRegistry();
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
        workerExecutor.shutdown();
        Intent.unbindIdGenerator(idGenerator);
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkPermission(INTENT_WRITE);
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.INSTALL_REQ, null);
        store.addPending(data);
    }

    @Override
    public void withdraw(Intent intent) {
        checkPermission(INTENT_WRITE);
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.WITHDRAW_REQ, null);
        store.addPending(data);
    }

    @Override
    public void purge(Intent intent) {
        checkPermission(INTENT_WRITE);
        checkNotNull(intent, INTENT_NULL);
        IntentData data = new IntentData(intent, IntentState.PURGE_REQ, null);
        store.addPending(data);
    }

    @Override
    public Intent getIntent(Key key) {
        checkPermission(INTENT_READ);
        return store.getIntent(key);
    }

    @Override
    public Iterable<Intent> getIntents() {
        checkPermission(INTENT_READ);
        return store.getIntents();
    }

    @Override
    public Iterable<IntentData> getIntentData() {
        checkPermission(INTENT_READ);
        return store.getIntentData(false, 0);
    }

    @Override
    public long getIntentCount() {
        checkPermission(INTENT_READ);
        return store.getIntentCount();
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        checkPermission(INTENT_READ);
        checkNotNull(intentKey, INTENT_ID_NULL);
        return store.getIntentState(intentKey);
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        checkPermission(INTENT_READ);
        checkNotNull(intentKey, INTENT_ID_NULL);
        return store.getInstallableIntents(intentKey);
    }

    @Override
    public boolean isLocal(Key intentKey) {
        checkPermission(INTENT_READ);
        return store.isMaster(intentKey);
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
    public Iterable<Intent> getPending() {
        checkPermission(INTENT_READ);

        return store.getPending();
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements IntentStoreDelegate {
        @Override
        public void notify(IntentEvent event) {
            post(event);
        }

        @Override
        public void process(IntentData data) {
            accumulator.add(data);
        }

        @Override
        public void onUpdate(IntentData intentData) {
            trackerService.trackIntent(intentData);
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
                if (RECOMPILE.contains(state) || intentAllowsPartialFailure(intent)) {
                    if (WITHDRAW.contains(state)) {
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

            // batchExecutor is single-threaded, so only one batch is in flight at a time
            batchExecutor.execute(new IntentBatchProcess(operations));
        }
    }

    private class InternalIntentProcessor implements IntentProcessor {
        @Override
        public List<Intent> compile(Intent intent, List<Intent> previousInstallables) {
            return compilerRegistry.compile(intent, previousInstallables);
        }

        @Override
        public void apply(Optional<IntentData> toUninstall, Optional<IntentData> toInstall) {
            IntentManager.this.apply(toUninstall, toInstall);
        }
    }

    private enum Direction {
        ADD,
        REMOVE
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

    private void apply(Optional<IntentData> toUninstall, Optional<IntentData> toInstall) {
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
                      toUninstall.isPresent() ? toUninstall.get().key() : "<empty>",
                      toInstall.isPresent() ? toInstall.get().key() : "<empty>",
                      operations.stages().stream().mapToLong(i -> i.size()).sum(),
                      operations.stages());
        }

        flowRuleService.apply(operations);
    }

}
