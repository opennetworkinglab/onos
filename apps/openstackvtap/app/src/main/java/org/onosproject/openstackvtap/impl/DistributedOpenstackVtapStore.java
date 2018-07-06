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
package org.onosproject.openstackvtap.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapEvent;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapStore;
import org.onosproject.openstackvtap.api.OpenstackVtapStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedPrimitive.Status;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of users using a {@code ConsistentMap}.
 */
@Component(immediate = true)
@Service
public class DistributedOpenstackVtapStore
        extends AbstractStore<OpenstackVtapEvent, OpenstackVtapStoreDelegate>
        implements OpenstackVtapStore {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<OpenstackVtapId, DefaultOpenstackVtap> vTapConsistentMap;
    private MapEventListener<OpenstackVtapId, DefaultOpenstackVtap>
                                            vTapListener = new VtapEventListener();
    private Map<OpenstackVtapId, DefaultOpenstackVtap> vTapMap;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(OpenstackVtapId.class)
                    .register(UUID.class)
                    .register(DefaultOpenstackVtap.class)
                    .register(OpenstackVtap.Type.class)
                    .register(DefaultOpenstackVtapCriterion.class)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                    .build());

    private Map<DeviceId, Set<OpenstackVtapId>>
                                    vTapIdsByTxDeviceId = Maps.newConcurrentMap();
    private Map<DeviceId, Set<OpenstackVtapId>>
                                    vTapIdsByRxDeviceId = Maps.newConcurrentMap();

    private ScheduledExecutorService eventExecutor;

    private Consumer<Status> vTapStatusListener;

    public static final String INVALID_DESCRIPTION = "Invalid create/update parameter";

    @Activate
    public void activate() {
        eventExecutor = newSingleThreadScheduledExecutor(
                groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

        vTapConsistentMap = storageService.<OpenstackVtapId, DefaultOpenstackVtap>
                consistentMapBuilder()
                .withName("vTapMap")
                .withSerializer(SERIALIZER)
                .build();

        vTapMap = vTapConsistentMap.asJavaMap();
        vTapConsistentMap.addListener(vTapListener);

        vTapStatusListener = status -> {
            if (status == Status.ACTIVE) {
                eventExecutor.execute(this::loadVtapIds);
            }
        };
        vTapConsistentMap.addStatusChangeListener(vTapStatusListener);

        log.info("Started {} - {}", this.getClass().getSimpleName());
    }

    @Deactivate
    public void deactivate() {
        vTapConsistentMap.removeStatusChangeListener(vTapStatusListener);
        vTapConsistentMap.removeListener(vTapListener);
        eventExecutor.shutdown();

        log.info("Stopped {} - {}", this.getClass().getSimpleName());
    }

    @Override
    public OpenstackVtap createOrUpdateVtap(OpenstackVtapId vTapId,
                                            OpenstackVtap description,
                                            boolean replaceFlag) {

        return vTapMap.compute(vTapId, (id, existing) -> {
            if (existing == null &&
                    (description.type() == null ||
                     description.vTapCriterion() == null ||
                     description.txDeviceIds() == null ||
                     description.rxDeviceIds() == null)) {
                checkState(false, INVALID_DESCRIPTION);
                return null;
            }

            if (shouldUpdate(existing, description, replaceFlag)) {
                // Replace items
                OpenstackVtap.Type type =
                        (description.type() == null ? existing.type() : description.type());
                OpenstackVtapCriterion vTapCriterion =
                        (description.vTapCriterion() == null ?
                        existing.vTapCriterion() : description.vTapCriterion());

                // Replace or add devices
                Set<DeviceId> txDeviceIds;
                if (description.txDeviceIds() == null) {
                    txDeviceIds = existing.txDeviceIds();
                } else {
                    if (existing == null || replaceFlag) {
                        txDeviceIds = ImmutableSet.copyOf(description.txDeviceIds());
                    } else {
                        txDeviceIds = Sets.newHashSet(existing.txDeviceIds());
                        txDeviceIds.addAll(description.txDeviceIds());
                    }
                }

                Set<DeviceId> rxDeviceIds;
                if (description.rxDeviceIds() == null) {
                    rxDeviceIds = existing.rxDeviceIds();
                } else {
                    if (existing == null || replaceFlag) {
                        rxDeviceIds = ImmutableSet.copyOf(description.rxDeviceIds());
                    } else {
                        rxDeviceIds = Sets.newHashSet(existing.rxDeviceIds());
                        rxDeviceIds.addAll(description.rxDeviceIds());
                    }
                }

                // Replace or add annotations
                SparseAnnotations annotations;
                if (existing != null) {
                    annotations = merge((DefaultAnnotations) existing.annotations(),
                            (SparseAnnotations) description.annotations());
                } else {
                    annotations = (SparseAnnotations) description.annotations();
                }

                // Make new changed vTap and return
                return DefaultOpenstackVtap.builder()
                        .id(vTapId)
                        .type(type)
                        .vTapCriterion(vTapCriterion)
                        .txDeviceIds(txDeviceIds)
                        .rxDeviceIds(rxDeviceIds)
                        .annotations(annotations)
                        .build();
            }
            return existing;
        });
    }

    @Override
    public OpenstackVtap removeVtapById(OpenstackVtapId vTapId) {
        return vTapMap.remove(vTapId);
    }

    @Override
    public boolean addDeviceToVtap(OpenstackVtapId vTapId,
                                   OpenstackVtap.Type type,
                                   DeviceId deviceId) {
        checkNotNull(vTapId);
        checkNotNull(deviceId);

        OpenstackVtap vTap = vTapMap.compute(vTapId, (id, existing) -> {
            if (existing == null) {
                return null;
            }
            if (!existing.type().isValid(type)) {
                log.error("Not valid OpenstackVtap type {} for requested type {}",
                        existing.type(), type);
                return existing;
            }

            Set<DeviceId> txDeviceIds = null;
            if (type.isValid(OpenstackVtap.Type.VTAP_TX) &&
                    !existing.txDeviceIds().contains(deviceId)) {
                txDeviceIds = Sets.newHashSet(existing.txDeviceIds());
                txDeviceIds.add(deviceId);
            }

            Set<DeviceId> rxDeviceIds = null;
            if (type.isValid(OpenstackVtap.Type.VTAP_RX) &&
                    !existing.rxDeviceIds().contains(deviceId)) {
                rxDeviceIds = Sets.newHashSet(existing.rxDeviceIds());
                rxDeviceIds.add(deviceId);
            }

            if (txDeviceIds != null || rxDeviceIds != null) {
                //updateVTapIdFromDeviceId(existing.id(), deviceId);    // execute from event listener

                return DefaultOpenstackVtap.builder()
                        .id(vTapId)
                        .type(existing.type())
                        .vTapCriterion(existing.vTapCriterion())
                        .txDeviceIds(txDeviceIds != null ? txDeviceIds : existing.txDeviceIds())
                        .rxDeviceIds(rxDeviceIds != null ? rxDeviceIds : existing.rxDeviceIds())
                        .annotations(existing.annotations())
                        .build();
            }
            return existing;
        });
        return (vTap != null);
    }

    @Override
    public boolean removeDeviceFromVtap(OpenstackVtapId vTapId,
                                        OpenstackVtap.Type type,
                                        DeviceId deviceId) {
        checkNotNull(vTapId);
        checkNotNull(deviceId);

        OpenstackVtap vTap = vTapMap.compute(vTapId, (id, existing) -> {
            if (existing == null) {
                return null;
            }
            if (!existing.type().isValid(type)) {
                log.error("Not valid OpenstackVtap type {} for requested type {}",
                        existing.type(), type);
                return existing;
            }

            Set<DeviceId> txDeviceIds = null;
            if (type.isValid(OpenstackVtap.Type.VTAP_TX) &&
                    existing.txDeviceIds().contains(deviceId)) {
                txDeviceIds = Sets.newHashSet(existing.txDeviceIds());
                txDeviceIds.remove(deviceId);
            }

            Set<DeviceId> rxDeviceIds = null;
            if (type.isValid(OpenstackVtap.Type.VTAP_RX) &&
                    existing.rxDeviceIds().contains(deviceId)) {
                rxDeviceIds = Sets.newHashSet(existing.rxDeviceIds());
                rxDeviceIds.remove(deviceId);
            }

            if (txDeviceIds != null || rxDeviceIds != null) {
                //removeVTapIdFromDeviceId(existing.id(), deviceId);    // execute from event listener

                return DefaultOpenstackVtap.builder()
                        .id(vTapId)
                        .type(existing.type())
                        .vTapCriterion(existing.vTapCriterion())
                        .txDeviceIds(txDeviceIds != null ? txDeviceIds : existing.txDeviceIds())
                        .rxDeviceIds(rxDeviceIds != null ? rxDeviceIds : existing.rxDeviceIds())
                        .annotations(existing.annotations())
                        .build();
            }
            return existing;
        });
        return (vTap != null);
    }

    @Override
    public boolean updateDeviceForVtap(OpenstackVtapId vTapId,
                                       Set<DeviceId> txDeviceIds, Set<DeviceId> rxDeviceIds,
                                       boolean replaceDevices) {
        checkNotNull(vTapId);
        checkNotNull(txDeviceIds);
        checkNotNull(rxDeviceIds);

        OpenstackVtap vTap = vTapMap.compute(vTapId, (id, existing) -> {
            if (existing == null) {
                return null;
            }

            // Replace or add devices
            Set<DeviceId> txDS = null;
            if (replaceDevices) {
                if (!existing.txDeviceIds().equals(txDeviceIds)) {
                    txDS = ImmutableSet.copyOf(txDeviceIds);
                }
            } else {
                if (!existing.txDeviceIds().containsAll(txDeviceIds)) {
                    txDS = Sets.newHashSet(existing.txDeviceIds());
                    txDS.addAll(txDeviceIds);
                }
            }

            Set<DeviceId> rxDS = null;
            if (replaceDevices) {
                if (!existing.rxDeviceIds().equals(rxDeviceIds)) {
                    rxDS = ImmutableSet.copyOf(rxDeviceIds);
                }
            } else {
                if (!existing.rxDeviceIds().containsAll(rxDeviceIds)) {
                    rxDS = Sets.newHashSet(existing.rxDeviceIds());
                    rxDS.addAll(rxDeviceIds);
                }
            }

            if (txDS != null || rxDS != null) {

                return DefaultOpenstackVtap.builder()
                        .id(vTapId)
                        .type(existing.type())
                        .vTapCriterion(existing.vTapCriterion())
                        .txDeviceIds(txDS != null ? txDS : existing.txDeviceIds())
                        .rxDeviceIds(rxDS != null ? rxDS : existing.rxDeviceIds())
                        .annotations(existing.annotations())
                        .build();
            }
            return existing;
        });
        return (vTap != null);
    }

    @Override
    public int getVtapCount(OpenstackVtap.Type type) {
        return (int) vTapMap.values().parallelStream()
                .filter(vTap -> vTap.type().isValid(type))
                .count();
    }

    @Override
    public Set<OpenstackVtap> getVtaps(OpenstackVtap.Type type) {
        return ImmutableSet.copyOf(
                vTapMap.values().parallelStream()
                        .filter(vTap -> vTap.type().isValid(type))
                        .collect(Collectors.toSet()));
    }

    @Override
    public OpenstackVtap getVtap(OpenstackVtapId vTapId) {
        return vTapMap.get(vTapId);
    }

    @Override
    public Set<OpenstackVtap> getVtapsByDeviceId(OpenstackVtap.Type type,
                                                 DeviceId deviceId) {
        Set<OpenstackVtapId> vtapIds = Sets.newHashSet();
        if (type.isValid(OpenstackVtap.Type.VTAP_TX)) {
            if (vTapIdsByTxDeviceId.get(deviceId) != null) {
                vtapIds.addAll(vTapIdsByTxDeviceId.get(deviceId));
            }
        }
        if (type.isValid(OpenstackVtap.Type.VTAP_RX)) {
            if (vTapIdsByRxDeviceId.get(deviceId) != null) {
                vtapIds.addAll(vTapIdsByRxDeviceId.get(deviceId));
            }
        }

        return ImmutableSet.copyOf(
                vtapIds.parallelStream()
                        .map(vTapId -> vTapMap.get(vTapId))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
    }

    private void loadVtapIds() {
        vTapIdsByTxDeviceId.clear();
        vTapIdsByRxDeviceId.clear();
        vTapMap.values().forEach(vTap -> refreshDeviceIdsByVtap(null, vTap));
    }

    private boolean shouldUpdate(DefaultOpenstackVtap existing,
                                 OpenstackVtap description,
                                 boolean replaceDevices) {
        if (existing == null) {
            return true;
        }

        if ((description.type() != null && !description.type().equals(existing.type()))
                || (description.vTapCriterion() != null &&
                !description.vTapCriterion().equals(existing.vTapCriterion()))) {
            return true;
        }

        if (description.txDeviceIds() != null) {
            if (replaceDevices) {
                if (!existing.txDeviceIds().equals(description.txDeviceIds())) {
                    return true;
                }
            } else {
                if (!existing.txDeviceIds().containsAll(description.txDeviceIds())) {
                    return true;
                }
            }
        }

        if (description.rxDeviceIds() != null) {
            if (replaceDevices) {
                if (!existing.rxDeviceIds().equals(description.rxDeviceIds())) {
                    return true;
                }
            } else {
                if (!existing.rxDeviceIds().containsAll(description.rxDeviceIds())) {
                    return true;
                }
            }
        }

        // check to see if any of the annotations provided by vTap
        // differ from those in the existing vTap
        return description.annotations().keys().stream()
                .anyMatch(k -> !Objects.equals(description.annotations().value(k),
                        existing.annotations().value(k)));
    }

    private class VtapComparator implements Comparator<OpenstackVtap> {
        @Override
        public int compare(OpenstackVtap v1, OpenstackVtap v2) {
            int diff = (v2.type().compareTo(v1.type()));
            if (diff == 0) {
                return (v2.vTapCriterion().ipProtocol() - v1.vTapCriterion().ipProtocol());
            }
            return diff;
        }
    }

    private static Set<OpenstackVtapId> addVTapIds(OpenstackVtapId vTapId) {
        Set<OpenstackVtapId> vtapIds = Sets.newConcurrentHashSet();
        vtapIds.add(vTapId);
        return vtapIds;
    }

    private static Set<OpenstackVtapId> updateVTapIds(Set<OpenstackVtapId> existingVtapIds,
                                                      OpenstackVtapId vTapId) {
        existingVtapIds.add(vTapId);
        return existingVtapIds;
    }

    private static Set<OpenstackVtapId> removeVTapIds(Set<OpenstackVtapId> existingVtapIds,
                                                      OpenstackVtapId vTapId) {
        existingVtapIds.remove(vTapId);
        if (existingVtapIds.isEmpty()) {
            return null;
        }
        return existingVtapIds;
    }

    private void updateVTapIdFromTxDeviceId(OpenstackVtapId vTapId, DeviceId deviceId) {
        vTapIdsByTxDeviceId.compute(deviceId, (k, v) -> v == null ?
                addVTapIds(vTapId) : updateVTapIds(v, vTapId));
    }

    private void removeVTapIdFromTxDeviceId(OpenstackVtapId vTapId, DeviceId deviceId) {
        vTapIdsByTxDeviceId.computeIfPresent(deviceId, (k, v) -> removeVTapIds(v, vTapId));
    }

    private void updateVTapIdFromRxDeviceId(OpenstackVtapId vTapId, DeviceId deviceId) {
        vTapIdsByRxDeviceId.compute(deviceId, (k, v) -> v == null ?
                addVTapIds(vTapId) : updateVTapIds(v, vTapId));
    }

    private void removeVTapIdFromRxDeviceId(OpenstackVtapId vTapId, DeviceId deviceId) {
        vTapIdsByRxDeviceId.computeIfPresent(deviceId, (k, v) -> removeVTapIds(v, vTapId));
    }

    private void refreshDeviceIdsByVtap(OpenstackVtap oldOpenstackVtap,
                                        OpenstackVtap newOpenstackVtap) {
        if (oldOpenstackVtap != null) {
            Set<DeviceId> removeDeviceIds;

            // Remove TX vTap
            removeDeviceIds = (newOpenstackVtap != null) ?
                    Sets.difference(oldOpenstackVtap.txDeviceIds(),
                            newOpenstackVtap.txDeviceIds()) : oldOpenstackVtap.txDeviceIds();
            removeDeviceIds.forEach(id -> removeVTapIdFromTxDeviceId(oldOpenstackVtap.id(), id));

            // Remove RX vTap
            removeDeviceIds = (newOpenstackVtap != null) ?
                    Sets.difference(oldOpenstackVtap.rxDeviceIds(),
                            newOpenstackVtap.rxDeviceIds()) : oldOpenstackVtap.rxDeviceIds();
            removeDeviceIds.forEach(id -> removeVTapIdFromRxDeviceId(oldOpenstackVtap.id(), id));
        }

        if (newOpenstackVtap != null) {
            Set<DeviceId> addDeviceIds;

            // Add TX vTap
            addDeviceIds = (oldOpenstackVtap != null) ?
                    Sets.difference(newOpenstackVtap.txDeviceIds(),
                            oldOpenstackVtap.txDeviceIds()) : newOpenstackVtap.txDeviceIds();
            addDeviceIds.forEach(id -> updateVTapIdFromTxDeviceId(newOpenstackVtap.id(), id));

            // Add RX vTap
            addDeviceIds = (oldOpenstackVtap != null) ?
                    Sets.difference(newOpenstackVtap.rxDeviceIds(),
                            oldOpenstackVtap.rxDeviceIds()) : newOpenstackVtap.rxDeviceIds();
            addDeviceIds.forEach(id -> updateVTapIdFromRxDeviceId(newOpenstackVtap.id(), id));
        }
    }

    private class VtapEventListener
            implements MapEventListener<OpenstackVtapId, DefaultOpenstackVtap> {
        @Override
        public void event(MapEvent<OpenstackVtapId, DefaultOpenstackVtap> event) {
            DefaultOpenstackVtap newValue =
                    event.newValue() != null ? event.newValue().value() : null;
            DefaultOpenstackVtap oldValue =
                    event.oldValue() != null ? event.oldValue().value() : null;

            log.debug("VtapEventListener {} -> {}, {}", event.type(), oldValue, newValue);
            switch (event.type()) {
                case INSERT:
                    refreshDeviceIdsByVtap(oldValue, newValue);
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_ADDED, newValue));
                    break;

                case UPDATE:
                    if (!Objects.equals(newValue, oldValue)) {
                        refreshDeviceIdsByVtap(oldValue, newValue);
                        notifyDelegate(new OpenstackVtapEvent(
                                OpenstackVtapEvent.Type.VTAP_UPDATED, newValue, oldValue));
                    }
                    break;

                case REMOVE:
                    refreshDeviceIdsByVtap(oldValue, newValue);
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_REMOVED, oldValue));
                    break;

                default:
                    log.warn("Unknown map event type: {}", event.type());
            }
        }
    }
}
