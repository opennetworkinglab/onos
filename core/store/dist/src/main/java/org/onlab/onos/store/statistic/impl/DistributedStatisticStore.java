package org.onlab.onos.store.statistic.impl;

import static org.onlab.onos.store.statistic.impl.StatisticStoreMessageSubjects.*;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.ImmutableSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions;
import org.onlab.onos.net.statistic.StatisticStore;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.cluster.messaging.ClusterMessageResponse;
import org.onlab.onos.store.flow.ReplicaInfo;
import org.onlab.onos.store.flow.ReplicaInfoService;
import org.onlab.onos.store.serializers.KryoNamespaces;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Maintains statistics using RPC calls to collect stats from remote instances
 * on demand.
 */
@Component(immediate = true)
@Service
public class DistributedStatisticStore implements StatisticStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private Map<ConnectPoint, InternalStatisticRepresentation> representations =
            new ConcurrentHashMap<>();

    private Map<ConnectPoint, Set<FlowEntry>> previous =
            new ConcurrentHashMap<>();

    private Map<ConnectPoint, Set<FlowEntry>> current =
            new ConcurrentHashMap<>();

    protected static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    // register this store specific classes here
                    .build()
                    .populate(1);
        }
    };;

    private static final long STATISTIC_STORE_TIMEOUT_MILLIS = 3000;

    @Activate
    public void activate() {
        clusterCommunicator.addSubscriber(GET_CURRENT, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                ConnectPoint cp = SERIALIZER.decode(message.payload());
                try {
                    message.respond(SERIALIZER.encode(getCurrentStatisticInternal(cp)));
                } catch (IOException e) {
                    log.error("Failed to respond back", e);
                }
            }
        });

        clusterCommunicator.addSubscriber(GET_PREVIOUS, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                ConnectPoint cp = SERIALIZER.decode(message.payload());
                try {
                    message.respond(SERIALIZER.encode(getPreviousStatisticInternal(cp)));
                } catch (IOException e) {
                    log.error("Failed to respond back", e);
                }
            }
        });
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void prepareForStatistics(FlowRule rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }
        InternalStatisticRepresentation rep;
        synchronized (representations) {
            rep = getOrCreateRepresentation(cp);
        }
        rep.prepare();
    }

    @Override
    public void removeFromStatistics(FlowRule rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }
        InternalStatisticRepresentation rep = representations.get(cp);
        if (rep != null) {
            rep.remove(rule);
        }
    }

    @Override
    public void addOrUpdateStatistic(FlowEntry rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }
        InternalStatisticRepresentation rep = representations.get(cp);
        if (rep != null && rep.submit(rule)) {
            updatePublishedStats(cp, rep.get());
        }
    }

    private synchronized void updatePublishedStats(ConnectPoint cp,
                                                   Set<FlowEntry> flowEntries) {
        Set<FlowEntry> curr = current.get(cp);
        if (curr == null) {
            curr = new HashSet<>();
        }
        previous.put(cp, curr);
        current.put(cp, flowEntries);

    }

    @Override
    public Set<FlowEntry> getCurrentStatistic(ConnectPoint connectPoint) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(connectPoint.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return getCurrentStatisticInternal(connectPoint);
        } else {
            ClusterMessage message = new ClusterMessage(
                    clusterService.getLocalNode().id(),
                    GET_CURRENT,
                    SERIALIZER.encode(connectPoint));

            try {
                ClusterMessageResponse response =
                        clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
                return SERIALIZER.decode(response.get(STATISTIC_STORE_TIMEOUT_MILLIS,
                                                      TimeUnit.MILLISECONDS));
            } catch (IOException | TimeoutException e) {
                // FIXME: throw a FlowStoreException
                throw new RuntimeException(e);
            }
        }

    }

    private synchronized Set<FlowEntry> getCurrentStatisticInternal(ConnectPoint connectPoint) {
        return current.get(connectPoint);
    }

    @Override
    public Set<FlowEntry> getPreviousStatistic(ConnectPoint connectPoint) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(connectPoint.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return getPreviousStatisticInternal(connectPoint);
        } else {
            ClusterMessage message = new ClusterMessage(
                    clusterService.getLocalNode().id(),
                    GET_CURRENT,
                    SERIALIZER.encode(connectPoint));

            try {
                ClusterMessageResponse response =
                        clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
                return SERIALIZER.decode(response.get(STATISTIC_STORE_TIMEOUT_MILLIS,
                                                      TimeUnit.MILLISECONDS));
            } catch (IOException | TimeoutException e) {
                // FIXME: throw a FlowStoreException
                throw new RuntimeException(e);
            }
        }

    }

    private synchronized Set<FlowEntry> getPreviousStatisticInternal(ConnectPoint connectPoint) {
        return previous.get(connectPoint);
    }

    private InternalStatisticRepresentation getOrCreateRepresentation(ConnectPoint cp) {

        if (representations.containsKey(cp)) {
            return representations.get(cp);
        } else {
            InternalStatisticRepresentation rep = new InternalStatisticRepresentation();
            representations.put(cp, rep);
            return rep;
        }

    }

    private ConnectPoint buildConnectPoint(FlowRule rule) {
        PortNumber port = getOutput(rule);
        if (port == null) {
            log.warn("Rule {} has no output.", rule);
            return null;
        }
        ConnectPoint cp = new ConnectPoint(rule.deviceId(), port);
        return cp;
    }

    private PortNumber getOutput(FlowRule rule) {
        for (Instruction i : rule.treatment().instructions()) {
            if (i.type() == Instruction.Type.OUTPUT) {
                Instructions.OutputInstruction out = (Instructions.OutputInstruction) i;
                return out.port();
            }
            if (i.type() == Instruction.Type.DROP) {
                return PortNumber.P0;
            }
        }
        return null;
    }

    private class InternalStatisticRepresentation {

        private final AtomicInteger counter = new AtomicInteger(0);
        private final Set<FlowEntry> rules = new HashSet<>();

        public void prepare() {
            counter.incrementAndGet();
        }

        public synchronized void remove(FlowRule rule) {
            rules.remove(rule);
            counter.decrementAndGet();
        }

        public synchronized boolean submit(FlowEntry rule) {
            if (rules.contains(rule)) {
                rules.remove(rule);
            }
            rules.add(rule);
            if (counter.get() == 0) {
                return true;
            } else {
                return counter.decrementAndGet() == 0;
            }
        }

        public synchronized Set<FlowEntry> get() {
            counter.set(rules.size());
            return ImmutableSet.copyOf(rules);
        }


    }

}
