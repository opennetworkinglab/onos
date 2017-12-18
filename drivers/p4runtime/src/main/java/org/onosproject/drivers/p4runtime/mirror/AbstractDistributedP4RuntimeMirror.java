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

package org.onosproject.drivers.p4runtime.mirror;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of a distributed P4Runtime mirror, backed by an
 * {@link EventuallyConsistentMap}.
 *
 * @param <H> handle class
 * @param <E> entry class
 */
@Beta
@Component(immediate = true)
public abstract class AbstractDistributedP4RuntimeMirror
        <H extends PiHandle, E extends PiEntity>
        implements P4RuntimeMirror<H, E> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    private EventuallyConsistentMap<H, TimedEntry<E>> mirrorMap;

    @Activate
    public void activate() {
        mirrorMap = storageService
                .<H, TimedEntry<E>>eventuallyConsistentMapBuilder()
                .withName(mapName())
                .withSerializer(storeSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    abstract String mapName();

    abstract KryoNamespace storeSerializer();

    @Deactivate
    public void deactivate() {
        mirrorMap.destroy();
        mirrorMap = null;
        log.info("Stopped");
    }

    @Override
    public Collection<TimedEntry<E>> getAll(DeviceId deviceId) {
        checkNotNull(deviceId);
        return mirrorMap.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public TimedEntry<E> get(H handle) {
        checkNotNull(handle);
        return mirrorMap.get(handle);
    }

    @Override
    public void put(H handle, E entry) {
        checkNotNull(handle);
        checkNotNull(entry);
        final long now = new WallClockTimestamp().unixTimestamp();
        final TimedEntry<E> timedEntry = new TimedEntry<>(now, entry);
        mirrorMap.put(handle, timedEntry);
    }

    @Override
    public void remove(H handle) {
        checkNotNull(handle);
        mirrorMap.remove(handle);
    }

}
