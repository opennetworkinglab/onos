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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.namedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.APPLY_BATCH_FLOWS;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.APPLY_EXTEND_FLOWS;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.GET_DEVICE_EXTENDFLOW_ENTRIES;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.GET_DEVICE_FLOW_ENTRIES;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.GET_FLOW_ENTRY;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.REMOVE_FLOW_ENTRY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleEvent.Type;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flowextend.FlowExtendCompletedOperation;
import org.onosproject.net.flowextend.FlowRuleBatchExtendEvent;
import org.onosproject.net.flowextend.FlowRuleBatchExtendRequest;
import org.onosproject.net.flowextend.FlowRuleExtendEntry;
import org.onosproject.net.flowextend.FlowRuleExtendStore;
import org.onosproject.net.flowextend.FlowRuleExtendStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.SMap;
import org.onosproject.store.serializers.DecodeTo;
import org.onosproject.store.serializers.FlowRuleExtendEntrySerializer;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hazelcast.core.IMap;

/**
 * Manages inventory of flow rules using a distributed state management
 * protocol.
 */
@Component(immediate = true)
@Service
public class DistributedFlowRuleExtendStore extends
                AbstractStore<FlowRuleBatchExtendEvent, FlowRuleExtendStoreDelegate>
		implements FlowRuleExtendStore {

	private final Logger log = getLogger(getClass());

	// primary data:
	// read/write needs to be locked
	private final ReentrantReadWriteLock flowEntriesLock = new ReentrantReadWriteLock();
	// store entries as a pile of rules, no info about device tables

	private final Multimap<DeviceId, OFMessage> flowOFmsgsById = ArrayListMultimap
			.<DeviceId, OFMessage> create();

	private final Multimap<DeviceId, FlowRuleExtendEntry> extendflowEntries = ArrayListMultimap
			.<DeviceId, FlowRuleExtendEntry> create();

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

	private Cache<Integer, SettableFuture<FlowExtendCompletedOperation>> pendingExtendFutures = CacheBuilder
			.newBuilder()
			.expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
			// .removalListener(new TimeoutFuture())
			.build();

	private final ExecutorService futureListeners = Executors
			.newCachedThreadPool(namedThreads("flowstore-peer-responders"));


	protected static final StoreSerializer SERIALIZER = new KryoSerializer() {
		@Override
		protected void setupKryoPool() {
			serializerPool = KryoNamespace
					.newBuilder()
					.register(DistributedStoreSerializers.STORE_COMMON)
					.nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
					.register(FlowRuleEvent.class)
					.register(new FlowRuleExtendEntrySerializer(),
					              FlowRuleExtendEntry.class).build();
		}
	};

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
                Set<OFMessage> ofmsgs = getInternalOFMessage(deviceId);
                ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
                for (OFMessage ofm : ofmsgs){
                	ofm.writeTo(buf);
                }
                try {
                    message.respond(buf.array());
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
                Collection<FlowRuleExtendEntry> operation=SERIALIZER.decode(message.payload());
                log.info("received batch request {}",operation);
                final ListenableFuture<FlowExtendCompletedOperation> f = storeBatchInternal(operation);
                
                f.addListener(new Runnable(){
                	@Override
                	public void run(){
                	    FlowExtendCompletedOperation result = Futures.getUnchecked(f);
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
		Collection<OFMessage> OFremoved = null;
		Collection<FlowRuleExtendEntry> removed = null;
		flowEntriesLock.writeLock().lock();
		try {
		    OFremoved = flowOFmsgsById.removeAll(did);
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
	public Iterable<OFMessage> getOFMessages(DeviceId deviceId) {
		
		ReplicaInfo replicaInfo = replicaInfoManager
				.getReplicaInfoFor(deviceId);

		if (!replicaInfo.master().isPresent()) {
			log.warn("Failed to storeBatch: No master for {}", deviceId);
			return Collections.emptyList();
		}

		if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
			return getInternalOFMessage(deviceId);
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
			ChannelBuffer cbf = ChannelBuffers.wrappedBuffer(bytes);
			OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();
			Collection<OFMessage> rules = new ArrayList();
			while(cbf.readerIndex()<cbf.capacity()) {
				OFMessage ofmessage = reader.readFrom(cbf);
				rules.add(ofmessage);
			}
			return ImmutableSet.copyOf(rules);
		} catch (IOException| TimeoutException | ExecutionException | InterruptedException e) {
			log.warn("Unable to fetch flow store contents from {}",replicaInfo.master().get());
		} catch (OFParseError e) {
			log.warn("Unable to read OfMessage");
		}
		return null;
	}
	
	public Set<OFMessage> getInternalOFMessage(DeviceId deviceId) {
		
		Collection<? extends OFMessage> rules = flowOFmsgsById.get(deviceId);
		if (rules == null) {
			return Collections.emptySet();
		}
		return ImmutableSet.copyOf(rules);
	}

        @Override
        public Future<FlowExtendCompletedOperation> storeBatch(Collection<FlowRuleExtendEntry> batchOperation) {
               // TODO Auto-generated method stub
              if (batchOperation.isEmpty()) {
               return Futures.immediateFuture(new FlowExtendCompletedOperation(true,
                            Collections.<FlowRuleExtendEntry> emptySet()));
              }
              // here should make some changes because all the collection belongs to one deviceId
             DeviceId deviceId = getBatchDeviceId(batchOperation);

             ReplicaInfo replicaInfo = replicaInfoManager
                            .getReplicaInfoFor(deviceId);

             if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
                   return storeBatchInternal(batchOperation);
             }

             log.trace(
                   "Forwarding storeBatch to {}, which is the primary (master) for device {}",
                   replicaInfo.master().orNull(), deviceId);
             
             ChannelBuffer buf=ChannelBuffers.dynamicBuffer();
             for (FlowRuleExtendEntry op : batchOperation){
                     buf.writeBytes(SERIALIZER.encode(op));;
             }
             ClusterMessage message = new ClusterMessage(clusterService.getLocalNode().id(), APPLY_EXTEND_FLOWS,
                     buf.array());

            try {
              ListenableFuture<byte[]> responseFuture = clusterCommunicator
                            .sendAndReceive(message, replicaInfo.master().get());
              //here should add another decode process 
              return Futures.transform(responseFuture,
                            new DecodeTo<FlowExtendCompletedOperation>(SERIALIZER));
            } catch (IOException e) {
              return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchExtendEvent event) {
        // TODO Auto-generated method stub
        final Integer batchId = event.subject().batchId();
        SettableFuture<FlowExtendCompletedOperation> future = pendingExtendFutures
                        .getIfPresent(batchId);
        if (future != null) {
                future.set(event.getresult());
                pendingExtendFutures.invalidate(batchId);
        }
    }

    private ListenableFuture<FlowExtendCompletedOperation> storeBatchInternal(
      Collection<FlowRuleExtendEntry> batchOperation) {
        for(FlowRuleExtendEntry operation : batchOperation) {
             DeviceId deviceId = DeviceId.deviceId(String.valueOf(operation.getDeviceId()));
             if (!extendflowEntries.containsEntry(deviceId, operation)) {
                    extendflowEntries.put(deviceId, operation);
             }
             byte[] boflen = operation.subBytes(operation.getFlowEntryExtend(), 16, 4);
             int length = operation.getInt(boflen);
             byte[] buf = operation.subBytes(operation.getFlowEntryExtend(), 20, length);
             try{
                    OFMessage msg = operation.readOFMessage(buf);
                    storeFlowRule(deviceId, msg);
             }catch (OFParseError e) {
                                                                                      
                    e.printStackTrace();
             }
        }
         SettableFuture<FlowExtendCompletedOperation> r = SettableFuture.create();
         final int batchId = localBatchIdGen.incrementAndGet();
         pendingExtendFutures.put(batchId, r);
         delegate.notify(FlowRuleBatchExtendEvent.requested(new FlowRuleBatchExtendRequest(batchId, batchOperation)));
         return r;
    }

    @Override
    public void storeFlowRule(DeviceId deviceId, OFMessage message) {
        // TODO Auto-generated method stub
        flowOFmsgsById.remove(deviceId, message);
        flowOFmsgsById.put(deviceId, message);
    }

    private DeviceId getBatchDeviceId(Collection<FlowRuleExtendEntry> batchOperation) {
        Iterator<FlowRuleExtendEntry> head = batchOperation.iterator();
        FlowRuleExtendEntry headOp = head.next();
        boolean sameId = true; 
        for(FlowRuleExtendEntry operation : batchOperation) {
            if(operation.getDeviceId() != headOp.getDeviceId()) {
                log.warn("this batch does not apply on one device Id ");
                sameId = false;
                break;
            }
        }
        return sameId? DeviceId.deviceId(String.valueOf(headOp.getDeviceId())) : null;
    }
}
