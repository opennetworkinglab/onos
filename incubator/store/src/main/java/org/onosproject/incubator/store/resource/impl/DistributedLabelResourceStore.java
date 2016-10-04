/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.store.resource.impl;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceDelegate;
import org.onosproject.incubator.net.resource.label.LabelResourceEvent;
import org.onosproject.incubator.net.resource.label.LabelResourceEvent.Type;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.incubator.net.resource.label.LabelResourceRequest;
import org.onosproject.incubator.net.resource.label.LabelResourceStore;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Manages label resources using copycat.
 */
@Component(immediate = true, enabled = true)
@Service
public class DistributedLabelResourceStore
        extends AbstractStore<LabelResourceEvent, LabelResourceDelegate>
        implements LabelResourceStore {
    private final Logger log = getLogger(getClass());

    private static final String POOL_MAP_NAME = "onos-label-resource-pool";

    private static final String GLOBAL_RESOURCE_POOL_DEVICE_ID = "global_resource_pool_device_id";

    private ConsistentMap<DeviceId, LabelResourcePool> resourcePool = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ExecutorService messageHandlingExecutor;
    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 8;
    private static final long PEER_REQUEST_TIMEOUT_MS = 5000;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(LabelResourceEvent.class)
                    .register(LabelResourcePool.class)
                    .register(LabelResourceRequest.class)
                    .register(LabelResourceRequest.Type.class)
                    .register(LabelResourceEvent.Type.class)
                    .register(DefaultLabelResource.class)
                    .register(LabelResourceId.class)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID).build());

    @Activate
    public void activate() {

        resourcePool = storageService
                .<DeviceId, LabelResourcePool>consistentMapBuilder()
                .withName(POOL_MAP_NAME).withSerializer(SERIALIZER).build();
        messageHandlingExecutor = Executors
                .newFixedThreadPool(MESSAGE_HANDLER_THREAD_POOL_SIZE,
                                    groupedThreads("onos/store/flow",
                                                   "message-handlers",
                                                   log));
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_CREATED,
                        SERIALIZER::<LabelResourcePool>decode,
                        operation -> {
                            log.trace("received get flow entry request for {}", operation);
                            return internalCreate(operation);
                        },
                        SERIALIZER::<Boolean>encode,
                        messageHandlingExecutor);
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_DESTROYED,
                        SERIALIZER::<DeviceId>decode,
                        deviceId -> {
                            log.trace("received get flow entry request for {}", deviceId);
                            return internalDestroy(deviceId);
                        },
                        SERIALIZER::<Boolean>encode,
                        messageHandlingExecutor);
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_APPLY,
                        SERIALIZER::<LabelResourceRequest>decode,
                        request -> {
                            log.trace("received get flow entry request for {}", request);
                            return internalApply(request);

                        },
                        SERIALIZER::<Collection<LabelResource>>encode,
                        messageHandlingExecutor);
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_RELEASE,
                        SERIALIZER::<LabelResourceRequest>decode,
                        request -> {
                            log.trace("received get flow entry request for {}",
                                    request);
                            return internalRelease(request);
                        },
                        SERIALIZER::<Boolean>encode,
                        messageHandlingExecutor);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_CREATED);
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_APPLY);
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_DESTROYED);
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_RELEASE);
        messageHandlingExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public boolean createDevicePool(DeviceId deviceId,
                                    LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
        LabelResourcePool pool = new LabelResourcePool(deviceId.toString(),
                                                       beginLabel.labelId(),
                                                       endLabel.labelId());
        return this.create(pool);
    }

    @Override
    public boolean createGlobalPool(LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
        LabelResourcePool pool = new LabelResourcePool(GLOBAL_RESOURCE_POOL_DEVICE_ID,
                                                       beginLabel.labelId(),
                                                       endLabel.labelId());
        return this.internalCreate(pool);
    }

    private boolean create(LabelResourcePool pool) {
        Device device = deviceService.getDevice(pool.deviceId());
        if (device == null) {
            return false;
        }

        NodeId master = mastershipService.getMasterFor(pool.deviceId());

        if (master == null) {
            log.warn("Failed to create label resource pool: No master for {}", pool);
            return false;
        }

        if (master.equals(clusterService.getLocalNode().id())) {
            return internalCreate(pool);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  master, pool.deviceId());

        return complete(clusterCommunicator
                .sendAndReceive(pool,
                                LabelResourceMessageSubjects.LABEL_POOL_CREATED,
                                SERIALIZER::encode, SERIALIZER::decode,
                                master));
    }

    private boolean internalCreate(LabelResourcePool pool) {
        Versioned<LabelResourcePool> poolOld = resourcePool
                .get(pool.deviceId());
        if (poolOld == null) {
            resourcePool.put(pool.deviceId(), pool);
            LabelResourceEvent event = new LabelResourceEvent(Type.POOL_CREATED,
                                                              pool);
            notifyDelegate(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean destroyDevicePool(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            return false;
        }

        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.warn("Failed to destroyDevicePool. No master for {}", deviceId);
            return false;
        }

        if (master.equals(clusterService.getLocalNode().id())) {
            return internalDestroy(deviceId);
        }

        log.trace("Forwarding request to {}, which is the primary (master) for device {}",
                  master, deviceId);

        return complete(clusterCommunicator
                .sendAndReceive(deviceId,
                                LabelResourceMessageSubjects.LABEL_POOL_DESTROYED,
                                SERIALIZER::encode, SERIALIZER::decode,
                                master));
    }

    private boolean internalDestroy(DeviceId deviceId) {
        Versioned<LabelResourcePool> poolOld = resourcePool.get(deviceId);
        if (poolOld != null) {
            resourcePool.remove(deviceId);
            LabelResourceEvent event = new LabelResourceEvent(Type.POOL_DESTROYED,
                                                              poolOld.value());
            notifyDelegate(event);
        }
        log.info("success to destroy the label resource pool of device id {}",
                 deviceId);
        return true;
    }

    @Override
    public Collection<LabelResource> applyFromDevicePool(DeviceId deviceId,
                                                         long applyNum) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }
        LabelResourceRequest request = new LabelResourceRequest(deviceId,
                                                                LabelResourceRequest.Type.APPLY,
                                                                applyNum, null);
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.warn("Failed to applyFromDevicePool: No master for {}", deviceId);
            return Collections.emptyList();
        }

        if (master.equals(clusterService.getLocalNode().id())) {
            return internalApply(request);
        }

        log.trace("Forwarding request to {}, which is the primary (master) for device {}",
                  master, deviceId);

        return complete(clusterCommunicator
                .sendAndReceive(request,
                                LabelResourceMessageSubjects.LABEL_POOL_APPLY,
                                SERIALIZER::encode, SERIALIZER::decode,
                                master));
    }

    private Collection<LabelResource> internalApply(LabelResourceRequest request) {
        DeviceId deviceId = request.deviceId();
        long applyNum = request.applyNum();
        Versioned<LabelResourcePool> poolOld = resourcePool.get(deviceId);
        if (poolOld == null) {
            log.info("label resource pool not allocated for deviceId {}.", deviceId);
            return Collections.emptyList();
        }
        LabelResourcePool pool = poolOld.value();
        Collection<LabelResource> result = new HashSet<>();
        long freeNum = this.getFreeNumOfDevicePool(deviceId);
        if (applyNum > freeNum) {
            log.info("the free number of the label resource pool of deviceId {} is not enough.");
            return Collections.emptyList();
        }
        Set<LabelResource> releaseLabels = new HashSet<>(pool.releaseLabelId());
        long tmp = releaseLabels.size() > applyNum ? applyNum : releaseLabels
                .size();
        LabelResource resource = null;
        for (int i = 0; i < tmp; i++) {
            Iterator<LabelResource> it = releaseLabels.iterator();
            if (it.hasNext()) {
                resource = it.next();
                releaseLabels.remove(resource);
            }
            result.add(resource);
        }
        for (long j = pool.currentUsedMaxLabelId().labelId(); j < pool
                .currentUsedMaxLabelId().labelId() + applyNum - tmp; j++) {
            resource = new DefaultLabelResource(deviceId,
                                                LabelResourceId
                                                        .labelResourceId(j));
            result.add(resource);
        }
        long beginLabel = pool.beginLabel().labelId();
        long endLabel = pool.endLabel().labelId();
        long totalNum = pool.totalNum();
        long current = pool.currentUsedMaxLabelId().labelId() + applyNum - tmp;
        long usedNum = pool.usedNum() + applyNum;
        ImmutableSet<LabelResource> freeLabel = ImmutableSet
                .copyOf(releaseLabels);
        LabelResourcePool newPool = new LabelResourcePool(deviceId.toString(),
                                                          beginLabel, endLabel,
                                                          totalNum, usedNum,
                                                          current, freeLabel);
        resourcePool.put(deviceId, newPool);
        log.info("success to apply label resource");
        return result;
    }

    @Override
    public boolean releaseToDevicePool(Multimap<DeviceId, LabelResource> release) {
        Map<DeviceId, Collection<LabelResource>> maps = release.asMap();
        Set<DeviceId> deviceIdSet = maps.keySet();
        LabelResourceRequest request = null;
        for (Iterator<DeviceId> it = deviceIdSet.iterator(); it.hasNext();) {
            DeviceId deviceId = it.next();
            Device device = deviceService.getDevice(deviceId);
            if (device == null) {
                continue;
            }
            ImmutableSet<LabelResource> collection = ImmutableSet.copyOf(maps
                    .get(deviceId));
            request = new LabelResourceRequest(deviceId,
                                               LabelResourceRequest.Type.RELEASE,
                                               0, collection);
            NodeId master = mastershipService.getMasterFor(deviceId);

            if (master == null) {
                log.warn("Failed to releaseToDevicePool: No master for {}", deviceId);
                return false;
            }

            if (master.equals(clusterService.getLocalNode().id())) {
                return internalRelease(request);
            }

            log.trace("Forwarding request to {}, which is the primary (master) for device {}",
                      master, deviceId);

            return complete(clusterCommunicator
                    .sendAndReceive(request,
                                    LabelResourceMessageSubjects.LABEL_POOL_RELEASE,
                                    SERIALIZER::encode, SERIALIZER::decode,
                                    master));
        }
        return false;
    }

    private boolean internalRelease(LabelResourceRequest request) {
        DeviceId deviceId = request.deviceId();
        Collection<LabelResource> release = request.releaseCollection();
        Versioned<LabelResourcePool> poolOld = resourcePool.get(deviceId);
        if (poolOld == null) {
            log.info("the label resource pool of device id {} not allocated");
            return false;
        }
        LabelResourcePool pool = poolOld.value();
        if (pool == null) {
            log.info("the label resource pool of device id {} does not exist");
            return false;
        }
        Set<LabelResource> storeSet = new HashSet<>(pool.releaseLabelId());
        LabelResource labelResource = null;
        long realReleasedNum = 0;
        for (Iterator<LabelResource> it = release.iterator(); it.hasNext();) {
            labelResource = it.next();
            if (labelResource.labelResourceId().labelId() < pool.beginLabel()
                    .labelId()
                    || labelResource.labelResourceId().labelId() > pool
                            .endLabel().labelId()) {
                continue;
            }
            if (pool.currentUsedMaxLabelId().labelId() > labelResource
                    .labelResourceId().labelId()
                    || !storeSet.contains(labelResource)) {
                storeSet.add(labelResource);
                realReleasedNum++;
            }
        }
        long beginNum = pool.beginLabel().labelId();
        long endNum = pool.endLabel().labelId();
        long totalNum = pool.totalNum();
        long usedNum = pool.usedNum() - realReleasedNum;
        long current = pool.currentUsedMaxLabelId().labelId();
        ImmutableSet<LabelResource> s = ImmutableSet.copyOf(storeSet);
        LabelResourcePool newPool = new LabelResourcePool(deviceId.toString(),
                                                          beginNum, endNum,
                                                          totalNum, usedNum,
                                                          current, s);
        resourcePool.put(deviceId, newPool);
        log.info("success to release label resource");
        return true;
    }

    @Override
    public boolean isDevicePoolFull(DeviceId deviceId) {
        Versioned<LabelResourcePool> pool = resourcePool.get(deviceId);
        if (pool == null) {
            return true;
        }
        return pool.value().currentUsedMaxLabelId() == pool.value().endLabel()
                && pool.value().releaseLabelId().size() == 0 ? true : false;
    }

    @Override
    public long getFreeNumOfDevicePool(DeviceId deviceId) {
        Versioned<LabelResourcePool> pool = resourcePool.get(deviceId);
        if (pool == null) {
            return 0;
        }
        return pool.value().endLabel().labelId()
                - pool.value().currentUsedMaxLabelId().labelId()
                + pool.value().releaseLabelId().size();
    }

    @Override
    public LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId) {
        Versioned<LabelResourcePool> pool = resourcePool.get(deviceId);
        return pool == null ? null : pool.value();
    }

    @Override
    public boolean destroyGlobalPool() {
        return this.internalDestroy(DeviceId
                .deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID));
    }

    @Override
    public Collection<LabelResource> applyFromGlobalPool(long applyNum) {
        LabelResourceRequest request = new LabelResourceRequest(DeviceId.deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID),
                                                                LabelResourceRequest.Type.APPLY,
                                                                applyNum, null);
        return this.internalApply(request);
    }

    @Override
    public boolean releaseToGlobalPool(Set<LabelResourceId> release) {
        Set<LabelResource> set = new HashSet<>();
        DefaultLabelResource resource = null;
        for (LabelResourceId labelResource : release) {
            resource = new DefaultLabelResource(DeviceId.deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID),
                                                labelResource);
            set.add(resource);
        }
        LabelResourceRequest request = new LabelResourceRequest(DeviceId.deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID),
                                                                LabelResourceRequest.Type.RELEASE,
                                                                0,
                                                                ImmutableSet.copyOf(set));
        return this.internalRelease(request);
    }

    @Override
    public boolean isGlobalPoolFull() {
        return this.isDevicePoolFull(DeviceId
                .deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID));
    }

    @Override
    public long getFreeNumOfGlobalPool() {
        return this.getFreeNumOfDevicePool(DeviceId
                .deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID));
    }

    @Override
    public LabelResourcePool getGlobalLabelResourcePool() {
        return this.getDeviceLabelResourcePool(DeviceId
                .deviceId(GLOBAL_RESOURCE_POOL_DEVICE_ID));
    }

    private <T> T complete(Future<T> future) {
        try {
            return future.get(PEER_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for operation to complete.", e);
            return null;
        } catch (TimeoutException | ExecutionException e) {
            log.error("Failed remote operation", e);
            return null;
        }
    }
}
