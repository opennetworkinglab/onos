package org.onosproject.store.resource.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.resource.ApplyLabelNumber;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceDelegate;
import org.onosproject.net.resource.LabelResourceEvent;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourcePool;
import org.onosproject.net.resource.LabelResourceRequest;
import org.onosproject.net.resource.LabelResourceStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.SMap;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.slf4j.Logger;

import com.google.common.collect.Multimap;

/**
 * Manages label resources using Hazelcast.
 */
@Component(immediate = true, enabled = true)
@Service
public class HazelcastLabelResourceStore
        extends
        AbstractHazelcastStore<LabelResourceEvent, LabelResourceDelegate>
        implements LabelResourceStore {

    @Override
    public void setDelegate(LabelResourceDelegate delegate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unsetDelegate(LabelResourceDelegate delegate) {
        // TODO Auto-generated method stub

    }

    private final Logger log = getLogger(getClass());

    private static final String POOL_MAP_NAME = "labelresourcepool";

    // primary data:
    // read/write needs to be locked
    private final ReentrantReadWriteLock resourcePoolLock = new ReentrantReadWriteLock();

    private SMap<DeviceId, LabelResourcePool> resourcePool = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    protected static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(LabelResourceEvent.class)
                    .register(LabelResourcePool.class).register(DeviceId.class)
                    .register(LabelResourceRequest.class)
                    .register(LabelResourceRequest.Type.class)
                    .register(LabelResourceEvent.Type.class)
                    .register(DefaultLabelResource.class)
                    .register(LabelResourceId.class).build();
        }
    };

    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

    @Activate
    public void activate() {

        super.activate();
        resourcePool = new SMap<>(
                                  theInstance
                                          .<byte[], byte[]>getMap(POOL_MAP_NAME),
                                  SERIALIZER);

        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_CREATED,
                               new ClusterMessageHandler() {

                                   @Override
                                   public void handle(ClusterMessage message) {
                                       LabelResourcePool operation = SERIALIZER
                                               .decode(message.payload());
                                       log.trace("received get flow entry request for {}",
                                                 operation);
                                       final LabelResourceEvent event = internalCreate(operation);
                                       try {
                                           message.respond(SERIALIZER
                                                   .encode(event));
                                       } catch (IOException e) {
                                           log.error("Failed to create the resource pool",
                                                     e);
                                       }
                                   }
                               });
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_DESTROYED,
                               new ClusterMessageHandler() {

                                   @Override
                                   public void handle(ClusterMessage message) {
                                       DeviceId deviceId = SERIALIZER
                                               .decode(message.payload());
                                       log.trace("received get flow entry request for {}",
                                                 deviceId);
                                       final LabelResourceEvent event = internalDestroy(deviceId);
                                       try {
                                           message.respond(SERIALIZER
                                                   .encode(event));
                                       } catch (IOException e) {
                                           log.error("Failed to destroy the resource pool",
                                                     e);
                                       }
                                   }
                               });
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_APPLE,
                               new ClusterMessageHandler() {

                                   @Override
                                   public void handle(ClusterMessage message) {
                                       LabelResourceRequest request = SERIALIZER
                                               .decode(message.payload());
                                       log.trace("received get flow entry request for {}",
                                                 request);
                                       final Collection<DefaultLabelResource> resource = internalApply(request);
                                       try {
                                           message.respond(SERIALIZER
                                                   .encode(resource));
                                       } catch (IOException e) {
                                           log.error("Failed to apply the label resource",
                                                     e);
                                       }
                                   }
                               });
        clusterCommunicator
                .addSubscriber(LabelResourceMessageSubjects.LABEL_POOL_RELEASE,
                               new ClusterMessageHandler() {

                                   @Override
                                   public void handle(ClusterMessage message) {
                                       LabelResourceRequest request = SERIALIZER
                                               .decode(message.payload());
                                       log.trace("received get flow entry request for {}",
                                                 request);
                                       final boolean isSuccess = internalRelease(request);
                                       try {
                                           message.respond(SERIALIZER
                                                   .encode(isSuccess));
                                       } catch (IOException e) {
                                           log.error("Failed to release the resource pool",
                                                     e);
                                       }
                                   }
                               });
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_CREATED);
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_APPLE);
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_DESTROYED);
        clusterCommunicator
                .removeSubscriber(LabelResourceMessageSubjects.LABEL_POOL_RELEASE);
        log.info("Stopped");
    }

    @Override
    public LabelResourceEvent createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                                     LabelResourceId endLabel) {
        LabelResourcePool pool = new LabelResourcePool(deviceId.toString(),
                                                       beginLabel.getLabelId(), endLabel.getLabelId());
        return this.create(pool);
    }

    @Override
    public LabelResourceEvent createGlobalPool(LabelResourceId beginLabel,
                                     LabelResourceId endLabel) {
        LabelResourcePool pool = new LabelResourcePool("",
                                                       beginLabel.getLabelId(), endLabel.getLabelId());
        return this.create(pool);
    }

    private LabelResourceEvent create(LabelResourcePool labelResourcePool) {
        if (labelResourcePool.getBeginLabel().getLabelId() < 0
                || labelResourcePool.getEndLabel().getLabelId() < 0) {
            log.warn("the value of beginLabel and the value of endLabel must be both positive number.");
            return null;
        }
        if (labelResourcePool.getBeginLabel().getLabelId() > labelResourcePool.getEndLabel().getLabelId()) {
            log.warn("beginLabel must be less than or equal to endLabel.");
            return null;
        }
        Device device = (Device) deviceService.getDevice(labelResourcePool
                .getDeviceId());
        if (device == null) {
            return null;
        }
        LabelResourcePool pool = new LabelResourcePool(labelResourcePool
                .getDeviceId().toString(), labelResourcePool.getBeginLabel().getLabelId(),
                                                       labelResourcePool
                                                               .getEndLabel().getLabelId());

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(pool
                .getDeviceId());

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to getFlowEntries: No master for {}",
                     labelResourcePool);
            return null;
        }

        if (replicaInfo.master().get()
                .equals(clusterService.getLocalNode().id())) {
            return internalCreate(pool);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  replicaInfo.master().orNull(), pool.getDeviceId());

        ClusterMessage message = new ClusterMessage(
                                                    clusterService
                                                            .getLocalNode()
                                                            .id(),
                                                    LabelResourceMessageSubjects.LABEL_POOL_CREATED,
                                                    SERIALIZER.encode(pool));

        try {
            Future<byte[]> responseFuture = clusterCommunicator
                    .sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER
                    .decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS,
                                               TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException
                | InterruptedException e) {
            log.warn("Unable to fetch flow store contents from {}", replicaInfo
                    .master().get());
        }
        return null;
    }

    private LabelResourceEvent internalCreate(LabelResourcePool pool) {
        resourcePoolLock.writeLock().lock();
        LabelResourcePool poolOld = resourcePool.get(pool.getDeviceId());
        if (poolOld == null) {
            resourcePool.put(pool.getDeviceId(), pool);
            resourcePoolLock.writeLock().unlock();
            return new LabelResourceEvent(LabelResourceEvent.Type.POOL_CREATED,
                                          null);
        }
        resourcePoolLock.writeLock().unlock();
        return null;
    }

    @Override
    public LabelResourceEvent destroyDevicePool(DeviceId deviceId) {
        if (deviceId == null || "".equals(deviceId.toString())) {
            log.warn("the value of device is null");
            return null;
        }
        Device device = (Device) deviceService.getDevice(deviceId);
        if (device == null) {
            return null;
        }
        ReplicaInfo replicaInfo = replicaInfoManager
                .getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to getFlowEntries: No master for {}", deviceId);
            return null;
        }

        if (replicaInfo.master().get()
                .equals(clusterService.getLocalNode().id())) {
            return internalDestroy(deviceId);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                                                    clusterService
                                                            .getLocalNode()
                                                            .id(),
                                                    LabelResourceMessageSubjects.LABEL_POOL_DESTROYED,
                                                    SERIALIZER.encode(deviceId));

        try {
            Future<byte[]> responseFuture = clusterCommunicator
                    .sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER
                    .decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS,
                                               TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException
                | InterruptedException e) {
            log.warn("Unable to fetch flow store contents from {}", replicaInfo
                    .master().get());
        }
        return null;
    }

    private LabelResourceEvent internalDestroy(DeviceId deviceId) {
        LabelResourcePool poolOld = resourcePool.get(deviceId);
        if (poolOld != null) {
            resourcePool.remove(deviceId);
        }
        log.info("success to destroy the label resource pool of device id {}",
                 deviceId);
        return new LabelResourceEvent(LabelResourceEvent.Type.POOL_DESTROYED,
                                      null);
    }

    @Override
    public Collection<DefaultLabelResource> applyFromDevicePool(DeviceId deviceId,
                                                  ApplyLabelNumber applyNum) {
        Device device = (Device) deviceService.getDevice(deviceId);
        if (device == null) {
            return null;
        }
        LabelResourceRequest request = new LabelResourceRequest(
                                                                deviceId,
                                                                LabelResourceRequest.Type.APPLY,
                                                                applyNum, null);
        ReplicaInfo replicaInfo = replicaInfoManager
                .getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to getFlowEntries: No master for {}", deviceId);
            return null;
        }

        if (replicaInfo.master().get()
                .equals(clusterService.getLocalNode().id())) {
            return internalApply(request);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                                                    clusterService
                                                            .getLocalNode()
                                                            .id(),
                                                    LabelResourceMessageSubjects.LABEL_POOL_APPLE,
                                                    SERIALIZER.encode(request));

        try {
            Future<byte[]> responseFuture = clusterCommunicator
                    .sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER
                    .decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS,
                                               TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException
                | InterruptedException e) {
            log.warn("Unable to fetch flow store contents from {}", replicaInfo
                    .master().get());
        }
        return null;
    }

    private Collection<DefaultLabelResource> internalApply(LabelResourceRequest request) {
        resourcePoolLock.writeLock().lock();
        DeviceId deviceId = request.getDeviceId();
        long applyNum = request.getApplyNum().getApplyNum();
        LabelResourcePool pool = resourcePool.get(deviceId);
        Collection<DefaultLabelResource> result = new ArrayList<DefaultLabelResource>();
        long freeNum = this.getFreeNumOfDevicePool(deviceId);
        if (applyNum > freeNum) {
            log.info("the free number of the label resource pool of deviceId {} is not enough.");
            resourcePoolLock.writeLock().unlock();
            return Collections.emptyList();
        }
        Queue<DefaultLabelResource> releaseLabels = pool.getReleaseLabelId();
        long tmp = releaseLabels.size() > applyNum ? applyNum : releaseLabels
                .size();
        DefaultLabelResource resource = null;
        for (int i = 0; i < tmp; i++) {
            resource = releaseLabels.poll();
            result.add(resource);
        }
        for (long j = pool.getCurrentUsedMaxLabelId().getLabelId(); j < pool
                .getCurrentUsedMaxLabelId().getLabelId() + applyNum - tmp; j++) {
            resource = new DefaultLabelResource(deviceId,
                                                LabelResourceId
                                                        .labelResourceId(j));
            result.add(resource);
        }
        long current = pool.getCurrentUsedMaxLabelId().getLabelId()
        + applyNum - tmp;
        pool.setCurrentUsedMaxLabelId(LabelResourceId.labelResourceId(current));
        pool.setUsedNum(pool.getUsedNum() + applyNum);
        resourcePool.put(deviceId, pool);
        log.info("success to apply label resource");
        resourcePoolLock.writeLock().unlock();
        return result;
    }

    @Override
    public boolean releaseToDevicePool(Multimap<DeviceId, DefaultLabelResource> release) {
        Map<DeviceId, Collection<DefaultLabelResource>> maps = release.asMap();
        Set<DeviceId> deviceIdSet = maps.keySet();
        LabelResourceRequest request = null;
        for (Iterator<DeviceId> it = deviceIdSet.iterator(); it.hasNext();) {
            DeviceId deviceId = (DeviceId) it.next();
            Device device = (Device) deviceService.getDevice(deviceId);
            if (device == null) {
                continue;
            }
            Collection<DefaultLabelResource> collection = maps.get(deviceId);
            request = new LabelResourceRequest(
                                               deviceId,
                                               LabelResourceRequest.Type.RELEASE,
                                               ApplyLabelNumber.applyLabelNumber(0), collection);
            ReplicaInfo replicaInfo = replicaInfoManager
                    .getReplicaInfoFor(deviceId);

            if (!replicaInfo.master().isPresent()) {
                log.warn("Failed to getFlowEntries: No master for {}", deviceId);
                return false;
            }

            if (replicaInfo.master().get()
                    .equals(clusterService.getLocalNode().id())) {
                return internalRelease(request);
            }

            log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                      replicaInfo.master().orNull(), deviceId);

            ClusterMessage message = new ClusterMessage(
                                                        clusterService
                                                                .getLocalNode()
                                                                .id(),
                                                        LabelResourceMessageSubjects.LABEL_POOL_RELEASE,
                                                        SERIALIZER
                                                                .encode(request));

            try {
                Future<byte[]> responseFuture = clusterCommunicator
                        .sendAndReceive(message, replicaInfo.master().get());
                return SERIALIZER.decode(responseFuture
                        .get(FLOW_RULE_STORE_TIMEOUT_MILLIS,
                             TimeUnit.MILLISECONDS));
            } catch (IOException | TimeoutException | ExecutionException
                    | InterruptedException e) {
                log.warn("Unable to fetch flow store contents from {}",
                         replicaInfo.master().get());
            }
        }
        return false;
    }

    private boolean internalRelease(LabelResourceRequest request) {
        resourcePoolLock.writeLock().lock();
        DeviceId deviceId = request.getDeviceId();
        Collection<DefaultLabelResource> release = request
                .getReleaseCollection();
        LabelResourcePool pool = resourcePool.get(deviceId);
        if (pool == null) {
            resourcePoolLock.writeLock().unlock();
            log.info("the label resource pool of device id {} does not exist");
            return false;
        }
        Queue<DefaultLabelResource> queue = pool.getReleaseLabelId();
        DefaultLabelResource labelResource = null;
        for (Iterator<DefaultLabelResource> it = release.iterator(); it
                .hasNext();) {
            labelResource = it.next();
            if (pool.getCurrentUsedMaxLabelId().getLabelId() > labelResource
                    .getLabelResourceId().getLabelId()
                    || !queue.contains(labelResource)) {
                queue.offer(labelResource);
            }
        }
        pool.setReleaseLabelId(queue);
        pool.setUsedNum(pool.getUsedNum() - release.size());
        resourcePool.put(deviceId, pool);
        log.info("success to release label resource");
        resourcePoolLock.writeLock().unlock();
        return true;
    }

    @Override
    public boolean isDevicePoolFull(DeviceId deviceId) {
        LabelResourcePool pool = resourcePool.get(deviceId);
        if (pool == null) {
            return true;
        }
        return pool.getCurrentUsedMaxLabelId() == pool.getEndLabel()
                && pool.getReleaseLabelId().size() == 0 ? true : false;
    }

    @Override
    public long getFreeNumOfDevicePool(DeviceId deviceId) {
        LabelResourcePool pool = resourcePool.get(deviceId);
        if (pool == null) {
            return 0;
        }
        return pool.getEndLabel().getLabelId() - pool.getCurrentUsedMaxLabelId().getLabelId()
                + pool.getReleaseLabelId().size();
    }

    @Override
    public boolean hasDelegate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return resourcePool.get(deviceId);
    }

    @Override
    public LabelResourceEvent destroyGlobalPool() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<DefaultLabelResource> applyFromGlobalPool(ApplyLabelNumber applyNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseToGlobalPool(Set<DefaultLabelResource> release) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isGlobalPoolFull() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getFreeNumOfGlobalPool() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public LabelResourcePool getGlobalLabelResourcePool() {
        // TODO Auto-generated method stub
        return null;
    }


}
