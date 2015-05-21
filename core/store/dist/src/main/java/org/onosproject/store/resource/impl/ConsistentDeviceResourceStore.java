/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.resource.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.device.DeviceResourceStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Store that manages device resources using Copycat-backed TransactionalMaps.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentDeviceResourceStore implements DeviceResourceStore {
    private final Logger log = getLogger(getClass());

    private static final String PORT_ALLOCATIONS = "PortAllocations";
    private static final String INTENT_ALLOCATIONS = "IntentAllocations";

    private static final Serializer SERIALIZER = Serializer.using(
            new KryoNamespace.Builder().register(KryoNamespaces.API).build());

    private ConsistentMap<Port, IntentId> portAllocMap;
    private ConsistentMap<IntentId, Set<Port>> intentAllocMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        portAllocMap = storageService.<Port, IntentId>consistentMapBuilder()
                .withName(PORT_ALLOCATIONS)
                .withSerializer(SERIALIZER)
                .build();
        intentAllocMap = storageService.<IntentId, Set<Port>>consistentMapBuilder()
                .withName(INTENT_ALLOCATIONS)
                .withSerializer(SERIALIZER)
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private TransactionalMap<Port, IntentId> getPortAllocs(TransactionContext tx) {
        return tx.getTransactionalMap(PORT_ALLOCATIONS, SERIALIZER);
    }

    private TransactionalMap<IntentId, Set<Port>> getIntentAllocs(TransactionContext tx) {
        return tx.getTransactionalMap(INTENT_ALLOCATIONS, SERIALIZER);
    }

    private TransactionContext getTxContext() {
        return storageService.transactionContextBuilder().build();
    }

    @Override
    public Set<Port> getFreePorts(DeviceId deviceId) {
        checkNotNull(deviceId);

        Set<Port> freePorts = new HashSet<>();
        for (Port port : deviceService.getPorts(deviceId)) {
            if (!portAllocMap.containsKey(port)) {
                freePorts.add(port);
            }
        }

        return freePorts;
    }

    @Override
    public void allocatePorts(Set<Port> ports, IntentId intentId) {
        checkNotNull(ports);
        checkArgument(ports.size() > 0);
        checkNotNull(intentId);

        TransactionContext tx = getTxContext();
        tx.begin();
        try {
            TransactionalMap<Port, IntentId> portAllocs = getPortAllocs(tx);
            for (Port port : ports) {
                portAllocs.put(port, intentId);
            }
            TransactionalMap<IntentId, Set<Port>> intentAllocs = getIntentAllocs(tx);
            intentAllocs.put(intentId, ports);
            tx.commit();
        } catch (Exception e) {
            log.error("Exception thrown, rolling back", e);
            tx.abort();
            throw e;
        }
    }

    @Override
    public void releasePorts(IntentId intentId) {
        checkNotNull(intentId);

        TransactionContext tx = getTxContext();
        tx.begin();
        try {
            TransactionalMap<IntentId, Set<Port>> intentAllocs = getIntentAllocs(tx);
            Set<Port> ports = intentAllocs.get(intentId);
            intentAllocs.remove(intentId);

            TransactionalMap<Port, IntentId> portAllocs = getPortAllocs(tx);
            for (Port port : ports) {
                portAllocs.remove(port);
            }
        } catch (Exception e) {
            log.error("Exception thrown, rolling back", e);
            tx.abort();
            throw e;
        }
    }
}
