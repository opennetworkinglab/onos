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
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtap.Type;
import org.onosproject.openstackvtap.api.OpenstackVtapEvent;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
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
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.store.service.Versioned.valueOrNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of openstack vtap and openstack vtap network using a {@code ConsistentMap}.
 */
@Component(immediate = true, service = OpenstackVtapStore.class)
public class DistributedOpenstackVtapStore
        extends AbstractStore<OpenstackVtapEvent, OpenstackVtapStoreDelegate>
        implements OpenstackVtapStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ConsistentMap<OpenstackVtapId, DefaultOpenstackVtap> vtapConsistentMap;
    private MapEventListener<OpenstackVtapId, DefaultOpenstackVtap> vtapListener =
            new VtapEventListener();
    private Map<OpenstackVtapId, DefaultOpenstackVtap> vtapMap;

    private ConsistentMap<Integer, DefaultOpenstackVtapNetwork> vtapNetworkConsistentMap;
    private MapEventListener<Integer, DefaultOpenstackVtapNetwork> vtapNetworkListener =
            new VtapNetworkEventListener();
    private Map<Integer, DefaultOpenstackVtapNetwork> vtapNetworkMap;

    private ConsistentMap<Integer, Set<DeviceId>> vtapNetworkDevicesConsistentMap;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(OpenstackVtapId.class)
                    .register(UUID.class)
                    .register(DefaultOpenstackVtap.class)
                    .register(Type.class)
                    .register(DefaultOpenstackVtapCriterion.class)
                    .register(DefaultOpenstackVtapNetwork.class)
                    .register(OpenstackVtapNetwork.Mode.class)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                    .build());

    private Map<DeviceId, Set<OpenstackVtapId>>
                                    vtapIdsByTxDeviceId = Maps.newConcurrentMap();
    private Map<DeviceId, Set<OpenstackVtapId>>
                                    vtapIdsByRxDeviceId = Maps.newConcurrentMap();

    private ScheduledExecutorService eventExecutor;
    private Consumer<Status> vtapStatusListener;

    private static final String ERR_NOT_FOUND = "ID {} does not exist";
    private static final String ERR_DUPLICATE = "ID {} already exists";

    @Activate
    public void activate() {
        eventExecutor = newSingleThreadScheduledExecutor(
                groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

        // vtap network data
        vtapNetworkConsistentMap = storageService.<Integer, DefaultOpenstackVtapNetwork>
                consistentMapBuilder()
                .withName("vtapNetworkMap")
                .withSerializer(SERIALIZER)
                .build();
        vtapNetworkMap = vtapNetworkConsistentMap.asJavaMap();
        vtapNetworkConsistentMap.addListener(vtapNetworkListener);

        // vtap network devices data
        vtapNetworkDevicesConsistentMap = storageService.<Integer, Set<DeviceId>>
                consistentMapBuilder()
                .withName("vtapNetworkDevicesMap")
                .withSerializer(SERIALIZER)
                .build();

        // vtap data
        vtapConsistentMap = storageService.<OpenstackVtapId, DefaultOpenstackVtap>
                consistentMapBuilder()
                .withName("vtapMap")
                .withSerializer(SERIALIZER)
                .build();
        vtapMap = vtapConsistentMap.asJavaMap();
        vtapConsistentMap.addListener(vtapListener);

        // initialize vtap data
        vtapStatusListener = status -> {
            if (status == Status.ACTIVE) {
                eventExecutor.execute(this::loadVtapIds);
            }
        };
        vtapConsistentMap.addStatusChangeListener(vtapStatusListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vtapConsistentMap.removeStatusChangeListener(vtapStatusListener);
        vtapConsistentMap.removeListener(vtapListener);
        vtapNetworkConsistentMap.removeListener(vtapNetworkListener);

        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private boolean shouldUpdateVtapNetwork(DefaultOpenstackVtapNetwork existing,
                                            OpenstackVtapNetwork description) {
        if (existing == null) {
            return true;
        }

        if (!Objects.equals(existing.mode(), description.mode()) ||
                !Objects.equals(existing.networkId(), description.networkId()) ||
                !Objects.equals(existing.serverIp(), description.serverIp())) {
            return true;
        }

        // check to see if any of the annotations provided by description
        // differ from those in the existing vtap network
        return description.annotations().keys().stream()
                .anyMatch(k -> !Objects.equals(description.annotations().value(k),
                        existing.annotations().value(k)));
    }

    private OpenstackVtapNetwork createOrUpdateVtapNetwork(boolean update,
                                                           Integer key,
                                                           OpenstackVtapNetwork description) {
        DefaultOpenstackVtapNetwork result =
                vtapNetworkMap.compute(key, (id, existing) -> {
                    // Check create or update validity
                    if (update && existing == null) {
                        return null;
                    } else if (!update && existing != null) {
                        return existing;
                    }

                    if (shouldUpdateVtapNetwork(existing, description)) {
                        // Replace or add annotations
                        final SparseAnnotations annotations;
                        if (existing != null) {
                            annotations = merge((DefaultAnnotations) existing.annotations(),
                                    (SparseAnnotations) description.annotations());
                        } else {
                            annotations = (SparseAnnotations) description.annotations();
                        }

                        return DefaultOpenstackVtapNetwork.builder(description)
                                .annotations(annotations)
                                .build();
                    }
                    return existing;
                });
        return result;
    }

    @Override
    public OpenstackVtapNetwork createVtapNetwork(Integer key, OpenstackVtapNetwork description) {
        if (getVtapNetwork(key) == null) {
            OpenstackVtapNetwork vtapNetwork = createOrUpdateVtapNetwork(false, key, description);
            if (Objects.equals(vtapNetwork, description)) {
                return vtapNetwork;
            }
        }
        log.error(ERR_DUPLICATE, key);
        return null;
    }

    @Override
    public OpenstackVtapNetwork updateVtapNetwork(Integer key, OpenstackVtapNetwork description) {
        OpenstackVtapNetwork vtapNetwork = createOrUpdateVtapNetwork(true, key, description);
        if (vtapNetwork == null) {
            log.error(ERR_NOT_FOUND, key);
        }
        return vtapNetwork;
    }

    @Override
    public OpenstackVtapNetwork removeVtapNetwork(Integer key) {
        return vtapNetworkMap.remove(key);
    }

    @Override
    public void clearVtapNetworks() {
        vtapNetworkMap.clear();
    }

    @Override
    public int getVtapNetworkCount() {
        return vtapNetworkMap.size();
    }

    @Override
    public OpenstackVtapNetwork getVtapNetwork(Integer key) {
        return vtapNetworkMap.get(key);
    }

    @Override
    public boolean addDeviceToVtapNetwork(Integer key, DeviceId deviceId) {
        Versioned<Set<DeviceId>> result =
                vtapNetworkDevicesConsistentMap.compute(key, (id, existing) -> {
                    // Add deviceId to deviceIds
                    if (existing == null) {
                        return Sets.newHashSet(deviceId);
                    } else if (!existing.contains(deviceId)) {
                        Set<DeviceId> deviceIds = Sets.newHashSet(existing);
                        deviceIds.add(deviceId);
                        return deviceIds;
                    } else {
                        return existing;
                    }
                });
        return Objects.nonNull(valueOrNull(result));
    }

    @Override
    public boolean removeDeviceFromVtapNetwork(Integer key, DeviceId deviceId) {
        Versioned<Set<DeviceId>> result =
                vtapNetworkDevicesConsistentMap.compute(key, (id, existing) -> {
                    // Remove deviceId from deviceIds
                    if (existing != null && existing.contains(deviceId)) {
                        Set<DeviceId> deviceIds = Sets.newHashSet(existing);
                        deviceIds.remove(deviceId);
                        return deviceIds;
                    } else {
                        return existing;
                    }
                });
        return Objects.nonNull(valueOrNull(result));
    }

    @Override
    public Set<DeviceId> getVtapNetworkDevices(Integer key) {
        return valueOrNull(vtapNetworkDevicesConsistentMap.get(key));
    }

    private boolean shouldUpdateVtap(DefaultOpenstackVtap existing,
                                     OpenstackVtap description,
                                     boolean replaceDevices) {
        if (existing == null) {
            return true;
        }

        if (!Objects.equals(existing.type(), description.type()) ||
                !Objects.equals(existing.vtapCriterion(), description.vtapCriterion())) {
            return true;
        }

        if (replaceDevices) {
            if (!Objects.equals(description.txDeviceIds(), existing.txDeviceIds()) ||
                    !Objects.equals(description.rxDeviceIds(), existing.rxDeviceIds())) {
                return true;
            }
        } else {
            if (!existing.txDeviceIds().containsAll(description.txDeviceIds()) ||
                    !existing.rxDeviceIds().containsAll(description.rxDeviceIds())) {
                return true;
            }
        }

        // check to see if any of the annotations provided by description
        // differ from those in the existing vtap
        return description.annotations().keys().stream()
                .anyMatch(k -> !Objects.equals(description.annotations().value(k),
                        existing.annotations().value(k)));
    }

    private OpenstackVtap createOrUpdateVtap(boolean update,
                                             OpenstackVtap description,
                                             boolean replaceDevices) {
        DefaultOpenstackVtap result =
                vtapMap.compute(description.id(), (id, existing) -> {
                    // Check create or update validity
                    if (update && existing == null) {
                        return null;
                    } else if (!update && existing != null) {
                        return existing;
                    }

                    if (shouldUpdateVtap(existing, description, replaceDevices)) {
                        // Replace or add devices
                        final Set<DeviceId> txDeviceIds;
                        if (existing == null || replaceDevices) {
                            txDeviceIds = description.txDeviceIds();
                        } else {
                            txDeviceIds = Sets.newHashSet(existing.txDeviceIds());
                            txDeviceIds.addAll(description.txDeviceIds());
                        }

                        final Set<DeviceId> rxDeviceIds;
                        if (existing == null || replaceDevices) {
                            rxDeviceIds = description.rxDeviceIds();
                        } else {
                            rxDeviceIds = Sets.newHashSet(existing.rxDeviceIds());
                            rxDeviceIds.addAll(description.rxDeviceIds());
                        }

                        // Replace or add annotations
                        final SparseAnnotations annotations;
                        if (existing != null) {
                            annotations = merge((DefaultAnnotations) existing.annotations(),
                                    (SparseAnnotations) description.annotations());
                        } else {
                            annotations = (SparseAnnotations) description.annotations();
                        }

                        return DefaultOpenstackVtap.builder(description)
                                .txDeviceIds(txDeviceIds)
                                .rxDeviceIds(rxDeviceIds)
                                .annotations(annotations)
                                .build();
                    }
                    return existing;
                });
        return result;
    }

    @Override
    public OpenstackVtap createVtap(OpenstackVtap description) {
        if (getVtap(description.id()) == null) {
            OpenstackVtap vtap = createOrUpdateVtap(false, description, true);
            if (Objects.equals(vtap, description)) {
                return vtap;
            }
        }
        log.error(ERR_DUPLICATE, description.id());
        return null;
    }

    @Override
    public OpenstackVtap updateVtap(OpenstackVtap description, boolean replaceDevices) {
        OpenstackVtap vtap = createOrUpdateVtap(true, description, replaceDevices);
        if (vtap == null) {
            log.error(ERR_NOT_FOUND, description.id());
        }
        return vtap;
    }

    @Override
    public OpenstackVtap removeVtap(OpenstackVtapId vtapId) {
        return vtapMap.remove(vtapId);
    }

    @Override
    public void clearVtaps() {
        vtapMap.clear();
    }

    @Override
    public int getVtapCount(Type type) {
        return (int) vtapMap.values().parallelStream()
                .filter(vtap -> vtap.type().isValid(type))
                .count();
    }

    @Override
    public Set<OpenstackVtap> getVtaps(Type type) {
        return ImmutableSet.copyOf(
                vtapMap.values().parallelStream()
                        .filter(vtap -> vtap.type().isValid(type))
                        .collect(Collectors.toSet()));
    }

    @Override
    public OpenstackVtap getVtap(OpenstackVtapId vtapId) {
        return vtapMap.get(vtapId);
    }

    @Override
    public boolean addDeviceToVtap(OpenstackVtapId vtapId, Type type, DeviceId deviceId) {
        OpenstackVtap result =
                vtapMap.compute(vtapId, (id, existing) -> {
                    if (existing == null) {
                        return null;
                    }

                    // Check type validate
                    if (!existing.type().isValid(type)) {
                        log.error("Not valid OpenstackVtap type {} for requested type {}", existing.type(), type);
                        return existing;
                    }

                    // Add deviceId to txDeviceIds
                    final Set<DeviceId> txDeviceIds;
                    if (existing.type().isValid(Type.VTAP_TX) &&
                            (type.isValid(Type.VTAP_TX) || type == Type.VTAP_ANY) &&
                            !existing.txDeviceIds().contains(deviceId)) {
                        txDeviceIds = Sets.newHashSet(existing.txDeviceIds());
                        txDeviceIds.add(deviceId);
                    } else {
                        txDeviceIds = null;
                    }

                    // Add deviceId to rxDeviceIds
                    final Set<DeviceId> rxDeviceIds;
                    if (existing.type().isValid(Type.VTAP_RX) &&
                            (type.isValid(Type.VTAP_RX) || type == Type.VTAP_ANY) &&
                            !existing.rxDeviceIds().contains(deviceId)) {
                        rxDeviceIds = Sets.newHashSet(existing.rxDeviceIds());
                        rxDeviceIds.add(deviceId);
                    } else {
                        rxDeviceIds = null;
                    }

                    if (txDeviceIds != null || rxDeviceIds != null) {
                        return DefaultOpenstackVtap.builder()
                                .id(id)
                                .type(existing.type())
                                .vtapCriterion(existing.vtapCriterion())
                                .txDeviceIds(txDeviceIds != null ? txDeviceIds : existing.txDeviceIds())
                                .rxDeviceIds(rxDeviceIds != null ? rxDeviceIds : existing.rxDeviceIds())
                                .annotations(existing.annotations())
                                .build();
                    } else {
                        return existing;
                    }
                });
        return Objects.nonNull(result);
    }

    @Override
    public boolean removeDeviceFromVtap(OpenstackVtapId vtapId, OpenstackVtap.Type type, DeviceId deviceId) {
        OpenstackVtap result =
                vtapMap.compute(vtapId, (id, existing) -> {
                    if (existing == null) {
                        return null;
                    }

                    // Check type validate
                    if (!existing.type().isValid(type)) {
                        log.error("Not valid OpenstackVtap type {} for requested type {}",
                                existing.type(), type);
                        return existing;
                    }

                    // Remove deviceId from txDeviceIds
                    final Set<DeviceId> txDeviceIds;
                    if (existing.type().isValid(Type.VTAP_TX) &&
                            (type.isValid(Type.VTAP_TX) || type == Type.VTAP_ANY) &&
                            existing.txDeviceIds().contains(deviceId)) {
                        txDeviceIds = Sets.newHashSet(existing.txDeviceIds());
                        txDeviceIds.remove(deviceId);
                    } else {
                        txDeviceIds = null;
                    }

                    // Remove deviceId from rxDeviceIds
                    final Set<DeviceId> rxDeviceIds;
                    if (existing.type().isValid(Type.VTAP_RX) &&
                            (type.isValid(Type.VTAP_RX) || type == Type.VTAP_ANY) &&
                            existing.rxDeviceIds().contains(deviceId)) {
                        rxDeviceIds = Sets.newHashSet(existing.rxDeviceIds());
                        rxDeviceIds.remove(deviceId);
                    } else {
                        rxDeviceIds = null;
                    }

                    if (txDeviceIds != null || rxDeviceIds != null) {
                        return DefaultOpenstackVtap.builder()
                                .id(id)
                                .type(existing.type())
                                .vtapCriterion(existing.vtapCriterion())
                                .txDeviceIds(txDeviceIds != null ? txDeviceIds : existing.txDeviceIds())
                                .rxDeviceIds(rxDeviceIds != null ? rxDeviceIds : existing.rxDeviceIds())
                                .annotations(existing.annotations())
                                .build();
                    } else {
                        return existing;
                    }
                });
        return Objects.nonNull(result);
    }

    @Override
    public Set<OpenstackVtap> getVtapsByDeviceId(DeviceId deviceId) {
        Set<OpenstackVtapId> vtapIds = Sets.newHashSet();
        Set<OpenstackVtapId> txIds = vtapIdsByTxDeviceId.get(deviceId);
        if (txIds != null) {
            vtapIds.addAll(txIds);
        }
        Set<OpenstackVtapId> rxIds = vtapIdsByRxDeviceId.get(deviceId);
        if (rxIds != null) {
            vtapIds.addAll(rxIds);
        }
        return ImmutableSet.copyOf(
                vtapIds.parallelStream()
                        .map(vtapId -> vtapMap.get(vtapId))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
    }

    private class VtapComparator implements Comparator<OpenstackVtap> {
        @Override
        public int compare(OpenstackVtap v1, OpenstackVtap v2) {
            int diff = (v2.type().compareTo(v1.type()));
            if (diff == 0) {
                return (v2.vtapCriterion().ipProtocol() - v1.vtapCriterion().ipProtocol());
            }
            return diff;
        }
    }

    private void loadVtapIds() {
        vtapIdsByTxDeviceId.clear();
        vtapIdsByRxDeviceId.clear();
        vtapMap.values().forEach(vtap -> refreshDeviceIdsByVtap(null, vtap));
    }

    private static Set<OpenstackVtapId> addVTapIds(OpenstackVtapId vtapId) {
        Set<OpenstackVtapId> vtapIds = Sets.newConcurrentHashSet();
        vtapIds.add(vtapId);
        return vtapIds;
    }

    private static Set<OpenstackVtapId> updateVTapIds(Set<OpenstackVtapId> existingVtapIds,
                                                      OpenstackVtapId vtapId) {
        existingVtapIds.add(vtapId);
        return existingVtapIds;
    }

    private static Set<OpenstackVtapId> removeVTapIds(Set<OpenstackVtapId> existingVtapIds,
                                                      OpenstackVtapId vtapId) {
        existingVtapIds.remove(vtapId);
        if (existingVtapIds.isEmpty()) {
            return null;
        }
        return existingVtapIds;
    }

    private void updateVTapIdFromTxDeviceId(OpenstackVtapId vtapId, DeviceId deviceId) {
        vtapIdsByTxDeviceId.compute(deviceId, (k, v) -> v == null ?
                addVTapIds(vtapId) : updateVTapIds(v, vtapId));
    }

    private void removeVTapIdFromTxDeviceId(OpenstackVtapId vtapId, DeviceId deviceId) {
        vtapIdsByTxDeviceId.computeIfPresent(deviceId, (k, v) -> removeVTapIds(v, vtapId));
    }

    private void updateVTapIdFromRxDeviceId(OpenstackVtapId vtapId, DeviceId deviceId) {
        vtapIdsByRxDeviceId.compute(deviceId, (k, v) -> v == null ?
                addVTapIds(vtapId) : updateVTapIds(v, vtapId));
    }

    private void removeVTapIdFromRxDeviceId(OpenstackVtapId vtapId, DeviceId deviceId) {
        vtapIdsByRxDeviceId.computeIfPresent(deviceId, (k, v) -> removeVTapIds(v, vtapId));
    }

    private void refreshDeviceIdsByVtap(OpenstackVtap newOpenstackVtap,
                                        OpenstackVtap oldOpenstackVtap) {
        if (Objects.equals(newOpenstackVtap, oldOpenstackVtap)) {
            return;
        }

        if (oldOpenstackVtap != null) {
            Set<DeviceId> removeDeviceIds;

            // Remove TX vtap
            removeDeviceIds = (newOpenstackVtap != null) ?
                    Sets.difference(oldOpenstackVtap.txDeviceIds(),
                            newOpenstackVtap.txDeviceIds()) : oldOpenstackVtap.txDeviceIds();
            removeDeviceIds.forEach(id -> removeVTapIdFromTxDeviceId(oldOpenstackVtap.id(), id));

            // Remove RX vtap
            removeDeviceIds = (newOpenstackVtap != null) ?
                    Sets.difference(oldOpenstackVtap.rxDeviceIds(),
                            newOpenstackVtap.rxDeviceIds()) : oldOpenstackVtap.rxDeviceIds();
            removeDeviceIds.forEach(id -> removeVTapIdFromRxDeviceId(oldOpenstackVtap.id(), id));
        }

        if (newOpenstackVtap != null) {
            Set<DeviceId> addDeviceIds;

            // Add TX vtap
            addDeviceIds = (oldOpenstackVtap != null) ?
                    Sets.difference(newOpenstackVtap.txDeviceIds(),
                            oldOpenstackVtap.txDeviceIds()) : newOpenstackVtap.txDeviceIds();
            addDeviceIds.forEach(id -> updateVTapIdFromTxDeviceId(newOpenstackVtap.id(), id));

            // Add RX vtap
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

            log.trace("VtapEventListener {}: {} -> {}", event.type(), oldValue, newValue);
            switch (event.type()) {
                case INSERT:
                    refreshDeviceIdsByVtap(newValue, oldValue);
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_ADDED, newValue, null));
                    break;

                case UPDATE:
                    refreshDeviceIdsByVtap(newValue, oldValue);
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_UPDATED, newValue, oldValue));
                    break;

                case REMOVE:
                    refreshDeviceIdsByVtap(newValue, oldValue);
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_REMOVED, null, oldValue));
                    break;

                default:
                    log.warn("Unknown map event type: {}", event.type());
            }
        }
    }

    private class VtapNetworkEventListener
            implements MapEventListener<Integer, DefaultOpenstackVtapNetwork> {
        @Override
        public void event(MapEvent<Integer, DefaultOpenstackVtapNetwork> event) {
            DefaultOpenstackVtapNetwork newValue =
                    event.newValue() != null ? event.newValue().value() : null;
            DefaultOpenstackVtapNetwork oldValue =
                    event.oldValue() != null ? event.oldValue().value() : null;

            log.trace("VtapNetworkEventListener {}: {} -> {}", event.type(), oldValue, newValue);
            switch (event.type()) {
                case INSERT:
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_NETWORK_ADDED, newValue, null));
                    break;

                case UPDATE:
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_NETWORK_UPDATED, newValue, oldValue));
                    break;

                case REMOVE:
                    notifyDelegate(new OpenstackVtapEvent(
                            OpenstackVtapEvent.Type.VTAP_NETWORK_REMOVED, null, oldValue));
                    break;

                default:
                    log.warn("Unknown map event type: {}", event.type());
            }
        }
    }

}
