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
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SharedExecutors;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.pi.service.PiPipeconfWatchdogService.PipelineStatus.READY;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfWatchdogService pipeconfWatchdogService;

    private EventuallyConsistentMap<H, TimedEntry<E>> mirrorMap;

    private EventuallyConsistentMap<H, Annotations> annotationsMap;

    private final PiPipeconfWatchdogListener pipeconfListener =
            new InternalPipeconfWatchdogListener();

    @Activate
    public void activate() {
        mirrorMap = storageService
                .<H, TimedEntry<E>>eventuallyConsistentMapBuilder()
                .withName(mapName())
                .withSerializer(storeSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        annotationsMap = storageService
                .<H, Annotations>eventuallyConsistentMapBuilder()
                .withName(mapName() + "-annotations")
                .withSerializer(storeSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        pipeconfWatchdogService.addListener(pipeconfListener);
        log.info("Started");
    }

    abstract String mapName();

    abstract KryoNamespace storeSerializer();

    @Deactivate
    public void deactivate() {
        pipeconfWatchdogService.removeListener(pipeconfListener);
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
        final PiPipeconfWatchdogService.PipelineStatus status =
                pipeconfWatchdogService.getStatus(handle.deviceId());
        if (!status.equals(READY)) {
            log.info("Ignoring device mirror update because pipeline " +
                             "status of {} is {}: {}",
                     handle.deviceId(), status, entry);
            return;
        }
        final long now = new WallClockTimestamp().unixTimestamp();
        final TimedEntry<E> timedEntry = new TimedEntry<>(now, entry);
        mirrorMap.put(handle, timedEntry);
    }

    @Override
    public void remove(H handle) {
        checkNotNull(handle);
        mirrorMap.remove(handle);
        annotationsMap.remove(handle);
    }

    @Override
    public void putAnnotations(H handle, Annotations annotations) {
        checkNotNull(handle);
        checkNotNull(annotations);
        annotationsMap.put(handle, annotations);
    }

    @Override
    public Annotations annotations(H handle) {
        checkNotNull(handle);
        return annotationsMap.get(handle);
    }

    @Override
    public void sync(DeviceId deviceId, Map<H, E> deviceState) {
        checkNotNull(deviceId);
        final Map<H, E> localState = getMirrorMapForDevice(deviceId);

        final AtomicInteger removeCount = new AtomicInteger(0);
        final AtomicInteger updateCount = new AtomicInteger(0);
        final AtomicInteger addCount = new AtomicInteger(0);
        // Add missing entries.
        deviceState.keySet().stream()
                .filter(deviceHandle -> !localState.containsKey(deviceHandle))
                .forEach(deviceHandle -> {
                    final E entryToAdd = deviceState.get(deviceHandle);
                    log.debug("Adding mirror entry for {}: {}",
                              deviceId, entryToAdd);
                    put(deviceHandle, entryToAdd);
                    addCount.incrementAndGet();
                });
        // Update or remove local entries.
        localState.keySet().forEach(localHandle -> {
            final E localEntry = localState.get(localHandle);
            final E deviceEntry = deviceState.get(localHandle);
            if (deviceEntry == null) {
                log.debug("Removing mirror entry for {}: {}", deviceId, localEntry);
                remove(localHandle);
                removeCount.incrementAndGet();
            } else if (!deviceEntry.equals(localEntry)) {
                log.debug("Updating mirror entry for {}: {}-->{}",
                          deviceId, localEntry, deviceEntry);
                put(localHandle, deviceEntry);
                updateCount.incrementAndGet();
            }
        });
        if (removeCount.get() + updateCount.get() + addCount.get() > 0) {
            log.info("Synchronized mirror entries for {}: {} removed, {} updated, {} added",
                     deviceId, removeCount, updateCount, addCount);
        }
    }

    private Set<H> getHandlesForDevice(DeviceId deviceId) {
        return mirrorMap.keySet().stream()
                .filter(h -> h.deviceId().equals(deviceId))
                .collect(Collectors.toSet());
    }

    private Map<H, E> getMirrorMapForDevice(DeviceId deviceId) {
        final Map<H, E> deviceMap = Maps.newHashMap();
        mirrorMap.entrySet().stream()
                .filter(e -> e.getKey().deviceId().equals(deviceId))
                .forEach(e -> deviceMap.put(e.getKey(), e.getValue().entry()));
        return deviceMap;
    }

    private void removeAll(DeviceId deviceId) {
        checkNotNull(deviceId);
        Collection<H> handles = getHandlesForDevice(deviceId);
        handles.forEach(this::remove);
    }

    public class InternalPipeconfWatchdogListener implements PiPipeconfWatchdogListener {
        @Override
        public void event(PiPipeconfWatchdogEvent event) {
            log.debug("Flushing mirror for {}, pipeline status is {}",
                      event.subject(), event.type());
            SharedExecutors.getPoolThreadExecutor().execute(
                    () -> removeAll(event.subject()));
        }

        @Override
        public boolean isRelevant(PiPipeconfWatchdogEvent event) {
            return event.type().equals(PiPipeconfWatchdogEvent.Type.PIPELINE_UNKNOWN);
        }
    }
}
