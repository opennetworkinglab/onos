package org.onosproject.store.trivial.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
/**
 * Test for Managing inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class SimpleFlowRuleExtStore extends
                                     AbstractStore<FlowRuleBatchExtEvent, FlowRuleExtStoreDelegate>
                                            implements FlowRuleExtStore {
    private final Logger log = getLogger(getClass());
    private final ConcurrentMap<DeviceId, Collection<FlowRuleExtEntry>> flowRuleEntries = Maps.newConcurrentMap();
    private int pendingFutureTimeoutMinutes = 5;
    private final int bufferSize = 1000;
    private final int maxSize = 4096;
    private final Kryo kryo = new Kryo();
    private Cache<Integer, SettableFuture<FlowExtCompletedOperation>> pendingExtendFutures = CacheBuilder
            .newBuilder()
            .expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
            // .removalListener(new TimeoutFuture())
            .build();
    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowRuleEntries.clear();
        log.info("Stopped");
    }

    /**
     * Stores a batch of flow extension rules.
     *
     * @param batchOperation batch of flow rules.
     *           A batch can contain flow rules for a single device only.
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    @Override
    public Future<FlowExtCompletedOperation> storeBatch(
            FlowRuleBatchExtRequest batchOperation) {
        Set<FlowRuleExtEntry> resultSet = new HashSet<FlowRuleExtEntry>();
        Collection<FlowRuleExtEntry> storeByDeviceId = null;
        Collection<FlowRuleExtEntry> batch = batchOperation.getBatch();
        for (FlowRuleExtEntry entry : batch) {
            try {
                storeByDeviceId = flowRuleEntries.get(entry.getDeviceId());
                if (storeByDeviceId == null) {
                     storeByDeviceId = new ArrayList<FlowRuleExtEntry>();
                }
                storeByDeviceId.add(entry);
                flowRuleEntries.put(entry.getDeviceId(), storeByDeviceId);
            } catch (Exception e) {
                resultSet.add(entry);
            }
        }
//        int batchId = localBatchIdGen.getAndIncrement();
//        delegate.notify(FlowRuleBatchExtEvent.requested(new FlowRuleBatchExtRequest(batchId, batchOperation)));
        boolean success = resultSet.isEmpty() ? true : false;
        FlowExtCompletedOperation completed = new FlowExtCompletedOperation(
                batchOperation.batchId(), success, resultSet);
        return Futures.immediateFuture(completed);
    }

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param event flow rule batch event
     */
    @Override
    public void batchOperationComplete(FlowRuleBatchExtEvent event) {
        final Integer batchId = event.subject().batchId();
        SettableFuture<FlowExtCompletedOperation> future = pendingExtendFutures
                .getIfPresent(batchId);
        if (future != null) {
            future.set(event.getresult());
            pendingExtendFutures.invalidate(batchId);
        }
        notifyDelegate(event);
    }

    /**
     * Get all extended flow entry of device, using for showing in GUI or CLI.
     *
     * @param did DeviceId of the device role changed
     * @return message parsed from byte[] using the specific serializer
     */
    @Override
    public Iterable<?> getExtMessages(DeviceId deviceId) {
        Collection<FlowRuleExtEntry> storeByDeviceId = getInternalMessage(deviceId);
        return decodeFlowExt(storeByDeviceId);
    }

    /**
     * Register classT and serializer which can decode byte stream to classT object.
     *
     * @param classT the class flowEntryExtension can be decoded to.
     * @param serializer the serializer apps provide using to decode flowEntryExtension
     */
    @Override
    public void registerSerializer(Class<?> classT, Serializer<?> serializer) {
        kryo.register(classT, serializer);
    }

    /**
     * decode flowExt to any ClassT type user-defined.
     *
     * @param batchOperation object to be decoded
     * @return Collection of ClassT object
     */
    private Iterable<?> decodeFlowExt(Collection<FlowRuleExtEntry> batchOperation) {
        Collection<Object> flowExtensions = new ArrayList<Object>();
        ByteBufferOutput output = new ByteBufferOutput(bufferSize, maxSize);
        for (FlowRuleExtEntry entry : batchOperation) {
            kryo.writeClass(output, entry.getClassT());
            kryo.writeObject(output, entry.getFlowEntryExt());
            flowExtensions.add(kryo.readClassAndObject(new Input(output.toBytes())));
            output.clear();
        }
        output.close();
        return flowExtensions;
    }

    /**
     * Get the messages stored in local memory.
     *
     * @param did DeviceId of the device role changed
     * @return all extended  flow rule entry belong to deviceId
     */
    public Set<FlowRuleExtEntry> getInternalMessage(DeviceId deviceId) {
            Collection<FlowRuleExtEntry> rules = flowRuleEntries
                    .get(deviceId);
            if (rules == null) {
                return Collections.emptySet();
            }
            return ImmutableSet.copyOf(rules);
    }
}
