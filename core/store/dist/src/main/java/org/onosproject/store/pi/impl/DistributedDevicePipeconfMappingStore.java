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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfDeviceMappingEvent;
import org.onosproject.net.pi.service.PiPipeconfMappingStore;
import org.onosproject.net.pi.service.PiPipeconfMappingStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages information of pipeconf to device binding.
 */
@Component(immediate = true, service = PiPipeconfMappingStore.class)
public class DistributedDevicePipeconfMappingStore
        extends AbstractStore<PiPipeconfDeviceMappingEvent, PiPipeconfMappingStoreDelegate>
        implements PiPipeconfMappingStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    protected ConsistentMap<DeviceId, PiPipeconfId> deviceToPipeconf;

    protected final MapEventListener<DeviceId, PiPipeconfId> mapListener =
            new InternalPiPipeconfListener();

    protected SetMultimap<PiPipeconfId, DeviceId> pipeconfToDevices =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());

    @Activate
    public void activate() {
        deviceToPipeconf = storageService.<DeviceId, PiPipeconfId>consistentMapBuilder()
                .withName("onos-pipeconf-table")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();
        deviceToPipeconf.addListener(mapListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceToPipeconf.removeListener(mapListener);
        deviceToPipeconf = null;
        pipeconfToDevices = null;
        log.info("Stopped");
    }

    @Override
    public PiPipeconfId getPipeconfId(DeviceId deviceId) {
        if (!deviceToPipeconf.containsKey(deviceId)) {
            return null;
        }
        return deviceToPipeconf.get(deviceId).value();
    }

    @Override
    public Set<DeviceId> getDevices(PiPipeconfId pipeconfId) {
        return ImmutableSet.copyOf(pipeconfToDevices.get(pipeconfId));
    }

    @Override
    public void createOrUpdateBinding(DeviceId deviceId, PiPipeconfId pipeconfId) {
        deviceToPipeconf.put(deviceId, pipeconfId);
    }

    @Override
    public void removeBinding(DeviceId deviceId) {
        deviceToPipeconf.remove(deviceId);
    }

    private class InternalPiPipeconfListener implements MapEventListener<DeviceId, PiPipeconfId> {

        @Override
        public void event(MapEvent<DeviceId, PiPipeconfId> mapEvent) {
            PiPipeconfDeviceMappingEvent.Type eventType = null;
            final DeviceId deviceId = mapEvent.key();
            final PiPipeconfId newPipeconfId = mapEvent.newValue() != null
                    ? mapEvent.newValue().value() : null;
            final PiPipeconfId oldPipeconfId = mapEvent.oldValue() != null
                    ? mapEvent.oldValue().value() : null;
            switch (mapEvent.type()) {
                case INSERT:
                case UPDATE:
                    if (newPipeconfId != null) {
                        if (!newPipeconfId.equals(oldPipeconfId)) {
                            eventType = PiPipeconfDeviceMappingEvent.Type.CREATED;
                        }
                        pipeconfToDevices.put(newPipeconfId, deviceId);
                    }
                    break;
                case REMOVE:
                    if (oldPipeconfId != null) {
                        eventType = PiPipeconfDeviceMappingEvent.Type.REMOVED;
                        pipeconfToDevices.remove(oldPipeconfId, deviceId);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Wrong event type " + mapEvent.type());
            }
            if (eventType != null) {
                notifyDelegate(new PiPipeconfDeviceMappingEvent(eventType, deviceId));
            }
        }
    }
}
