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
package org.onlab.onos.net.intent.impl;

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
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentBatchDelegate;
import org.onlab.onos.net.intent.IntentBatchService;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentException;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentOperation;
import org.onlab.onos.net.intent.IntentOperations;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.onos.net.intent.IntentState.*;
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

    // Collections for compiler, installer, and listener are ONOS instance local
    private final ConcurrentMap<Class<? extends Intent>,
            IntentCompiler<? extends Intent>> compilers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends Intent>,
            IntentInstaller<? extends Intent>> installers = new ConcurrentHashMap<>();

    private final AbstractListenerRegistry<IntentEvent, IntentListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private ExecutorService executor;
    private ExecutorService monitorExecutor;

    private final IntentStoreDelegate delegate = new InternalStoreDelegate();
    private final TopologyChangeDelegate topoDelegate = new InternalTopoChangeDelegate();
    private final IntentBatchDelegate batchDelegate = new InternalBatchDelegate();

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

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        trackerService.setDelegate(topoDelegate);
        batchService.setDelegate(batchDelegate);
        eventDispatcher.addSink(IntentEvent.class, listenerRegistry);
        executor = newSingleThreadExecutor(namedThreads("onos-intents"));
        monitorExecutor = newSingleThreadExecutor(namedThreads("onos-intent-monitor"));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        trackerService.unsetDelegate(topoDelegate);
        batchService.unsetDelegate(batchDelegate);
        eventDispatcher.removeSink(IntentEvent.class);
        executor.shutdown();
        monitorExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        execute(IntentOperations.builder().addSubmitOperation(intent).build());
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        execute(IntentOperations.builder().addWithdrawOperation(intent.id()).build());
    }

    @Override
    public void replace(IntentId oldIntentId, Intent newIntent) {
        checkNotNull(oldIntentId, INTENT_ID_NULL);
        checkNotNull(newIntent, INTENT_NULL);
        execute(IntentOperations.builder()
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
        update.setState(intent, COMPILING);

        try {
            // Compile the intent into installable derivatives.
            List<Intent> installables = compileIntent(intent, update);

            // If all went well, associate the resulting list of installable
            // intents with the top-level intent and proceed to install.
            update.setInstallables(installables);
        } catch (IntentException e) {
            log.warn("Unable to compile intent {} due to:", intent.id(), e);

            // If compilation failed, mark the intent as failed.
            update.setState(intent, FAILED);
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
        update.setState(update.newIntent(), INSTALLING);

        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (Intent installable : update.newInstallables()) {
            registerSubclassInstallerIfNeeded(installable);
            trackerService.addTrackedResources(update.newIntent().id(),
                                               installable.resources());
            try {
                batches.addAll(getInstaller(installable).install(installable));
            } catch (IntentException e) {
                log.warn("Unable to install intent {} due to:", update.newIntent().id(), e);
                //FIXME we failed... intent should be recompiled
                // TODO: remove resources
                // recompile!!!
            }
        }
        update.setBatches(batches);
    }

    /**
     * Uninstalls the specified intent by uninstalling all of its associated
     * installable derivatives.
     *
     * @param update intent update
     */
    private void executeWithdrawingPhase(IntentUpdate update) {
        if (!update.oldIntent().equals(update.newIntent())) {
            update.setState(update.oldIntent(), WITHDRAWING);
        } // else newIntent is FAILED
        uninstallIntent(update);

        // If all went well, disassociate the top-level intent with its
        // installable derivatives and mark it as withdrawn.
        // FIXME need to clean up
        //store.removeInstalledIntents(intent.id());
    }

    /**
     * Uninstalls all installable intents associated with the given intent.
     *
     * @param update intent update
     */
    //FIXME: need to handle next state properly
    private void uninstallIntent(IntentUpdate update) {
        if (update.oldInstallables == null) {
            return;
        }
        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (Intent installable : update.oldInstallables()) {
            trackerService.removeTrackedResources(update.oldIntent().id(),
                                                  installable.resources());
            try {
                batches.addAll(getInstaller(installable).uninstall(installable));
            } catch (IntentException e) {
                log.warn("Unable to uninstall intent {} due to:", update.oldIntent().id(), e);
                // TODO: this should never happen. but what if it does?
            }
        }
        update.setBatches(batches);
        // FIXME: next state for old is WITHDRAWN or FAILED
    }

    /**
     * Recompiles the specified intent.
     *
     * @param update intent update
     */
    // FIXME: update this to work
    private void executeRecompilingPhase(IntentUpdate update) {
        Intent intent = update.newIntent();
        // Indicate that the intent is entering the recompiling phase.
        store.setState(intent, RECOMPILING);

        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        try {
            // Compile the intent into installable derivatives.
            List<Intent> installable = compileIntent(intent, update);

            // If all went well, compare the existing list of installable
            // intents with the newly compiled list. If they are the same,
            // bail, out since the previous approach was determined not to
            // be viable.
            // FIXME do we need this?
            List<Intent> originalInstallable = store.getInstallableIntents(intent.id());

            //FIXME let's be smarter about how we perform the update
            //batches.addAll(uninstallIntent(intent, null));

            if (Objects.equals(originalInstallable, installable)) {
                eventDispatcher.post(store.setState(intent, FAILED));
            } else {
                // Otherwise, re-associate the newly compiled installable intents
                // with the top-level intent and kick off installing phase.
                store.setInstallableIntents(intent.id(), installable);
                // FIXME commented out for now
                //batches.addAll(executeInstallingPhase(update));
            }
        } catch (Exception e) {
            log.warn("Unable to recompile intent {} due to:", intent.id(), e);

            // If compilation failed, mark the intent as failed.
            eventDispatcher.post(store.setState(intent, FAILED));
        }
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
            update.setState(update.oldIntent(), WITHDRAWING);
        }
        update.setState(update.newIntent(), INSTALLING);

        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (int i = 0; i < update.oldInstallables().size(); i++) {
            Intent oldInstallable = update.oldInstallables().get(i);
            Intent newInstallable = update.newInstallables().get(i);
            if (oldInstallable.equals(newInstallable)) {
                continue;
            }
            checkArgument(oldInstallable.getClass().equals(newInstallable.getClass()),
                          "Installable Intent type mismatch.");
            trackerService.removeTrackedResources(update.oldIntent().id(), oldInstallable.resources());
            trackerService.addTrackedResources(update.newIntent().id(), newInstallable.resources());
            try {
                batches.addAll(getInstaller(newInstallable).replace(oldInstallable, newInstallable));
            } catch (IntentException e) {
                log.warn("Unable to update intent {} due to:", update.oldIntent().id(), e);
                //FIXME... we failed. need to uninstall (if same) or revert (if different)
            }
        }
        update.setBatches(batches);
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

    // Topology change delegate
    private class InternalTopoChangeDelegate implements TopologyChangeDelegate {
        @Override
        public void triggerCompile(Iterable<IntentId> intentIds,
                                   boolean compileAllFailed) {
            // Attempt recompilation of the specified intents first.
            IntentOperations.Builder builder = IntentOperations.builder();
            for (IntentId id : intentIds) {
                builder.addUpdateOperation(id);
            }

            if (compileAllFailed) {
                // If required, compile all currently failed intents.
                for (Intent intent : getIntents()) {
                    if (getIntentState(intent.id()) == FAILED) {
                        builder.addUpdateOperation(intent.id());
                    }
                }
            }
            execute(builder.build());
        }
    }

    /**
     * TODO.
     * @param op intent operation
     * @return intent update
     */
    private IntentUpdate processIntentOperation(IntentOperation op) {
        IntentUpdate update = new IntentUpdate(op);

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
            if (update.oldIntent() != null) {
                // TODO this shouldn't happen
                return update; //FIXME
            }
            if (update.newIntent() != null) {
                // TODO assert that next state is failed
                return update; //FIXME
            }
        }

        return update;
    }

    // TODO comments...
    private class IntentUpdate {
        private final IntentOperation op;
        private final Intent oldIntent;
        private final Intent newIntent;
        private final Map<Intent, IntentState> stateMap = Maps.newHashMap();

        private final List<Intent> oldInstallables;
        private List<Intent> newInstallables;
        private List<FlowRuleBatchOperation> batches;

        IntentUpdate(IntentOperation op) {
            this.op = op;
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
                    newIntent = oldIntent; //InnerAssignment: Inner assignments should be avoided.
                    break;
                default:
                    oldIntent = null;
                    newIntent = null;
                    break;
            }
            // add new intent to store (if required)
            if (newIntent != null) {
                IntentEvent event = store.createIntent(newIntent);
                if (event != null) {
                    eventDispatcher.post(event);
                }
            }
            // fetch the old intent's installables from the store
            if (oldIntent != null) {
                oldInstallables = store.getInstallableIntents(oldIntent.id());
                // TODO: remove intent from store after uninstall
            } else {
                oldInstallables = null;
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
            store.setInstallableIntents(newIntent.id(), installables);
        }

        List<FlowRuleBatchOperation> batches() {
            return batches;
        }

        void setBatches(List<FlowRuleBatchOperation> batches) {
            this.batches = batches;
        }

        IntentState getState(Intent intent) {
            return stateMap.get(intent);
        }

        void setState(Intent intent, IntentState newState) {
            // TODO: clean this up, or set to debug
            IntentState oldState = stateMap.get(intent);
            log.info("intent id: {}, old state: {}, new state: {}",
                     intent.id(), oldState, newState);

            stateMap.put(intent, newState);
            IntentEvent event = store.setState(intent, newState);
            if (event != null) {
                eventDispatcher.post(event);
            }
        }

        Map<Intent, IntentState> stateMap() {
            return stateMap;
        }
    }

    private static List<FlowRuleBatchOperation> mergeBatches(Map<IntentOperation,
            IntentUpdate> intentUpdates) {
        //TODO test this.
        List<FlowRuleBatchOperation> batches = Lists.newArrayList();
        for (IntentUpdate update : intentUpdates.values()) {
            if (update.batches() == null) {
                continue;
            }
            int i = 0;
            for (FlowRuleBatchOperation batch : update.batches()) {
                if (i == batches.size()) {
                    batches.add(batch);
                } else {
                    FlowRuleBatchOperation existing = batches.get(i);
                    existing.addAll(batch);
                }
                i++;
            }
        }
        return batches;
    }

    // Auxiliary runnable to perform asynchronous tasks.
    private class IntentTask implements Runnable {
        private final IntentOperations operations;

        public IntentTask(IntentOperations operations) {
            this.operations = operations;
        }

        @Override
        public void run() {
            Map<IntentOperation, IntentUpdate> intentUpdates = Maps.newHashMap();
            for (IntentOperation op : operations.operations()) {
                intentUpdates.put(op, processIntentOperation(op));
            }
            List<FlowRuleBatchOperation> batches = mergeBatches(intentUpdates);
            monitorExecutor.execute(new IntentInstallMonitor(operations, intentUpdates, batches));
        }
    }

    private class IntentInstallMonitor implements Runnable {

        private static final long TIMEOUT = 5000; // ms
        private final IntentOperations ops;
        private final Map<IntentOperation, IntentUpdate> intentUpdateMap;
        private final List<FlowRuleBatchOperation> work;
        private Future<CompletedBatchOperation> future;
        private final long startTime = System.currentTimeMillis();
        private final long endTime = startTime + TIMEOUT;

        public IntentInstallMonitor(IntentOperations ops,
                                    Map<IntentOperation, IntentUpdate> intentUpdateMap,
                                    List<FlowRuleBatchOperation> work) {
            this.ops = ops;
            this.intentUpdateMap = intentUpdateMap;
            this.work = work;
            future = applyNextBatch();
        }

        /**
         * Applies the next batch, and returns the future.
         *
         * @return Future for next batch
         */
        private Future<CompletedBatchOperation> applyNextBatch() {
            if (work.isEmpty()) {
                return null;
            }
            FlowRuleBatchOperation batch = work.remove(0);
            return flowRuleService.applyBatch(batch);
        }

        /**
         * Update the intent store with the next status for this intent.
         */
        private void updateIntents() {
            // FIXME we assume everything passes for now.
            for (IntentUpdate update : intentUpdateMap.values()) {
                for (Intent intent : update.stateMap().keySet()) {
                    switch (update.getState(intent)) {
                        case INSTALLING:
                            update.setState(intent, INSTALLED);
                            break;
                        case WITHDRAWING:
                            update.setState(intent, WITHDRAWN);
                        // Fall-through
                        case FAILED:
                            store.removeInstalledIntents(intent.id());
                            break;

                        case SUBMITTED:
                        case COMPILING:
                        case RECOMPILING:
                        case WITHDRAWN:
                        case INSTALLED:
                        default:
                            //FIXME clean this up (we shouldn't ever get here)
                            log.warn("Bad state: {} for {}", update.getState(intent), intent);
                            break;
                    }
                }
            }
            /*
            for (IntentOperation op : ops.operations()) {
                switch (op.type()) {
                    case SUBMIT:
                        store.setState(op.intent(), INSTALLED);
                        break;
                    case WITHDRAW:
                        Intent intent = store.getIntent(op.intentId());
                        store.setState(intent, WITHDRAWN);
                        break;
                    case REPLACE:
                        store.setState(op.intent(), INSTALLED);
                        intent = store.getIntent(op.intentId());
                        store.setState(intent, WITHDRAWN);
                        break;
                    case UPDATE:
                        intent = store.getIntent(op.intentId());
                        store.setState(intent, INSTALLED);
                        break;
                    default:
                        break;
                }
            }
            */
            /*
            if (nextState == RECOMPILING) {
                eventDispatcher.post(store.setState(intent, FAILED));
                // FIXME try to recompile
//                executor.execute(new IntentTask(nextState, intent));
            } else if (nextState == INSTALLED || nextState == WITHDRAWN) {
                eventDispatcher.post(store.setState(intent, nextState));
            } else {
                log.warn("Invalid next intent state {} for intent {}", nextState, intent);
            }*/
        }

        /**
         * Iterate through the pending futures, and remove them when they have completed.
         */
        private void processFutures() {
            if (future == null) {
                return; //FIXME look at this
            }
            try {
                CompletedBatchOperation completed = future.get(100, TimeUnit.NANOSECONDS);
                if (completed.isSuccess()) {
                    future = applyNextBatch();
                } else {
                    // TODO check if future succeeded and if not report fail items
                    log.warn("Failed items: {}", completed.failedItems());
                    // FIXME revert.... by submitting a new batch
                    //uninstallIntent(intent, RECOMPILING);
                }
            } catch (TimeoutException | InterruptedException | ExecutionException te) {
                //TODO look into error message
                log.debug("Intallations of intent {} is still pending", ops);
            }
        }

        @Override
        public void run() {
            processFutures();
            if (future == null) {
                // woohoo! we are done!
                updateIntents();
                batchService.removeIntentOperations(ops);
            } else if (endTime < System.currentTimeMillis()) {
                log.warn("Install request timed out");
//                future.cancel(true);
                // TODO retry and/or report the failure
            } else {
                // resubmit ourselves if we are not done yet
                monitorExecutor.submit(this);
            }
        }
    }

    private class InternalBatchDelegate implements IntentBatchDelegate {
        @Override
        public void execute(IntentOperations operations) {
            log.info("Execute operations: {}", operations);
            //FIXME: perhaps we want to track this task so that we can cancel it.
            executor.execute(new IntentTask(operations));
        }

        @Override
        public void cancel(IntentOperations operations) {
            //FIXME: implement this
            log.warn("NOT IMPLEMENTED -- Cancel operations: {}", operations);
        }
    }
}
