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
import org.onlab.util.KryoNamespace;
import org.onlab.util.SharedExecutors;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.EntityUpdateRequest;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.WriteRequest;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.WriteResponse;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
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
public abstract class AbstractDistributedP4RuntimeMirror
        <H extends PiHandle, E extends PiEntity>
        implements P4RuntimeMirror<H, E> {

    private static final String MAP_NAME_TEMPLATE = "onos-p4runtime-mirror-%s-map";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PiPipeconfWatchdogService pipeconfWatchdogService;

    private EventuallyConsistentMap<PiHandle, TimedEntry<E>> mirrorMap;
    private EventuallyConsistentMap<PiHandle, Annotations> annotationsMap;

    private final PiEntityType entityType;

    private final boolean flushOnPipelineUnknown;

    private final PiPipeconfWatchdogListener pipeconfListener =
            new InternalPipeconfWatchdogListener();

    AbstractDistributedP4RuntimeMirror(PiEntityType entityType) {
        this.entityType = entityType;
        this.flushOnPipelineUnknown = false;
    }

    AbstractDistributedP4RuntimeMirror(PiEntityType entityType,
                                       boolean flushOnPipelineUnknown) {
        this.entityType = entityType;
        this.flushOnPipelineUnknown = flushOnPipelineUnknown;
    }

    /**
     * Returns a string that identifies the map maintained by this store among
     * others that uses this abstract class.
     *
     * @return string
     */
    protected abstract String mapSimpleName();

    @Activate
    public void activate() {
        final String fullMapName = format(MAP_NAME_TEMPLATE, mapSimpleName());
        final KryoNamespace serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(TimedEntry.class)
                .build();

        mirrorMap = storageService
                .<PiHandle, TimedEntry<E>>eventuallyConsistentMapBuilder()
                .withName(fullMapName)
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        annotationsMap = storageService
                .<PiHandle, Annotations>eventuallyConsistentMapBuilder()
                .withName(fullMapName + "-annotations")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        pipeconfWatchdogService.addListener(pipeconfListener);
        log.info("Started");
    }

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
        if (flushOnPipelineUnknown && !status.equals(READY)) {
            // Keep mirror empty if pipeline status is UNKNOWN.
            log.info("Ignoring {} mirror update because pipeline " +
                             "status of {} is {}: {}",
                     entityType, handle.deviceId(), status, entry);
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
    @SuppressWarnings("unchecked")
    public void sync(DeviceId deviceId, Collection<E> entities) {
        checkNotNull(deviceId);
        final Map<PiHandle, E> deviceState = entities.stream()
                .collect(Collectors.toMap(e -> e.handle(deviceId), e -> e));
        final Map<PiHandle, E> localState = deviceHandleMap(deviceId);

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
                    put((H) deviceHandle, entryToAdd);
                    addCount.incrementAndGet();
                });
        // Update or remove local entries.
        localState.keySet().forEach(localHandle -> {
            final E localEntry = localState.get(localHandle);
            final E deviceEntry = deviceState.get(localHandle);
            if (deviceEntry == null) {
                log.debug("Removing mirror entry for {}: {}", deviceId, localEntry);
                remove((H) localHandle);
                removeCount.incrementAndGet();
            } else if (!deviceEntry.equals(localEntry)) {
                log.debug("Updating mirror entry for {}: {}-->{}",
                          deviceId, localEntry, deviceEntry);
                put((H) localHandle, deviceEntry);
                updateCount.incrementAndGet();
            }
        });
        if (removeCount.get() + updateCount.get() + addCount.get() > 0) {
            log.info("Synchronized {} mirror for {}: {} removed, {} updated, {} added",
                     entityType, deviceId, removeCount, updateCount, addCount);
        }
    }

    private Set<PiHandle> getHandlesForDevice(DeviceId deviceId) {
        return mirrorMap.keySet().stream()
                .filter(h -> h.deviceId().equals(deviceId))
                .collect(Collectors.toSet());
    }

    private Map<PiHandle, E> deviceHandleMap(DeviceId deviceId) {
        final Map<PiHandle, E> deviceMap = Maps.newHashMap();
        mirrorMap.entrySet().stream()
                .filter(e -> e.getKey().deviceId().equals(deviceId))
                .forEach(e -> deviceMap.put(e.getKey(), e.getValue().entry()));
        return deviceMap;
    }


    private void removeAll(DeviceId deviceId) {
        checkNotNull(deviceId);
        @SuppressWarnings("unchecked")
        Collection<H> handles = (Collection<H>) getHandlesForDevice(deviceId);
        handles.forEach(this::remove);
    }

    @Override
    public void applyWriteRequest(WriteRequest request) {
        // Optimistically assume all requests will be successful.
        applyUpdates(request.pendingUpdates());
    }

    @Override
    public void applyWriteResponse(WriteResponse response) {
        // Record only successful updates.
        applyUpdates(response.success());
    }

    @SuppressWarnings("unchecked")
    private void applyUpdates(Collection<? extends EntityUpdateRequest> updates) {
        updates.stream()
                .filter(r -> r.entityType().equals(this.entityType))
                .forEach(r -> {
                    switch (r.updateType()) {
                        case INSERT:
                        case MODIFY:
                            put((H) r.handle(), (E) r.entity());
                            break;
                        case DELETE:
                            remove((H) r.handle());
                            break;
                        default:
                            log.error("Unknown update type {}", r.updateType());
                    }
                });
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
            return flushOnPipelineUnknown &&
                    event.type().equals(PiPipeconfWatchdogEvent.Type.PIPELINE_UNKNOWN);
        }
    }
}
