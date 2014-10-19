package org.onlab.onos.store.flow.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleEvent.Type;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.net.flow.StoredFlowEntry;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageResponse;
import org.onlab.onos.store.flow.ReplicaInfo;
import org.onlab.onos.store.flow.ReplicaInfoService;
import org.onlab.onos.store.serializers.DistributedStoreSerializers;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoPool;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
        extends AbstractStore<FlowRuleEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, StoredFlowEntry> flowEntries =
            ArrayListMultimap.<DeviceId, StoredFlowEntry>create();

    private final Multimap<Short, FlowRule> flowEntriesById =
            ArrayListMultimap.<Short, FlowRule>create();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    protected static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoPool.newBuilder()
                    .register(DistributedStoreSerializers.COMMON)
                    .build()
                    .populate(1);
        }
    };

    // TODO: make this configurable
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 1000;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }


    @Override
    public int getFlowRuleCount() {
        return flowEntries.size();
    }

    @Override
    public synchronized FlowEntry getFlowEntry(FlowRule rule) {
        return getFlowEntryInternal(rule);
    }

    private synchronized StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
        for (StoredFlowEntry f : flowEntries.get(rule.deviceId())) {
            if (f.equals(rule)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public synchronized Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        Collection<? extends FlowEntry> rules = flowEntries.get(deviceId);
        if (rules == null) {
            return Collections.emptyList();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public synchronized Iterable<FlowRule> getFlowRulesByAppId(ApplicationId appId) {
        Collection<FlowRule> rules = flowEntriesById.get(appId.id());
        if (rules == null) {
            return Collections.emptyList();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            storeFlowEntryInternal(rule);
            return;
        }

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                FlowStoreMessageSubjects.STORE_FLOW_RULE,
                SERIALIZER.encode(rule));

        try {
            ClusterMessageResponse response = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            response.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (IOException | TimeoutException e) {
            // FIXME: throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private synchronized void storeFlowEntryInternal(FlowRule flowRule) {
        StoredFlowEntry flowEntry = new DefaultFlowEntry(flowRule);
        DeviceId deviceId = flowRule.deviceId();
        // write to local copy.
        if (!flowEntries.containsEntry(deviceId, flowEntry)) {
            flowEntries.put(deviceId, flowEntry);
            flowEntriesById.put(flowRule.appId(), flowEntry);
        }
        // write to backup.
        // TODO: write to a hazelcast map.
    }

    @Override
    public synchronized void deleteFlowRule(FlowRule rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            deleteFlowRuleInternal(rule);
            return;
        }

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                FlowStoreMessageSubjects.DELETE_FLOW_RULE,
                SERIALIZER.encode(rule));

        try {
            ClusterMessageResponse response = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            response.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (IOException | TimeoutException e) {
            // FIXME: throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private synchronized void deleteFlowRuleInternal(FlowRule flowRule) {
        StoredFlowEntry entry = getFlowEntryInternal(flowRule);
        if (entry == null) {
            return;
        }
        entry.setState(FlowEntryState.PENDING_REMOVE);
        // TODO: also update backup.
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return addOrUpdateFlowRuleInternal(rule);
        }

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                FlowStoreMessageSubjects.ADD_OR_UPDATE_FLOW_RULE,
                SERIALIZER.encode(rule));

        try {
            ClusterMessageResponse response = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(response.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException e) {
            // FIXME: throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private synchronized FlowRuleEvent addOrUpdateFlowRuleInternal(FlowEntry rule) {
        DeviceId did = rule.deviceId();

        // check if this new rule is an update to an existing entry
        StoredFlowEntry stored = getFlowEntryInternal(rule);
        if (stored != null) {
            stored.setBytes(rule.bytes());
            stored.setLife(rule.life());
            stored.setPackets(rule.packets());
            if (stored.state() == FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.ADDED);
                return new FlowRuleEvent(Type.RULE_ADDED, rule);
            }
            return new FlowRuleEvent(Type.RULE_UPDATED, rule);
        }

        // TODO: Confirm if this behavior is correct. See SimpleFlowRuleStore
        flowEntries.put(did, new DefaultFlowEntry(rule));
        return null;

        // TODO: also update backup.
    }

    @Override
    public FlowRuleEvent removeFlowRule(FlowEntry rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            // bypass and handle it locally
            return removeFlowRuleInternal(rule);
        }

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                FlowStoreMessageSubjects.REMOVE_FLOW_RULE,
                SERIALIZER.encode(rule));

        try {
            ClusterMessageResponse response = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(response.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException e) {
            // FIXME: throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private synchronized FlowRuleEvent removeFlowRuleInternal(FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        if (flowEntries.remove(rule.deviceId(), rule)) {
            return new FlowRuleEvent(RULE_REMOVED, rule);
        } else {
            return null;
        }
        // TODO: also update backup.
    }
}
