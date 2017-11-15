/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.store.pi.impl;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfDeviceMappingEvent;
import org.onosproject.net.pi.service.PiPipeconfMappingStore;
import org.onosproject.net.pi.service.PiPipeconfMappingStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages information of pipeconf to device binding using gossip protocol to distribute
 * information.
 */
@Component(immediate = true)
@Service
public class DistributedDevicePipeconfMappingStore
        extends AbstractStore<PiPipeconfDeviceMappingEvent, PiPipeconfMappingStoreDelegate>
        implements PiPipeconfMappingStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    protected EventuallyConsistentMap<DeviceId, PiPipeconfId> deviceToPipeconf;

    protected final EventuallyConsistentMapListener<DeviceId, PiPipeconfId> pipeconfListener =
            new InternalPiPipeconfListener();

    protected ConcurrentMap<PiPipeconfId, Set<DeviceId>> pipeconfToDevices = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class);
        deviceToPipeconf = storageService.<DeviceId, PiPipeconfId>eventuallyConsistentMapBuilder()
                .withName("onos-pipeconf-table")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        deviceToPipeconf.addListener(pipeconfListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceToPipeconf.removeListener(pipeconfListener);
        deviceToPipeconf = null;
        pipeconfToDevices = null;
        log.info("Stopped");
    }

    @Override
    public PiPipeconfId getPipeconfId(DeviceId deviceId) {
        return deviceToPipeconf.get(deviceId);
    }

    @Override
    public Set<DeviceId> getDevices(PiPipeconfId pipeconfId) {
        return pipeconfToDevices.get(pipeconfId);
    }

    @Override
    public void createOrUpdateBinding(DeviceId deviceId, PiPipeconfId pipeconfId) {
        deviceToPipeconf.put(deviceId, pipeconfId);
    }

    @Override
    public void removeBinding(DeviceId deviceId) {
        deviceToPipeconf.remove(deviceId);
    }

    private class InternalPiPipeconfListener implements EventuallyConsistentMapListener<DeviceId, PiPipeconfId> {

        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId, PiPipeconfId> mapEvent) {
            final PiPipeconfDeviceMappingEvent.Type type;
            final DeviceId deviceId = mapEvent.key();
            final PiPipeconfId pipeconfId = mapEvent.value();
            switch (mapEvent.type()) {
                case PUT:
                    type = PiPipeconfDeviceMappingEvent.Type.CREATED;
                    pipeconfToDevices.compute(pipeconfId, (pipeconf, devices) -> {
                        if (devices == null) {
                            devices = Sets.newConcurrentHashSet();
                        }
                        devices.add(deviceId);
                        return devices;
                    });
                    break;
                case REMOVE:
                    type = PiPipeconfDeviceMappingEvent.Type.REMOVED;
                    pipeconfToDevices.computeIfPresent(pipeconfId, (pipeconf, devices) -> {
                        devices.remove(deviceId);
                        return devices;
                    });
                    break;
                default:
                    throw new IllegalArgumentException("Wrong event type " + mapEvent.type());
            }
            notifyDelegate(new PiPipeconfDeviceMappingEvent(type, deviceId));
        }
    }
}
