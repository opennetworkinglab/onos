/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.flowext.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flowext.DefaultFlowRuleExt;
import org.onosproject.net.flowext.DownStreamFlowEntry;
import org.onosproject.net.flowext.FlowExtCompletedOperation;
import org.onosproject.net.flowext.FlowRuleExtRouter;
import org.onosproject.net.flowext.FlowRuleExtRouterListener;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.serializers.DecodeTo;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.store.flowext.impl.FlowExtRouterMessageSubjects.APPLY_EXTEND_FLOWS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 * Implement a simple routing-like mechanism to directly send service data to its master and push to device.
 * This Router does not save any flow rule extension data in cache, it focus on routing mechanism.
 */
@Component(immediate = true, enabled = false)
@Service
public class DefaultFlowRuleExtRouter
        implements FlowRuleExtRouter {

    private final Logger log = getLogger(getClass());

    // TODO: Make configurable.
    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 4;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private int pendingFutureTimeoutMinutes = 5;

    protected Set<FlowRuleExtRouterListener> routerListener = new HashSet<>();
    private Cache<Long, SettableFuture<FlowExtCompletedOperation>> pendingExtendFutures = CacheBuilder
            .newBuilder()
            .expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
                    // .removalListener(new TimeoutFuture())
            .build();

    private final ExecutorService futureListeners = Executors
            .newCachedThreadPool(groupedThreads("onos/flow", "store-peer-responders"));

    private ExecutorService messageHandlingExecutor;

    protected static final StoreSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(FlowExtCompletedOperation.class)
                    .register(FlowRuleBatchRequest.class)
                    .register(DownStreamFlowEntry.class)
                    .register(DefaultFlowRuleExt.class)
                    .build();
        }
    };

    private ReplicaInfoEventListener replicaInfoEventListener;

    @Activate
    public void activate() {

        messageHandlingExecutor = Executors.newFixedThreadPool(
                MESSAGE_HANDLER_THREAD_POOL_SIZE,
                groupedThreads("onos/flow", "message-handlers"));

        clusterCommunicator.addSubscriber(APPLY_EXTEND_FLOWS,
            new ClusterMessageHandler() {

              @Override
              public void handle(ClusterMessage message) {
                  // decode the extended flow entry and store them in memory.
                  FlowRuleBatchRequest operation = SERIALIZER.decode(message.payload());
                  log.info("received batch request {}", operation);
                  final ListenableFuture<FlowExtCompletedOperation> f = applyBatchInternal(operation);
                  f.addListener(new Runnable() {
                      @Override
                      public void run() {
                          FlowExtCompletedOperation result = Futures.getUnchecked(f);
                          try {
                              message.respond(SERIALIZER.encode(result));
                          } catch (IOException e) {
                              log.error("Failed to respond back", e);
                          }
                      }
                  }, futureListeners);
              }
            }, messageHandlingExecutor);

        replicaInfoManager.addListener(replicaInfoEventListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator.removeSubscriber(APPLY_EXTEND_FLOWS);
        messageHandlingExecutor.shutdown();
        replicaInfoManager.removeListener(replicaInfoEventListener);
        log.info("Stopped");
    }

    /**
     * apply the sub batch of flow extension rules.
     *
     * @param batchOperation batch of flow rules.
     *                       A batch can contain flow rules for a single device only.
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    @Override
    public Future<FlowExtCompletedOperation> applySubBatch(FlowRuleBatchRequest batchOperation) {
        // TODO Auto-generated method stub
        if (batchOperation.ops().isEmpty()) {
            return Futures.immediateFuture(new FlowExtCompletedOperation(
                    batchOperation.batchId(), true, Collections.emptySet()));
        }
        // get the deviceId all the collection belongs to
        DeviceId deviceId = getBatchDeviceId(batchOperation.ops());

        if (deviceId == null) {
            log.error("This Batch exists more than two deviceId");
            return null;
        }
        ReplicaInfo replicaInfo = replicaInfoManager
                .getReplicaInfoFor(deviceId);

        if (replicaInfo.master().get()
                .equals(clusterService.getLocalNode().id())) {
            return applyBatchInternal(batchOperation);
        }

        log.trace("Forwarding storeBatch to {}, which is the primary (master) for device {}",
                  replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(clusterService
                    .getLocalNode().id(), APPLY_EXTEND_FLOWS, SERIALIZER.encode(batchOperation));

        try {
            ListenableFuture<byte[]> responseFuture = clusterCommunicator
                    .sendAndReceive(message, replicaInfo.master().get());
            // here should add another decode process
            return Futures.transform(responseFuture,
                                     new DecodeTo<FlowExtCompletedOperation>(SERIALIZER));
        } catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * apply the batch in local node.
     * It means this instance is master of the device the flow entry belongs to.
     *
     * @param batchOperation a collection of flow entry, all they should send down to one device
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    private ListenableFuture<FlowExtCompletedOperation> applyBatchInternal(FlowRuleBatchRequest batchOperation) {
        SettableFuture<FlowExtCompletedOperation> r = SettableFuture.create();
        pendingExtendFutures.put(batchOperation.batchId(), r);
        // here should notify manager to complete
        notify(batchOperation);
        return r;
    }

    /**
     * Get the deviceId of this batch.
     * The whole Batch should belong to one deviceId.
     *
     * @param batchOperation a collection of flow entry, all they should send down to one device
     * @return the deviceId the whole batch belongs to
     */
    private DeviceId getBatchDeviceId(Collection<FlowRuleBatchEntry> batchOperation) {
        Iterator<FlowRuleBatchEntry> head = batchOperation.iterator();
        FlowRuleBatchEntry headOp = head.next();
        boolean sameId = true;
        for (FlowRuleBatchEntry operation : batchOperation) {
            if (operation.target().deviceId() != headOp.target().deviceId()) {
                log.warn("this batch does not apply on one device Id ");
                sameId = false;
                break;
            }
        }
        return sameId ? headOp.target().deviceId() : null;
    }

    /**
     * Notify the listener of Router to do some reaction.
     *
     * @param request the requested operation to do
     */
    public void notify(FlowRuleBatchRequest request) {
        for (FlowRuleExtRouterListener listener : routerListener) {
            listener.notify(FlowRuleBatchEvent
                                    // TODO fill in the deviceId
                                    .requested(request, null));
        }
    }

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param event flow rule batch event
     */
    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        // TODO Auto-generated method stub
        final Long batchId = event.subject().batchId();
        SettableFuture<FlowExtCompletedOperation> future = pendingExtendFutures
                .getIfPresent(batchId);
        if (future != null) {
            FlowRuleBatchRequest request = event.subject();
            CompletedBatchOperation result = event.result();
            FlowExtCompletedOperation completed =
                    new FlowExtCompletedOperation(request.batchId(), result.isSuccess(), result.failedItems());
            future.set(completed);
            pendingExtendFutures.invalidate(batchId);
        }
    }

    /**
     * Register the listener to monitor Router,
     * The Router find master to send downStream.
     *
     * @param listener the listener to register
     */
    @Override
    public void addListener(FlowRuleExtRouterListener listener) {
        routerListener.add(listener);
    }

    /**
     * Remove the listener of Router.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(FlowRuleExtRouterListener listener) {
        routerListener.remove(listener);
    }
}