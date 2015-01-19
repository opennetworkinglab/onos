/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.flow.impl;

import static org.onlab.util.Tools.namedThreads;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.APPLY_EXTEND_FLOWS;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.GET_DEVICE_EXTENDFLOW_ENTRIES;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowext.FlowExtCompletedOperation;
import org.onosproject.net.flowext.FlowRuleBatchExtEvent;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.onosproject.net.flowext.FlowRuleExtEntry;
import org.onosproject.net.flowext.FlowRuleExtStore;
import org.onosproject.net.flowext.FlowRuleExtStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.serializers.DecodeTo;
import org.onosproject.store.serializers.FlowRuleExtEntrySerializer;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.slf4j.Logger;

import com.esotericsoftware.kryo.Serializer;
import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Manages inventory of flow rules using a distributed state management
 * protocol.
 */


@Component(immediate = true)
@Service
public class DistributedFlowRuleExtStore extends
                AbstractStore<FlowRuleBatchExtEvent, FlowRuleExtStoreDelegate>
		implements FlowRuleExtStore {

	private final Logger log = getLogger(getClass());

	// primary data:
	// read/write needs to be locked
	private final ReentrantReadWriteLock flowEntriesLock = new ReentrantReadWriteLock();

	// store entries as a pile of rules, no info about device tables
	private final Multimap<DeviceId, FlowRuleExtEntry> extendflowEntries = ArrayListMultimap
			.<DeviceId, FlowRuleExtEntry> create();

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected ReplicaInfoService replicaInfoManager;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected ClusterCommunicationService clusterCommunicator;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected ClusterService clusterService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected DeviceService deviceService;
	
	private final AtomicInteger localBatchIdGen = new AtomicInteger();

	private int pendingFutureTimeoutMinutes = 5;

	private Cache<Integer, SettableFuture<FlowExtCompletedOperation>> pendingExtendFutures = CacheBuilder
			.newBuilder()
			.expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
			// .removalListener(new TimeoutFuture())
			.build();

	private final ExecutorService futureListeners = Executors
			.newCachedThreadPool(namedThreads("flowstore-peer-responders"));


	private InternalKryoSerializer SERIALIZER = new InternalKryoSerializer();

	private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

	private ReplicaInfoEventListener replicaInfoEventListener;

    @Activate
    public void activate() {

        final NodeId local = clusterService.getLocalNode().id();

        clusterCommunicator.addSubscriber(GET_DEVICE_EXTENDFLOW_ENTRIES, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                DeviceId deviceId = SERIALIZER.decode(message.payload());
                log.trace("Received get flow entries request for {} from {}", deviceId, message.sender());
                Set<FlowRuleExtEntry> value = getInternalMessage(deviceId);
                ImmutableList<FlowRuleExtEntry> flowmsgs = ImmutableList.copyOf(value);
                try {
                    message.respond(SERIALIZER.encode(flowmsgs));
                } catch (IOException e) {
                    log.error("Failed to respond to peer's getFlowEntries request", e);
                }
            }
        });
        
        clusterCommunicator.addSubscriber(APPLY_EXTEND_FLOWS, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                ChannelBuffer buf = ChannelBuffers.wrappedBuffer(message.payload());
                //here should add a decode process
                ImmutableList<FlowRuleExtEntry> operation=SERIALIZER.decode(message.payload());
                log.info("received batch request {}",operation);
                final ListenableFuture<FlowExtCompletedOperation> f = storeBatchInternal(operation);
                
                f.addListener(new Runnable(){
                	@Override
                	public void run(){
                	    FlowExtCompletedOperation result = Futures.getUnchecked(f);
                		try {
                            message.respond(SERIALIZER.encode(result));
                        } catch (IOException e) {
                            log.error("Failed to respond back", e);
                        }
                	}
                }, futureListeners);
            }
        });

        replicaInfoEventListener = new InternalReplicaInfoEventListener();

        replicaInfoManager.addListener(replicaInfoEventListener);

        log.info("Started");
    }

	@Deactivate
	public void deactivate() {
		clusterCommunicator.removeSubscriber(APPLY_EXTEND_FLOWS);
		clusterCommunicator.removeSubscriber(GET_DEVICE_EXTENDFLOW_ENTRIES);
		replicaInfoManager.removeListener(replicaInfoEventListener);
		log.info("Stopped");
	}

	private void removeFromPrimary(final DeviceId did) {
		Collection<FlowRuleExtEntry> removed = null;
		flowEntriesLock.writeLock().lock();
		try {
		    removed = extendflowEntries.removeAll(did);
		} finally {
			flowEntriesLock.writeLock().unlock();
		}
		log.trace("removedFromPrimary {}", removed);
	}

	private final class InternalReplicaInfoEventListener implements
			ReplicaInfoEventListener {

		@Override
		public void event(ReplicaInfoEvent event) {
			final NodeId local = clusterService.getLocalNode().id();
			final DeviceId did = event.subject();
			final ReplicaInfo rInfo = event.replicaInfo();

			switch (event.type()) {
			case MASTER_CHANGED:
				if (!local.equals(rInfo.master().orNull())) {
					// This node is the new master, populate local structure
					// from backup
				        removeFromPrimary(did);
				}
				break;
			default:
				break;

			}
		}
	}

	@Override
	public Iterable<?> getExtMessages(DeviceId deviceId, Class<?> classT) {
		
		ReplicaInfo replicaInfo = replicaInfoManager
				.getReplicaInfoFor(deviceId);

		if (!replicaInfo.master().isPresent()) {
			log.warn("Failed to storeBatch: No master for {}", deviceId);
			return Collections.emptyList();
		}

		if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
			return getInternalMessage(deviceId);
		}

		log.trace(
				"Forwarding storeBatch to {}, which is the primary (master) for device {}",
				replicaInfo.master().orNull(), deviceId);

		ClusterMessage message = new ClusterMessage(clusterService.getLocalNode().id(), GET_DEVICE_EXTENDFLOW_ENTRIES,
				SERIALIZER.encode(deviceId));

		try {
			ListenableFuture<byte[]> responseFuture = clusterCommunicator
					.sendAndReceive(message, replicaInfo.master().get());
			byte[] bytes = responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
			//make some change about buffer
			ImmutableList<FlowRuleExtEntry> flows = SERIALIZER.decode(bytes);
			//common method to decode to classT
                        /*OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();
			Collection<OFMessage> rules = new ArrayList();
			while(cbf.readerIndex()<cbf.capacity()) {
				OFMessage ofmessage = reader.readFrom(cbf);
				rules.add(ofmessage);
			}*/
			return ImmutableSet.copyOf(rules);
		} catch (IOException| TimeoutException | ExecutionException | InterruptedException e) {
			log.warn("Unable to fetch flow store contents from {}",replicaInfo.master().get());
		} catch (OFParseError e) {
			log.warn("Unable to read OfMessage");
		}
		return null;
	}
	
	public Set<FlowRuleExtEntry> getInternalMessage(DeviceId deviceId) {
		
		Collection<FlowRuleExtEntry> rules = extendflowEntries.get(deviceId);
		if (rules == null) {
		         return Collections.emptySet();
		}
		return ImmutableSet.copyOf(rules);
	}

        @Override
        public Future<FlowExtCompletedOperation> storeBatch(Collection<FlowRuleExtEntry> batchOperation) {
               // TODO Auto-generated method stub
              if (batchOperation.isEmpty()) {
               return Futures.immediateFuture(new FlowExtCompletedOperation(true,
                            Collections.<FlowRuleExtEntry> emptySet()));
              }
              // here should make some changes because all the collection belongs to one deviceId
             DeviceId deviceId = getBatchDeviceId(batchOperation);

             if(deviceId == null) {
                 log.error("This Batch exists more than two deviceId");
                 return null;
             }
             ReplicaInfo replicaInfo = replicaInfoManager
                            .getReplicaInfoFor(deviceId);

             if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
                   return storeBatchInternal(batchOperation);
             }

             log.trace(
                   "Forwarding storeBatch to {}, which is the primary (master) for device {}",
                   replicaInfo.master().orNull(), deviceId);

             ImmutableList<FlowRuleExtEntry> flowmsgs = ImmutableList.copyOf(batchOperation);
             ClusterMessage message = new ClusterMessage(clusterService.getLocalNode().id(), APPLY_EXTEND_FLOWS,
                                                         SERIALIZER.encode(flowmsgs));

            try {
              ListenableFuture<byte[]> responseFuture = clusterCommunicator
                            .sendAndReceive(message, replicaInfo.master().get());
              //here should add another decode process 
              return Futures.transform(responseFuture,
                            new DecodeTo<FlowExtCompletedOperation>(SERIALIZER));
            } catch (IOException e) {
              return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchExtEvent event) {
        // TODO Auto-generated method stub
        final Integer batchId = event.subject().batchId();
        SettableFuture<FlowExtCompletedOperation> future = pendingExtendFutures
                        .getIfPresent(batchId);
        if (future != null) {
                future.set(event.getresult());
                pendingExtendFutures.invalidate(batchId);
        }
    }

    private ListenableFuture<FlowExtCompletedOperation> storeBatchInternal(
      Collection<FlowRuleExtEntry> batchOperation) {
        for(FlowRuleExtEntry operation : batchOperation) {
             DeviceId deviceId = DeviceId.deviceId(String.valueOf(operation.getDeviceId()));
             if (!extendflowEntries.containsEntry(deviceId, operation)) {
                    extendflowEntries.put(deviceId, operation);
             }
        }
         SettableFuture<FlowExtCompletedOperation> r = SettableFuture.create();
         final int batchId = localBatchIdGen.incrementAndGet();
         pendingExtendFutures.put(batchId, r);
         delegate.notify(FlowRuleBatchExtEvent.requested(new FlowRuleBatchExtRequest(batchId, batchOperation)));
         return r;
    }

    private DeviceId getBatchDeviceId(Collection<FlowRuleExtEntry> batchOperation) {
        Iterator<FlowRuleExtEntry> head = batchOperation.iterator();
        FlowRuleExtEntry headOp = head.next();
        boolean sameId = true; 
        for(FlowRuleExtEntry operation : batchOperation) {
            if(operation.getDeviceId() != headOp.getDeviceId()) {
                log.warn("this batch does not apply on one device Id ");
                sameId = false;
                break;
            }
        }
        return sameId? headOp.getDeviceId() : null;
    }

    @Override
    public void registerSerializer(Class<?> classT, Serializer<?> serializer) {
        // TODO Auto-generated method stub
        SERIALIZER.setupKryoPool(classT, serializer);
    }

    private Iterable<?> decodeFlowExt(Collection<FlowRuleExtEntry> batchOperation) {
        return null;
    }
    /** 
     * Internal Serializer used for register self-defined serializer, this 
     * serializer used for decoding byte Stream to object and use to show in GUI
     * or CLI 
     */
    private  class InternalKryoSerializer implements StoreSerializer {

        public KryoNamespace serializerPool;
        public InternalKryoSerializer() {
            setupKryoPool();
        }

        /**
         * Sets up the common serializers pool.
         */
        protected void setupKryoPool() {
            serializerPool = KryoNamespace
                    .newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(new FlowRuleExtEntrySerializer(),
                                  FlowRuleExtEntry.class).build();
        }
        
        
        /**
         * Sets up the special serializers pool.
         */
        protected void setupKryoPool(Class<?> classT, Serializer<?> serializer) {
            serializerPool = KryoNamespace
                    .newBuilder()
                    .register(serializerPool)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(serializer, classT).build();
        }

        @Override
        public byte[] encode(final Object obj) {
            return serializerPool.serialize(obj);
        }

        @Override
        public <T> T decode(final byte[] bytes) {
            if (bytes == null) {
                return null;
            }
            return serializerPool.deserialize(bytes);
        }

        @Override
        public void encode(Object obj, ByteBuffer buffer) {
            serializerPool.serialize(obj, buffer);
        }

        @Override
        public <T> T decode(ByteBuffer buffer) {
            return serializerPool.deserialize(buffer);
        }

        @Override
        public void encode(Object obj, OutputStream stream) {
            serializerPool.serialize(obj, stream);
        }

        @Override
        public <T> T decode(InputStream stream) {
            return serializerPool.deserialize(stream);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("serializerPool", serializerPool)
                    .toString();
        }
    }
}
