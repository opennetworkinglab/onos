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
            .register(BigInteger.class)
            .build();

    private final Logger log = getLogger(getClass());
    private final EventuallyConsistentMapListener<DeviceId, BigInteger> mapListener =
            new InternalMapListener();

    private EventuallyConsistentMap<DeviceId, BigInteger> masterElectionIds;
    private ConcurrentMap<DeviceId, MasterElectionIdListener> listeners =
            Maps.newConcurrentMap();

    @Activate
    public void activate() {
        this.listeners = Maps.newConcurrentMap();
        this.masterElectionIds = storageService.<DeviceId, BigInteger>eventuallyConsistentMapBuilder()
                .withName("p4runtime-master-election-ids")
                .withSerializer(SERIALIZER)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        this.masterElectionIds.addListener(mapListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        this.masterElectionIds.removeListener(mapListener);
        this.masterElectionIds.destroy();
        this.masterElectionIds = null;
        this.listeners.clear();
        this.listeners = null;
        log.info("Stopped");
    }


    @Override
    public void set(DeviceId deviceId, BigInteger electionId) {
        checkNotNull(deviceId);
        checkNotNull(electionId);
        this.masterElectionIds.put(deviceId, electionId);
    }

    @Override
    public BigInteger get(DeviceId deviceId) {
        checkNotNull(deviceId);
        return this.masterElectionIds.get(deviceId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        checkNotNull(deviceId);
        this.masterElectionIds.remove(deviceId);
    }

    @Override
    public void setListener(DeviceId deviceId, MasterElectionIdListener newListener) {
        checkNotNull(deviceId);
        checkNotNull(newListener);
        listeners.compute(deviceId, (did, existingListener) -> {
            if (existingListener == null || existingListener == newListener) {
                return newListener;
            } else {
                log.error("Cannot add listener as one already exist for {}", deviceId);
                return existingListener;
            }
        });
    }

    @Override
    public void unsetListener(DeviceId deviceId) {
        listeners.remove(deviceId);
    }

    private class InternalMapListener implements EventuallyConsistentMapListener<DeviceId, BigInteger> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId, BigInteger> event) {
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
