/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.controller;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed implementation of MasterElectionIdStore.
 */
@Component(immediate = true, service = MasterElectionIdStore.class)
public class DistributedMasterElectionIdStore implements MasterElectionIdStore {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private static final KryoNamespace SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Pair.class)
            .register(ImmutablePair.class)
            .register(Long.class)
            .register(BigInteger.class)
            .build();

    private final Logger log = getLogger(getClass());
    private final EventuallyConsistentMapListener<Pair<DeviceId, Long>, BigInteger> mapListener =
            new InternalMapListener();

    private EventuallyConsistentMap<Pair<DeviceId, Long>, BigInteger> masterElectionIds;
    private ConcurrentMap<Pair<DeviceId, Long>, MasterElectionIdListener> listeners =
            Maps.newConcurrentMap();

    @Activate
    public void activate() {
        listeners = Maps.newConcurrentMap();
        masterElectionIds = storageService.<Pair<DeviceId, Long>,
                BigInteger>eventuallyConsistentMapBuilder()
                .withName("p4runtime-master-election-ids")
                .withSerializer(SERIALIZER)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        masterElectionIds.addListener(mapListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        masterElectionIds.removeListener(mapListener);
        masterElectionIds.destroy();
        masterElectionIds = null;
        listeners.clear();
        listeners = null;
        log.info("Stopped");
    }


    @Override
    public void set(DeviceId deviceId, long p4DeviceId, BigInteger electionId) {
        checkNotNull(deviceId);
        checkNotNull(electionId);
        masterElectionIds.put(Pair.of(deviceId, p4DeviceId), electionId);
    }

    @Override
    public BigInteger get(DeviceId deviceId, long p4DeviceId) {
        checkNotNull(deviceId);
        return masterElectionIds.get(Pair.of(deviceId, p4DeviceId));
    }

    @Override
    public void remove(DeviceId deviceId, long p4DeviceId) {
        checkNotNull(deviceId);
        masterElectionIds.remove(Pair.of(deviceId, p4DeviceId));
    }

    @Override
    public void removeAll(DeviceId deviceId) {
        masterElectionIds.keySet().forEach(k -> {
            if (k.getLeft().equals(deviceId)) {
                masterElectionIds.remove(k);
            }
        });
    }

    @Override
    public void setListener(DeviceId deviceId, long p4DeviceId,
                            MasterElectionIdListener newListener) {
        checkNotNull(deviceId);
        checkNotNull(newListener);
        listeners.compute(Pair.of(deviceId, p4DeviceId), (x, existingListener) -> {
            if (existingListener == null || existingListener == newListener) {
                return newListener;
            } else {
                log.error("Cannot add listener as one already exist for {}", deviceId);
                return existingListener;
            }
        });
    }

    @Override
    public void unsetListener(DeviceId deviceId, long p4DeviceId) {
        listeners.remove(Pair.of(deviceId, p4DeviceId));
    }

    private class InternalMapListener implements EventuallyConsistentMapListener<Pair<DeviceId, Long>, BigInteger> {
        @Override
        public void event(EventuallyConsistentMapEvent<Pair<DeviceId, Long>, BigInteger> event) {
            final MasterElectionIdListener listener = listeners.get(event.key());
            if (listener == null) {
                return;
            }
            switch (event.type()) {
                case PUT:
                    listener.updated(event.value());
                    break;
                case REMOVE:
                    listener.updated(null);
                    break;
                default:
                    log.error("Unrecognized map event type {}", event.type());
            }
        }
    }
}
