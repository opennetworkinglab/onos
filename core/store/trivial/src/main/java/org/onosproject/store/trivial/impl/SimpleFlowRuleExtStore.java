package org.onosproject.store.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowext.FlowExtCompletedOperation;
import org.onosproject.net.flowext.FlowRuleBatchExtEvent;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.onosproject.net.flowext.FlowRuleExtEntry;
import org.onosproject.net.flowext.FlowRuleExtStore;
import org.onosproject.net.flowext.FlowRuleExtStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import com.esotericsoftware.kryo.Serializer;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
/**
 * Test for Managing inventory of flow rules using a distributed state management
 * protocol.
 */
@Component(immediate = true)
@Service
public class SimpleFlowRuleExtStore extends
                                     AbstractStore<FlowRuleBatchExtEvent, FlowRuleExtStoreDelegate>
                                            implements FlowRuleExtStore {
    private final Logger log = getLogger(getClass());
    private final ConcurrentMap<DeviceId, Collection<FlowRuleExtEntry>> flowRuleEntries = Maps.newConcurrentMap();
    private final AtomicInteger localBatchIdGen = new AtomicInteger();
    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
    	flowRuleEntries.clear();
        log.info("Stopped");
    }

    @Override
    public Future<FlowExtCompletedOperation> storeBatch(
            Collection<FlowRuleExtEntry> batchOperation) {
    	Set<FlowRuleExtEntry> resultSet = new HashSet<FlowRuleExtEntry>();
    	Collection<FlowRuleExtEntry> storeByDeviceId = null;
    	FlowRuleExtEntry flowRuleExtEntry = null;
        for (Iterator<FlowRuleExtEntry> iterator = batchOperation.iterator(); iterator.hasNext();) {
        	flowRuleExtEntry = (FlowRuleExtEntry) iterator.next();
            try{
                storeByDeviceId = flowRuleEntries.get(flowRuleExtEntry.getDeviceId());
                if (storeByDeviceId == null) {
            	     storeByDeviceId = new ArrayList<FlowRuleExtEntry>();
                }
                storeByDeviceId.add(flowRuleExtEntry);
                flowRuleEntries.put(flowRuleExtEntry.getDeviceId(), storeByDeviceId);
        	}catch (Exception e) {
        		resultSet.add(flowRuleExtEntry);
        	}
		}
//        int batchId = localBatchIdGen.getAndIncrement();
//        delegate.notify(FlowRuleBatchExtEvent.requested(new FlowRuleBatchExtRequest(batchId, batchOperation)));
        return resultSet.isEmpty()?Futures.immediateFuture(new FlowExtCompletedOperation(true, resultSet)):Futures.immediateFuture(new FlowExtCompletedOperation(false, resultSet));
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchExtEvent event) {
    	notifyDelegate(event);
    }

    @Override
    public Iterable<FlowRuleExtEntry> getExtMessages(DeviceId deviceId) {
    	Collection<FlowRuleExtEntry> storeByDeviceId = flowRuleEntries.get(deviceId);
        if (storeByDeviceId == null) {
    	     storeByDeviceId = Collections.emptyList();
        }
        return storeByDeviceId;
    }

    @Override
    public void registerSerializer(Class<?> classT, Serializer<?> serializer) {
    	
    }

}
