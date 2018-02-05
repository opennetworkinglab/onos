/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.pi.demo.app.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of an app providing fabric connectivity for a 2-stage
 * Clos topology of P4Runtime devices.
 */
@Component(immediate = true)
public abstract class AbstractUpgradableFabricApp {

    private static final Map<String, AbstractUpgradableFabricApp>
            APP_HANDLES = Maps.newConcurrentMap();

    // TOPO_SIZE should be the same of the --size argument when running bmv2-demo.py
    private static final int TOPO_SIZE = 2;
    private static final boolean WITH_IMBALANCED_STRIPING = false;
    private static final int HASHED_LINKS = TOPO_SIZE + (WITH_IMBALANCED_STRIPING ? 1 : 0);

    private static final int FLOW_PRIORITY = 100;
    private static final int CHECK_TOPOLOGY_INTERVAL_SECONDS = 5;

    private static final int CLEANUP_SLEEP = 2000;

    protected final Logger log = getLogger(getClass());

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(8, groupedThreads("onos/pi-demo-app", "pi-app-task", log));

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final String appName;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ApplicationAdminService appService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfService piPipeconfService;

    private boolean appActive = false;
    private boolean appFreezed = false;

    private boolean otherAppFound = false;
    private AbstractUpgradableFabricApp otherApp;

    private boolean flowRuleGenerated = false;
    protected ApplicationId appId;

    private Collection<PiPipeconf> appPipeconfs;

    private Set<DeviceId> leafSwitches;
    private Set<DeviceId> spineSwitches;

    private Map<DeviceId, List<FlowRule>> deviceFlowRules;
    private Map<DeviceId, Boolean> pipeconfFlags;
    private Map<DeviceId, Boolean> ruleFlags;

    private ConcurrentMap<DeviceId, Lock> deviceLocks = Maps.newConcurrentMap();

    /**
     * Creates a new PI fabric app.
     *
     * @param appName      app name
     * @param appPipeconfs collection of compatible pipeconfs
     */
    protected AbstractUpgradableFabricApp(String appName,
                                          Collection<PiPipeconf> appPipeconfs) {
        this.appName = checkNotNull(appName);
        this.appPipeconfs = checkNotNull(appPipeconfs);
        checkArgument(appPipeconfs.size() > 0, "appPipeconfs cannot have size 0");
    }

    @Activate
    public void activate() {
        log.info("Starting...");

        appActive = true;
        appFreezed = false;

        if (APP_HANDLES.size() > 0) {
            if (APP_HANDLES.size() > 1) {
                throw new IllegalStateException(
                        "Found more than 1 active app handles");
            }
            otherAppFound = true;
            otherApp = APP_HANDLES.values().iterator().next();
            log.info("Found other fabric app active, signaling to freeze to {}...",
                     otherApp.appName);
            otherApp.setAppFreezed(true);
        }

        APP_HANDLES.put(appName, this);

        appId = coreService.registerApplication(appName);
        deviceService.addListener(deviceListener);

        init();

        log.info("STARTED", appId.id());
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopping...");
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            List<Runnable> runningTasks = executorService.shutdownNow();
            log.warn("Unable to stop the following tasks: {}", runningTasks);
            Thread.currentThread().interrupt();
        }
        scheduledExecutorService.shutdown();
        deviceService.removeListener(deviceListener);
        flowRuleService.removeFlowRulesById(appId);

        appActive = false;
        APP_HANDLES.remove(appName);

