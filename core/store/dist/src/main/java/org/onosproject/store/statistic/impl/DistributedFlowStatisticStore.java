/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.base.Objects;
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
import org.onosproject.net.statistic.FlowStatisticStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.store.OsgiPropertyConstants.DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE;
import static org.onosproject.store.OsgiPropertyConstants.DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Maintains flow statistics using RPC calls to collect stats from remote instances
 * on demand.
 */
@Component(
        immediate = true,
        service = FlowStatisticStore.class,
        property = {
                DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE + ":Integer=" + DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT
        }
)
public class DistributedFlowStatisticStore implements FlowStatisticStore {
    private final Logger log = getLogger(getClass());

    private static final String FORMAT = "Setting: messageHandlerThreadPoolSize={}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    private Map<ConnectPoint, Set<FlowEntry>> previous =
            new ConcurrentHashMap<>();

    private Map<ConnectPoint, Set<FlowEntry>> current =
            new ConcurrentHashMap<>();

    public static final MessageSubject GET_CURRENT = new MessageSubject("peer-return-current");
    public static final MessageSubject GET_PREVIOUS = new MessageSubject("peer-return-previous");

    protected static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    private NodeId local;
    private ExecutorService messageHandlingExecutor;

    /** Size of thread pool to assign message handler. */
    private static int messageHandlerThreadPoolSize = DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;


    private static final long STATISTIC_STORE_TIMEOUT_MILLIS = 3000;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());

        modified(context);

        local = clusterService.getLocalNode().id();

        messageHandlingExecutor = Executors.newFixedThreadPool(
                messageHandlerThreadPoolSize,
                groupedThreads("onos/store/statistic", "message-handlers", log));

        clusterCommunicator.addSubscriber(
                GET_CURRENT, SERIALIZER::decode, this::getCurrentStatisticInternal, SERIALIZER::encode,
                messageHandlingExecutor);

        clusterCommunicator.addSubscriber(
                GET_CURRENT, SERIALIZER::decode, this::getPreviousStatisticInternal, SERIALIZER::encode,
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
            String s = get(properties, DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE);

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
    public synchronized void removeFlowStatistic(FlowRule rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }

        // remove this rule if present from current map
        current.computeIfPresent(cp, (c, e) -> {
            e.remove(rule);
            return e;
        });

        // remove this on if present from previous map
        previous.computeIfPresent(cp, (c, e) -> {
            e.remove(rule);
            return e;
        });
    }

    @Override
    public synchronized void addFlowStatistic(FlowEntry rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }

        // create one if absent and add this rule
        current.putIfAbsent(cp, new HashSet<>());
        current.computeIfPresent(cp, (c, e) -> {
            e.add(rule); return e;
        });

        // remove previous one if present
        previous.computeIfPresent(cp, (c, e) -> {
            e.remove(rule); return e;
        });
    }

    @Override
    public synchronized void updateFlowStatistic(FlowEntry rule) {
        ConnectPoint cp = buildConnectPoint(rule);
        if (cp == null) {
            return;
        }

        Set<FlowEntry> curr = current.get(cp);
        if (curr == null) {
            addFlowStatistic(rule);
        } else {
            Optional<FlowEntry> f = curr.stream().filter(c -> rule.equals(c)).
                    findAny();
            if (f.isPresent() && rule.bytes() < f.get().bytes()) {
                log.debug("DistributedFlowStatisticStore:updateFlowStatistic():" +
                        " Invalid Flow Update! Will be removed!!" +
                        " curr flowId=" + Long.toHexString(rule.id().value()) +
                        ", prev flowId=" + Long.toHexString(f.get().id().value()) +
                        ", curr bytes=" + rule.bytes() + ", prev bytes=" + f.get().bytes() +
                        ", curr life=" + rule.life() + ", prev life=" + f.get().life() +
                        ", curr lastSeen=" + rule.lastSeen() + ", prev lastSeen=" + f.get().lastSeen());
                // something is wrong! invalid flow entry, so delete it
                removeFlowStatistic(rule);
                return;
            }
            Set<FlowEntry> prev = previous.get(cp);
            if (prev == null) {
                prev = new HashSet<>();
                previous.put(cp, prev);
            }

            // previous one is exist
            if (f.isPresent()) {
                // remove old one and add new one
                prev.remove(rule);
                if (!prev.add(f.get())) {
                    log.debug("DistributedFlowStatisticStore:updateFlowStatistic():" +
                                    " flowId={}, add failed into previous.",
                            Long.toHexString(rule.id().value()));
                }
            }

            // remove old one and add new one
            curr.remove(rule);
            if (!curr.add(rule)) {
                log.debug("DistributedFlowStatisticStore:updateFlowStatistic():" +
                                " flowId={}, add failed into current.",
                        Long.toHexString(rule.id().value()));
            }
        }
    }

    @Override
    public Set<FlowEntry> getCurrentFlowStatistic(ConnectPoint connectPoint) {
        final DeviceId deviceId = connectPoint.deviceId();

        NodeId master = mastershipService.getMasterFor(deviceId);
        if (master == null) {
            log.warn("No master for {}", deviceId);
            return Collections.emptySet();
        }

        if (Objects.equal(local, master)) {
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
    public Set<FlowEntry> getPreviousFlowStatistic(ConnectPoint connectPoint) {
        final DeviceId deviceId = connectPoint.deviceId();

        NodeId master = mastershipService.getMasterFor(deviceId);
        if (master == null) {
            log.warn("No master for {}", deviceId);
            return Collections.emptySet();
        }

        if (Objects.equal(local, master)) {
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
                                                     groupedThreads("DistFlowStats", "messageHandling-%d", log));
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
