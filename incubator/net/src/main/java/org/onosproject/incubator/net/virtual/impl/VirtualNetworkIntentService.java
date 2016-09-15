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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Iterators;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;

/**
 * Intent service implementation built on the virtual network service.
 */
public class VirtualNetworkIntentService extends AbstractListenerManager<IntentEvent, IntentListener>
        implements IntentService, VnetService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NETWORK_NULL = "Network cannot be null";
    private static final String NETWORK_ID_NULL = "Network ID cannot be null";
    private static final String DEVICE_NULL = "Device cannot be null";
    private static final String INTENT_NULL = "Intent cannot be null";
    private static final String KEY_NULL = "Key cannot be null";
    private static final String APP_ID_NULL = "Intent app identifier cannot be null";
    private static final String INTENT_KEY_NULL = "Intent key cannot be null";
    private static final String CP_NULL = "Connect Point cannot be null";

    protected IntentService intentService;
    protected VirtualNetworkStore store;
    protected WorkPartitionService partitionService;

    private final VirtualNetwork network;
    private final VirtualNetworkService manager;

    /**
     * Creates a new VirtualNetworkIntentService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param network               virtual network
     * @param serviceDirectory      service directory
     */
    public VirtualNetworkIntentService(VirtualNetworkService virtualNetworkManager, VirtualNetwork network,
                                       ServiceDirectory serviceDirectory) {
        checkNotNull(network, NETWORK_NULL);
        this.network = network;
        this.manager = virtualNetworkManager;
        this.store = serviceDirectory.get(VirtualNetworkStore.class);
        this.intentService = serviceDirectory.get(IntentService.class);
        this.partitionService = serviceDirectory.get(WorkPartitionService.class);
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        checkState(intent instanceof VirtualNetworkIntent, "Only VirtualNetworkIntent is supported.");
        checkArgument(validateIntent((VirtualNetworkIntent) intent), "Invalid Intent");

        intentService.submit(intent);
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
        return port == null ? false : true;
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

        Optional<VirtualPort> foundPort = manager.getVirtualPorts(this.network.id(), deviceId)
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
        // Withdraws the physical intents created due to the virtual intents.
        store.getTunnelIds(intent).forEach(tunnelId -> {
            Key intentKey = Key.of(tunnelId.id(), intent.appId());
            Intent physicalIntent = intentService.getIntent(intentKey);
            checkNotNull(physicalIntent, INTENT_NULL);

            // Withdraw the physical intent(s)
            log.debug("Withdrawing pt-pt intent: " + physicalIntent);
            intentService.withdraw(physicalIntent);
        });
        // Now withdraw the virtual intent
        log.debug("Withdrawing virtual intent: " + intent);
        intentService.withdraw(intent);
    }

    @Override
    public void purge(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        // Purges the physical intents created for each tunnelId.
        store.getTunnelIds(intent)
                .forEach(tunnelId -> {
                    Key intentKey = Key.of(tunnelId.id(), intent.appId());
                    Intent physicalIntent = intentService.getIntent(intentKey);
                    checkNotNull(physicalIntent, INTENT_NULL);

                    // Purge the physical intent(s)
                    intentService.purge(physicalIntent);
                    store.removeTunnelId(intent, tunnelId);
                });
        // Now purge the virtual intent
        intentService.purge(intent);
    }

    @Override
    public Intent getIntent(Key key) {
        checkNotNull(key, KEY_NULL);
        return store.getIntent(key);
    }

    @Override
    public Iterable<Intent> getIntents() {
        return store.getIntents();
    }

    @Override
    public Iterable<IntentData> getIntentData() {
        return store.getIntentData();
    }

    @Override
    public long getIntentCount() {
        return Iterators.size(getIntents().iterator());
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        checkNotNull(intentKey, KEY_NULL);
        return Optional.ofNullable(store.getIntentData(intentKey))
                .map(IntentData::state)
                .orElse(null);
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        List<Intent> intents = new ArrayList<>();
        getIntentData().forEach(intentData -> {
            if (intentData.intent().key().equals(intentKey)) {
                intents.addAll(intentData.installables());
            }
        });
        return intents;
    }

    @Override
    public boolean isLocal(Key intentKey) {
        checkNotNull(intentKey, INTENT_KEY_NULL);
        Intent intent = getIntent(intentKey);
        checkNotNull(intent, INTENT_NULL);
        return partitionService.isMine(intentKey, Key::hash);
    }

    @Override
    public Iterable<Intent> getPending() {
        return null;
    }


    @Override
    public VirtualNetwork network() {
        return network;
    }
}