        log.info("STOPPED");
    }

    private void init() {

        // Reset any previous state
        synchronized (this) {
            flowRuleGenerated = Boolean.FALSE;
            leafSwitches = Sets.newHashSet();
            spineSwitches = Sets.newHashSet();
            deviceFlowRules = Maps.newConcurrentMap();
            ruleFlags = Maps.newConcurrentMap();
            pipeconfFlags = Maps.newConcurrentMap();
        }

        // Schedules a thread that periodically checks the topology, as soon as
        // it corresponds to the expected one, it generates the necessary flow
        // rules and starts the deploy process on each device.
        scheduledExecutorService.scheduleAtFixedRate(
                this::checkTopologyAndGenerateFlowRules,
                0, CHECK_TOPOLOGY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void setAppFreezed(boolean appFreezed) {
        this.appFreezed = appFreezed;
        if (appFreezed) {
            log.info("Freezing...");
        } else {
            log.info("Unfreezing...!");
        }
    }

    /**
     * Perform device initialization. Returns true if the operation was
     * successful, false otherwise.
     *
     * @param deviceId a device id
     * @return a boolean value
     */
    public abstract boolean initDevice(DeviceId deviceId);

    /**
     * Generates a list of flow rules for the given leaf switch, source host,
     * destination hosts, spine switches and topology.
     *
     * @param leaf     a leaf device id
     * @param srcHost  a source host
     * @param dstHosts a collection of destination hosts
     * @param spines   a collection of spine device IDs
     * @param topology a topology
     * @return a list of flow rules
     * @throws FlowRuleGeneratorException if flow rules cannot be generated
     */
    public abstract List<FlowRule> generateLeafRules(DeviceId leaf, Host srcHost,
                                                     Set<Host> dstHosts,
                                                     Set<DeviceId> spines,
                                                     Topology topology)
            throws FlowRuleGeneratorException;

    /**
     * Generates a list of flow rules for the given spine switch, destination
     * hosts and topology.
     *
     * @param deviceId a spine device id
     * @param dstHosts a collection of destination hosts
     * @param topology a topology
     * @return a list of flow rules
     * @throws FlowRuleGeneratorException if flow rules cannot be generated
     */
    public abstract List<FlowRule> generateSpineRules(DeviceId deviceId,
                                                      Set<Host> dstHosts,
                                                      Topology topology)
            throws FlowRuleGeneratorException;

    private void deployAllDevices() {
        if (otherAppFound && otherApp.appActive) {
            log.info("Deactivating other app...");
            appService.deactivate(otherApp.appId);
            try {
                Thread.sleep(CLEANUP_SLEEP);
            } catch (InterruptedException e) {
                log.warn("Cleanup sleep interrupted!");
                Thread.currentThread().interrupt();
            }
        }

        Stream.concat(leafSwitches.stream(), spineSwitches.stream())
                .map(deviceService::getDevice)
                .forEach(device -> spawnTask(() -> deployDevice(device)));
    }

    private boolean matchPipeconf(PiPipeconfId piPipeconfId) {
        return appPipeconfs.stream()
                .anyMatch(p -> p.id().equals(piPipeconfId));
    }

    /**
     * Executes a device deploy.
     *
     * @param device a device
     */
    private void deployDevice(Device device) {

        DeviceId deviceId = device.id();

        // Synchronize executions over the same device.
        Lock lock = deviceLocks.computeIfAbsent(deviceId, k -> new ReentrantLock());
        lock.lock();

        try {
            // Set pipeconf flag if not already done.
            if (!pipeconfFlags.getOrDefault(deviceId, false)) {
                if (piPipeconfService.ofDevice(deviceId).isPresent() &&
                        matchPipeconf(piPipeconfService.ofDevice(deviceId).get())) {
                    pipeconfFlags.put(device.id(), true);
                } else {
                    log.warn("Wrong pipeconf for {}, expecting {}, but found {}, aborting deploy",
                             deviceId, Arrays.toString(appPipeconfs.toArray()),
                             piPipeconfService.ofDevice(deviceId).get());
                    return;
                }
            }

            // Initialize device.
            if (!initDevice(deviceId)) {
                log.warn("Failed to initialize device {}", deviceId);
            }

            // Install rules.
            if (!ruleFlags.getOrDefault(deviceId, false) &&
                    deviceFlowRules.containsKey(deviceId)) {
                log.info("Installing {} rules for {}...",
                         deviceFlowRules.get(deviceId).size(), deviceId);
                installFlowRules(deviceFlowRules.get(deviceId));
                ruleFlags.put(deviceId, true);
            }
        } finally {
            lock.unlock();
        }
    }

    private void spawnTask(Runnable task) {
        executorService.execute(task);
    }


    private void installFlowRules(Collection<FlowRule> rules) {
        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
        rules.forEach(opsBuilder::add);
        flowRuleService.apply(opsBuilder.build());
    }

    /**
     * Generates flow rules to provide host-to-host connectivity for the given
     * topology and hosts.
     */
    private synchronized void checkTopologyAndGenerateFlowRules() {

        Topology topo = topologyService.currentTopology();
        Set<Host> hosts = Sets.newHashSet(hostService.getHosts());

        if (flowRuleGenerated) {
            log.debug("Flow rules have been already generated, aborting...");
            return;
        }

        log.debug("Starting flow rules generator...");

        TopologyGraph graph = topologyService.getGraph(topo);
        Set<DeviceId> spines = Sets.newHashSet();
        Set<DeviceId> leafs = Sets.newHashSet();
        graph.getVertexes().stream()
                .map(TopologyVertex::deviceId)
                .forEach(did -> (isSpine(did, topo) ? spines : leafs).add(did));

        if (spines.size() != TOPO_SIZE || leafs.size() != TOPO_SIZE) {
            log.info("Invalid leaf/spine count, aborting... > leafCount={}, spineCount={}",
                     spines.size(), leafs.size());
            return;
        }

        for (DeviceId did : spines) {
            int portCount = deviceService.getPorts(did).size();
            // Expected port count: num leafs + 1 redundant leaf link (if imbalanced)
            if (portCount != HASHED_LINKS) {
                log.info("Invalid port count for spine, aborting... > deviceId={}, portCount={}",
                         did, portCount);
                return;
            }
        }
        for (DeviceId did : leafs) {
            int portCount = deviceService.getPorts(did).size();
            // Expected port count: num spines + host port + 1 redundant spine link
            if (portCount != HASHED_LINKS + 1) {
                log.info("Invalid port count for leaf, aborting... > deviceId={}, portCount={}",
                         did, portCount);
                return;
            }
        }

        // Check hosts, number and exactly one per leaf
        Map<DeviceId, Host> hostMap = Maps.newHashMap();
        hosts.forEach(h -> hostMap.put(h.location().deviceId(), h));
        if (hosts.size() != TOPO_SIZE || !leafs.equals(hostMap.keySet())) {
            log.info("Wrong host configuration, aborting... > hostCount={}, hostMapz={}",
                     hosts.size(), hostMap);
            return;
        }

        List<FlowRule> newFlowRules = Lists.newArrayList();

        try {
            for (DeviceId deviceId : leafs) {
                Host srcHost = hostMap.get(deviceId);
                Set<Host> dstHosts = hosts.stream()
                        .filter(h -> h != srcHost)
                        .collect(toSet());
                newFlowRules.addAll(generateLeafRules(deviceId, srcHost,
                                                      dstHosts, spines, topo));
            }
            for (DeviceId deviceId : spines) {
                newFlowRules.addAll(generateSpineRules(deviceId, hosts, topo));
            }
        } catch (FlowRuleGeneratorException e) {
            log.warn("Exception while executing flow rule generator: {}",
                     e.getMessage());
            return;
        }

        if (newFlowRules.size() == 0) {
            // Something went wrong
            log.error("0 flow rules generated, BUG?");
            return;
        }

        // All good!
        // Divide flow rules per device id...
        ImmutableMap.Builder<DeviceId, List<FlowRule>> mapBuilder =
                ImmutableMap.builder();
        concat(spines.stream(), leafs.stream())
                .map(deviceId -> ImmutableList.copyOf(
                        newFlowRules.stream()
                                .filter(fr -> fr.deviceId().equals(deviceId))
                                .iterator()))
                .forEach(frs -> mapBuilder.put(frs.get(0).deviceId(), frs));
        this.deviceFlowRules = mapBuilder.build();

        this.leafSwitches = ImmutableSet.copyOf(leafs);
        this.spineSwitches = ImmutableSet.copyOf(spines);

        // Avoid other executions to modify the generated flow rules.
        flowRuleGenerated = true;

        log.info("Generated {} flow rules for {} devices",
                 newFlowRules.size(), spines.size() + leafs.size());

        spawnTask(this::deployAllDevices);
    }

    /**
     * Returns a new, pre-configured flow rule builder.
     *
     * @param did     a device id
     * @param tableId a table id
     * @return a new flow rule builder
     */
    protected FlowRule.Builder flowRuleBuilder(DeviceId did, PiTableId tableId)
            throws FlowRuleGeneratorException {

        final Device device = deviceService.getDevice(did);
        if (!device.is(PiPipelineInterpreter.class)) {
            throw new FlowRuleGeneratorException(format(
                    "Device %s has no PiPipelineInterpreter", did));
        }

        return DefaultFlowRule.builder()
                .forDevice(did)
                .forTable(tableId)
                .fromApp(appId)
                .withPriority(FLOW_PRIORITY)
                .makePermanent();
    }

    private List<Port> getHostPorts(DeviceId deviceId, Topology topology) {
        // Get all non-fabric ports.
        return deviceService
                .getPorts(deviceId)
                .stream()
                .filter(p -> !isFabricPort(p, topology))
                .collect(Collectors.toList());
    }

    private boolean isSpine(DeviceId deviceId, Topology topology) {
        // True if all ports are fabric.
        return getHostPorts(deviceId, topology).size() == 0;
    }

    protected boolean isFabricPort(Port port, Topology topology) {
        // True if the port connects this device to another infrastructure device.
        return topologyService.isInfrastructure(
                topology, new ConnectPoint(port.element().id(), port.number()));
    }

    /**
     * A listener of device events that executes a device deploy task each time
     * a device is added, updated or re-connects.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            spawnTask(() -> deployDevice(event.subject()));
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return !appFreezed &&
                    (event.type() == DEVICE_ADDED ||
                            event.type() == DEVICE_UPDATED ||
                            (event.type() == DEVICE_AVAILABILITY_CHANGED &&
                                    deviceService.isAvailable(event.subject().id())));
        }
    }

    /**
     * An exception occurred while generating flow rules for this fabric.
     */
    public static class FlowRuleGeneratorException extends Exception {

        public FlowRuleGeneratorException() {
        }

        FlowRuleGeneratorException(String msg) {
            super(msg);
        }
    }
}
