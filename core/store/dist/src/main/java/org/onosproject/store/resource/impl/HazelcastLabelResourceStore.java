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
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;

import com.google.common.collect.Multimap;
import com.hazelcast.core.IMap;

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

    private IMap<DeviceId, LabelResourcePool> resourcePool = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    protected static final KryoSerializer SERIALIZER = new KryoSerializer();
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

    @Activate
    public void activate() {

        super.activate();
        resourcePool = theInstance.getMap(POOL_MAP_NAME);

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
    public LabelResourceEvent create(DeviceId deviceId, long beginLabel,
                                     long endLabel) {
        LabelResourcePool pool = new LabelResourcePool(deviceId.toString(),
                                                       beginLabel, endLabel);
        return this.create(pool);
    }

    @Override
    public LabelResourceEvent create(LabelResourcePool labelResourcePool) {
        if (labelResourcePool.getBeginLabel() < 0
                || labelResourcePool.getEndLabel() < 0) {
            log.warn("the value of beginLabel and the value of endLabel must be both positive number.");
            return null;
        }
        if (labelResourcePool.getBeginLabel() > labelResourcePool.getEndLabel()) {
            log.warn("beginLabel must be less than or equal to endLabel.");
            return null;
        }

        LabelResourcePool pool = new LabelResourcePool(labelResourcePool
                .getDeviceId().toString(), labelResourcePool.getBeginLabel(),
                                                       labelResourcePool
                                                               .getEndLabel());

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
    public LabelResourceEvent destroy(DeviceId deviceId) {
        if (deviceId == null || "".equals(deviceId.toString())) {
            log.warn("the value of device is null");
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
        poolOld = null;
        log.info("success to destroy the label resource pool of device id {}",
                 deviceId);
        return new LabelResourceEvent(LabelResourceEvent.Type.POOL_DESTROYED,
                                      null);
    }

    @Override
    public Collection<DefaultLabelResource> apply(DeviceId deviceId,
                                                  long applyNum) {
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
        long applyNum = request.getApplyNum();
        LabelResourcePool pool = resourcePool.get(deviceId);
        Collection<DefaultLabelResource> result = new ArrayList<DefaultLabelResource>();
        long freeNum = this.getFreeNum(deviceId);
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
        for (long j = pool.getCurrentUsedMaxLabelId(); j < pool
                .getCurrentUsedMaxLabelId() + 1 + applyNum - tmp; j++) {
            resource = new DefaultLabelResource(deviceId,
                                                LabelResourceId
                                                        .labelResourceId(j));
            result.add(resource);
        }
        pool.setCurrentUsedMaxLabelId(pool.getCurrentUsedMaxLabelId()
                + applyNum - tmp);
        log.info("success to apply label resource");
        resourcePoolLock.writeLock().unlock();
        return result;
    }

    @Override
    public boolean release(Multimap<DeviceId, DefaultLabelResource> release) {
        Map<DeviceId, Collection<DefaultLabelResource>> maps = release.asMap();
        Set<DeviceId> deviceIdSet = maps.keySet();
        LabelResourceRequest request = null;
        for (Iterator<DeviceId> it = deviceIdSet.iterator(); it.hasNext();) {
            DeviceId deviceId = (DeviceId) it.next();
            Collection<DefaultLabelResource> collection = maps.get(deviceId);
            request = new LabelResourceRequest(
                                               deviceId,
                                               LabelResourceRequest.Type.RELEASE,
                                               0, collection);
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
            queue.offer(labelResource);
        }
        log.info("success to release label resource");
        resourcePoolLock.writeLock().unlock();
        return true;
    }

    @Override
    public boolean isFull(DeviceId deviceId) {
        LabelResourcePool pool = resourcePool.get(deviceId);
        if (pool == null) {
            return true;
        }
        return pool.getCurrentUsedMaxLabelId() == pool.getEndLabel()
                && pool.getReleaseLabelId().size() == 0 ? true : false;
    }

    @Override
    public long getFreeNum(DeviceId deviceId) {
        LabelResourcePool pool = resourcePool.get(deviceId);
        if (pool == null) {
            return 0;
        }
        return pool.getEndLabel() - pool.getCurrentUsedMaxLabelId()
                + pool.getReleaseLabelId().size();
    }

    @Override
    public boolean hasDelegate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public LabelResourcePool getLabelResourcePool(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return resourcePool.get(deviceId);
    }

}
