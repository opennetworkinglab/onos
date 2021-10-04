/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.Tools;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntentStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.VnetService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.incubator.net.virtual.impl.intent.phase.VirtualFinalIntentProcessPhase;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentInstallCoordinator;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentAccumulator;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentCompilerRegistry;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentInstallerRegistry;
import org.onosproject.incubator.net.virtual.impl.intent.phase.VirtualIntentProcessPhase;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentProcessor;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentSkipped;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.resource.ResourceConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;
import static org.onlab.util.BoundedThreadPool.newFixedThreadPool;
import static org.onlab.util.BoundedThreadPool.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.incubator.net.virtual.impl.intent.phase.VirtualIntentProcessPhase.newInitialPhase;
import static org.onosproject.net.intent.IntentState.FAILED;

/**
 * Intent service implementation built on the virtual network service.
 */
public class VirtualNetworkIntentManager
        extends AbstractVirtualListenerManager<IntentEvent, IntentListener>
        implements IntentService, VnetService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_NUM_THREADS = 12;
    private int numThreads = DEFAULT_NUM_THREADS;

    private static final String NETWORK_ID_NULL = "Network ID cannot be null";
    private static final String DEVICE_NULL = "Device cannot be null";
    private static final String INTENT_NULL = "Intent cannot be null";
    private static final String KEY_NULL = "Key cannot be null";
    private static final String APP_ID_NULL = "Intent app identifier cannot be null";
    private static final String INTENT_KEY_NULL = "Intent key cannot be null";
    private static final String CP_NULL = "Connect Point cannot be null";

    //FIXME: Tracker service for vnet.

    //ONOS core services
    protected VirtualNetworkStore virtualNetworkStore;
    protected VirtualNetworkIntentStore intentStore;

    //Virtual network services
    protected GroupService groupService;

    private final IntentBatchDelegate batchDelegate = new InternalBatchDelegate();
    private final InternalIntentProcessor processor = new InternalIntentProcessor();
    private final IntentStoreDelegate delegate = new InternalStoreDelegate();
    private final VirtualIntentCompilerRegistry compilerRegistry =
            VirtualIntentCompilerRegistry.getInstance();
    private final VirtualIntentInstallerRegistry installerRegistry =
            VirtualIntentInstallerRegistry.getInstance();
    private final VirtualIntentAccumulator accumulator =
            new VirtualIntentAccumulator(batchDelegate);

    private VirtualIntentInstallCoordinator installCoordinator;
    private ExecutorService batchExecutor;
    private ExecutorService workerExecutor;

    /**
     * Creates a new VirtualNetworkIntentService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param networkId a virtual network identifier
     */
    public VirtualNetworkIntentManager(VirtualNetworkService virtualNetworkManager,
                                       NetworkId networkId) {

        super(virtualNetworkManager, networkId, IntentEvent.class);

        this.virtualNetworkStore = serviceDirectory.get(VirtualNetworkStore.class);
        this.intentStore = serviceDirectory.get(VirtualNetworkIntentStore.class);

        this.groupService = manager.get(networkId, GroupService.class);

        intentStore.setDelegate(networkId, delegate);
        batchExecutor = newSingleThreadExecutor(groupedThreads("onos/intent", "batch", log));
        workerExecutor = newFixedThreadPool(numThreads, groupedThreads("onos/intent", "worker-%d", log));

        installCoordinator = new VirtualIntentInstallCoordinator(networkId, installerRegistry, intentStore);
        log.info("Started");

    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        checkState(intent instanceof VirtualNetworkIntent, "Only VirtualNetworkIntent is supported.");
        checkArgument(validateIntent((VirtualNetworkIntent) intent), "Invalid Intent");

        IntentData data = IntentData.submit(intent);
        intentStore.addPending(networkId, data);
    }

    /**
     * Returns true if the virtual network intent is valid.
     *
     * @param intent virtual network intent
     * @return true if intent is valid
     */
    private boolean validateIntent(VirtualNetworkIntent intent) {
        checkNotNull(intent, INTENT_NULL);
        checkNotNull(intent.networkId(), NETWORK_ID_NULL);
        checkNotNull(intent.appId(), APP_ID_NULL);
        checkNotNull(intent.key(), INTENT_KEY_NULL);
        ConnectPoint ingressPoint = intent.ingressPoint();
        ConnectPoint egressPoint = intent.egressPoint();

        return (validateConnectPoint(ingressPoint) && validateConnectPoint(egressPoint));
    }

    /**
     * Returns true if the connect point is valid.
     *
     * @param connectPoint connect point
     * @return true if connect point is valid
     */
    private boolean validateConnectPoint(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, CP_NULL);
        Port port = getPort(connectPoint.deviceId(), connectPoint.port());
        return port != null;
    }

    /**
     * Returns the virtual port for the given device identifier and port number.
     *
     * @param deviceId   virtual device identifier
     * @param portNumber virtual port number
     * @return virtual port
     */
    private Port getPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_NULL);

        Optional<VirtualPort> foundPort = manager.getVirtualPorts(this.networkId(), deviceId)
                .stream()
                .filter(port -> port.number().equals(portNumber))
                .findFirst();
        if (foundPort.isPresent()) {
            return foundPort.get();
        }
        return null;
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentData data = IntentData.withdraw(intent);
        intentStore.addPending(networkId, data);
    }

    @Override
    public void purge(Intent intent) {
        checkNotNull(intent, INTENT_NULL);

        IntentData data = IntentData.purge(intent);
        intentStore.addPending(networkId, data);

        // remove associated group if there is one
        // FIXME: Remove P2P intent for vnets
    }

    @Override
    public Intent getIntent(Key key) {
        checkNotNull(key, KEY_NULL);
        return intentStore.getIntent(networkId, key);
    }

    @Override
    public Iterable<Intent> getIntents() {
        return intentStore.getIntents(networkId);
    }

    @Override
    public Iterable<Intent> getIntentsByAppId(ApplicationId id) {
        ImmutableSet.Builder<Intent> builder = ImmutableSet.builder();
        for (Intent intent : intentStore.getIntents(networkId)) {
            if (intent.appId().equals(id)) {
                builder.add(intent);
            }
        }

        return builder.build();
    }

    @Override
    public void addPending(IntentData intentData) {
        checkNotNull(intentData, INTENT_NULL);
        //TODO we might consider further checking / assertions
        intentStore.addPending(networkId, intentData);
    }

    @Override
    public Iterable<IntentData> getIntentData() {
        return intentStore.getIntentData(networkId, false, 0);
    }

    @Override
    public long getIntentCount() {
        return intentStore.getIntentCount(networkId);
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        checkNotNull(intentKey, KEY_NULL);
        return intentStore.getIntentState(networkId, intentKey);
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        return intentStore.getInstallableIntents(networkId, intentKey);
    }

    @Override
    public boolean isLocal(Key intentKey) {
        return intentStore.isMaster(networkId, intentKey);
    }

    @Override
    public Iterable<Intent> getPending() {
        return intentStore.getPending(networkId);
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements IntentStoreDelegate {
        @Override
        public void notify(IntentEvent event) {
            post(event);
            switch (event.type()) {
                case WITHDRAWN:
                    //FIXME: release resources
                    break;
                default:
                    break;
            }
        }

        @Override
        public void process(IntentData data) {
            accumulator.add(data);
        }

        @Override
        public void onUpdate(IntentData intentData) {
            //FIXME: track intent
        }

        private void releaseResources(Intent intent) {
            // If a resource group is set on the intent, the resource consumer is
            // set equal to it. Otherwise it's set to the intent key
            ResourceConsumer resourceConsumer =
                    intent.resourceGroup() != null ? intent.resourceGroup() : intent.key();

            // By default the resource doesn't get released
            boolean removeResource = false;

            if (intent.resourceGroup() == null) {
                // If the intent doesn't have a resource group, it means the
                // resource was registered using the intent key, so it can be
                // released
                removeResource = true;
            } else {
                // When a resource group is set, we make sure there are no other
                // intents using the same resource group, before deleting the
                // related resources.
                Long remainingIntents =
                        Tools.stream(intentStore.getIntents(networkId))
                                .filter(i -> {
                                    return i.resourceGroup() != null
                                            && i.resourceGroup().equals(intent.resourceGroup());
                                })
                                .count();
                if (remainingIntents == 0) {
                    removeResource = true;
                }
            }

            if (removeResource) {
                // Release resources allocated to withdrawn intent
                // FIXME: confirm resources are released
            }
        }
    }

    private class InternalBatchDelegate implements IntentBatchDelegate {
        @Override
        public void execute(Collection<IntentData> operations) {
            log.debug("Execute {} operation(s).", operations.size());
            log.trace("Execute operations: {}", operations);

            // batchExecutor is single-threaded, so only one batch is in flight at a time
            CompletableFuture.runAsync(() -> {
                // process intent until the phase reaches one of the final phases
                List<CompletableFuture<IntentData>> futures = operations.stream()
                        .map(data -> {
                            log.debug("Start processing of {} {}@{}", data.request(), data.key(), data.version());
                            return data;
                        })
                        .map(x -> CompletableFuture.completedFuture(x)
                                .thenApply(VirtualNetworkIntentManager.this::createInitialPhase)
                                .thenApplyAsync(VirtualIntentProcessPhase::process, workerExecutor)
                                .thenApply(VirtualFinalIntentProcessPhase::data)
                                .exceptionally(e -> {
                                    // When the future fails, we update the Intent to simulate the failure of
                                    // the installation/withdrawal phase and we save in the current map. In
                                    // the next round the CleanUp Thread will pick this Intent again.
                                    log.warn("Future failed", e);
                                    log.warn("Intent {} - state {} - request {}",
                                             x.key(), x.state(), x.request());
                                    switch (x.state()) {
                                        case INSTALL_REQ:
                                        case INSTALLING:
                                        case WITHDRAW_REQ:
                                        case WITHDRAWING:
                                            // TODO should we swtich based on current
                                            IntentData current = intentStore.getIntentData(networkId, x.key());
                                            return IntentData.nextState(current, FAILED);
                                        default:
                                            return null;
                                    }
                                }))
                        .collect(Collectors.toList());

                // write multiple data to store in order
                intentStore.batchWrite(networkId, Tools.allOf(futures).join().stream()
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toList()));
            }, batchExecutor).exceptionally(e -> {
                log.error("Error submitting batches:", e);
                // FIXME incomplete Intents should be cleaned up
                //       (transition to FAILED, etc.)

                // the batch has failed
                // TODO: maybe we should do more?
                log.error("Walk the plank, matey...");
                return null;
            }).thenRun(accumulator::ready);

        }
    }

    private VirtualIntentProcessPhase createInitialPhase(IntentData data) {
        IntentData pending = intentStore.getPendingData(networkId, data.key());
        if (pending == null || pending.version().isNewerThan(data.version())) {
            /*
                If the pending map is null, then this intent was compiled by a
                previous batch iteration, so we can skip it.
                If the pending map has a newer request, it will get compiled as
                part of the next batch, so we can skip it.
             */
            return VirtualIntentSkipped.getPhase();
        }
        IntentData current = intentStore.getIntentData(networkId, data.key());
        return newInitialPhase(networkId, processor, data, current);
    }

    private class InternalIntentProcessor implements VirtualIntentProcessor {
        @Override
        public List<Intent> compile(NetworkId networkId,
                                    Intent intent,
                                    List<Intent> previousInstallables) {
            return compilerRegistry.compile(networkId, intent, previousInstallables);
        }

        @Override
        public void apply(NetworkId networkId,
                          Optional<IntentData> toUninstall,
                          Optional<IntentData> toInstall) {

            installCoordinator.installIntents(toUninstall, toInstall);
        }
    }
}
