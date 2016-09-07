/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.store.statistic.impl;

import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.statistic.StatisticStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.StoreSerializer;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Maintains statistics using RPC calls to collect stats from remote instances
 * on demand.
 */
@Component(immediate = true)
@Service
public class DistributedStatisticStore implements StatisticStore {

    private final Logger log = getLogger(getClass());

    private static final String FORMAT = "Setting: messageHandlerThreadPoolSize={}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    public static final MessageSubject GET_CURRENT = new MessageSubject("peer-return-current");
    public static final MessageSubject GET_PREVIOUS = new MessageSubject("peer-return-previous");

    private Map<ConnectPoint, InternalStatisticRepresentation> representations =
            new ConcurrentHashMap<>();

    private Map<ConnectPoint, Set<FlowEntry>> previous =
            new ConcurrentHashMap<>();

    private Map<ConnectPoint, Set<FlowEntry>> current =
            new ConcurrentHashMap<>();

    protected static final StoreSerializer SERIALIZER = StoreSerializer.using(KryoNamespaces.API);

    private ExecutorService messageHandlingExecutor;

    private static final int DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE = 4;
    @Property(name = "messageHandlerThreadPoolSize", intValue = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE,
            label = "Size of thread pool to assign message handler")
    private static int messageHandlerThreadPoolSize = DEFAULT_MESSAGE_HANDLER_THREAD_POOL_SIZE;

    private static final long STATISTIC_STORE_TIMEOUT_MILLIS = 3000;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());

        modified(context);

        messageHandlingExecutor = Executors.newFixedThreadPool(
                messageHandlerThreadPoolSize,
                groupedThreads("onos/store/statistic", "message-handlers", log));

        clusterCommunicator.<ConnectPoint, Set<FlowEntry>>addSubscriber(GET_CURRENT,
                SERIALIZER::decode,
                this::getCurrentStatisticInternal,
                SERIALIZER::encode,
                messageHandlingExecutor);

        clusterCommunicator.<ConnectPoint, Set<FlowEntry>>addSubscriber(GET_PREVIOUS,
                SERIALIZER::decode,
                this::getPreviousStatisticInternal,
                SERIALIZER::encode,
                messageHandlingExecutor);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        clusterCommunicator.removeSubscriber(GET_PREVIOUS);
        clusterCommunicator.removeSubscriber(GET_CURRENT);
        messageHandlingExecutor.shutdown();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        int newMessageHandlerThreadPoolSize;

        try {
            String s = get(properties, "messageHandlerThreadPoolSize");

            newMessageHandlerThreadPoolSize =
                    isNullOrEmpty(s) ? messageHandlerThreadPoolSize : Integer.parseInt(s.trim());

        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newMessageHandlerThreadPoolSize = messageHandlerThreadPoolSize;
        }

        // Any change in the following parameters implies thread pool restart
        if (newMessageHandlerThreadPoolSize != messageHandlerThreadPoolSize) {
            setMessageHandlerThreadPoolSize(newMessageHandlerThreadPoolSize);
            restartMessageHandlerThreadPool();
        }

        log.info(FORMAT, messageHandlerThreadPoolSize);
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
    public synchronized void removeFromStatistics(FlowRule rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }
        InternalStatisticRepresentation rep = representations.get(cp);
        if (rep != null && rep.remove(rule)) {
            updatePublishedStats(cp, Collections.emptySet());
        }
        Set<FlowEntry> values = current.get(cp);
        if (values != null) {
            values.remove(rule);
        }
        values = previous.get(cp);
        if (values != null) {
            values.remove(rule);
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
        final DeviceId deviceId = connectPoint.deviceId();
        NodeId master = mastershipService.getMasterFor(deviceId);
        if (master == null) {
            log.warn("No master for {}", deviceId);
            return Collections.emptySet();
        }
        if (master.equals(clusterService.getLocalNode().id())) {
            return getCurrentStatisticInternal(connectPoint);
        } else {
            return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(
                                        connectPoint,
                                        GET_CURRENT,
                                        SERIALIZER::encode,
                                        SERIALIZER::decode,
                                        master),
                                   STATISTIC_STORE_TIMEOUT_MILLIS,
                                   TimeUnit.MILLISECONDS,
                                   Collections.emptySet());
        }

    }

    private synchronized Set<FlowEntry> getCurrentStatisticInternal(ConnectPoint connectPoint) {
        return current.get(connectPoint);
    }

    @Override
    public Set<FlowEntry> getPreviousStatistic(ConnectPoint connectPoint) {
        final DeviceId deviceId = connectPoint.deviceId();
        NodeId master = mastershipService.getMasterFor(deviceId);
        if (master == null) {
            log.warn("No master for {}", deviceId);
            return Collections.emptySet();
        }
        if (master.equals(clusterService.getLocalNode().id())) {
            return getPreviousStatisticInternal(connectPoint);
        } else {
            return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(
                                        connectPoint,
                                        GET_PREVIOUS,
                                        SERIALIZER::encode,
                                        SERIALIZER::decode,
                                        master),
                                   STATISTIC_STORE_TIMEOUT_MILLIS,
                                   TimeUnit.MILLISECONDS,
                                   Collections.emptySet());
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
            return null;
        }
        ConnectPoint cp = new ConnectPoint(rule.deviceId(), port);
        return cp;
    }

    private PortNumber getOutput(FlowRule rule) {
        for (Instruction i : rule.treatment().allInstructions()) {
            if (i.type() == Instruction.Type.OUTPUT) {
                Instructions.OutputInstruction out = (Instructions.OutputInstruction) i;
                return out.port();
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

        public synchronized boolean remove(FlowRule rule) {
            rules.remove(rule);
            return counter.decrementAndGet() == 0;
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
            return Sets.newHashSet(rules);
        }


    }

    /**
     * Sets thread pool size of message handler.
     *
     * @param poolSize
     */
    private void setMessageHandlerThreadPoolSize(int poolSize) {
        checkArgument(poolSize >= 0, "Message handler pool size must be 0 or more");
        messageHandlerThreadPoolSize = poolSize;
    }

    /**
     * Restarts thread pool of message handler.
     */
    private void restartMessageHandlerThreadPool() {
        ExecutorService prevExecutor = messageHandlingExecutor;
        messageHandlingExecutor = newFixedThreadPool(getMessageHandlerThreadPoolSize(),
                                                     groupedThreads("DistStatsStore", "messageHandling-%d", log));
        prevExecutor.shutdown();
    }

    /**
     * Gets current thread pool size of message handler.
     *
     * @return messageHandlerThreadPoolSize
     */
    private int getMessageHandlerThreadPoolSize() {
        return messageHandlerThreadPoolSize;
    }

}
