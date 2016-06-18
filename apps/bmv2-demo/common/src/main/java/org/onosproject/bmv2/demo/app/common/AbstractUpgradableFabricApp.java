/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.demo.app.common;

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
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
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
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of an app providing fabric connectivity for a 2-stage Clos topology of BMv2 devices.
 */
@Component(immediate = true)
public abstract class AbstractUpgradableFabricApp {

    private static final Map<String, AbstractUpgradableFabricApp> APP_HANDLES = Maps.newConcurrentMap();

    private static final int NUM_LEAFS = 3;
    private static final int NUM_SPINES = 3;
    private static final int FLOW_PRIORITY = 100;

    private static final int CLEANUP_SLEEP = 2000;

    protected final Logger log = getLogger(getClass());

    private final TopologyListener topologyListener = new InternalTopologyListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(8, groupedThreads("onos/bmv2-demo-app", "bmv2-app-task", log));

    private final String appName;
    private final String configurationName;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ApplicationAdminService appService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2DeviceContextService bmv2ContextService;

    private boolean appActive = false;
    private boolean appFreezed = false;

    private boolean otherAppFound = false;
    private AbstractUpgradableFabricApp otherApp;

    private boolean flowRuleGenerated = false;
    private ApplicationId appId;

    private Bmv2DeviceContext bmv2Context;

    private Set<DeviceId> leafSwitches;
    private Set<DeviceId> spineSwitches;

    private Map<DeviceId, List<FlowRule>> deviceFlowRules;
    private Map<DeviceId, Bmv2DeviceContext> previousContexts;
    private Map<DeviceId, Boolean> contextFlags;
    private Map<DeviceId, Boolean> ruleFlags;

    private ConcurrentMap<DeviceId, Lock> deviceLocks = Maps.newConcurrentMap();

    /**
     * Creates a new BMv2 fabric app.
     *
     * @param appName           app name
     * @param configurationName a common name for the P4 program / BMv2 configuration used by this app
     * @param context a BMv2 device context to be used on devices
     */
    protected AbstractUpgradableFabricApp(String appName, String configurationName, Bmv2DeviceContext context) {
        this.appName = checkNotNull(appName);
        this.configurationName = checkNotNull(configurationName);
        this.bmv2Context = checkNotNull(context);
    }

    @Activate
    public void activate() {
        log.info("Starting...");

        appActive = true;
        appFreezed = false;

        if (APP_HANDLES.size() > 0) {
            if (APP_HANDLES.size() > 1) {
                throw new IllegalStateException("Found more than 1 active app handles");
            }
            otherAppFound = true;
            otherApp = APP_HANDLES.values().iterator().next();
            log.info("Found other fabric app active, signaling to freeze to {}...", otherApp.appName);
            otherApp.setAppFreezed(true);
        }

        APP_HANDLES.put(appName, this);

        appId = coreService.registerApplication(appName);

        topologyService.addListener(topologyListener);
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);

        bmv2ContextService.registerInterpreterClassLoader(bmv2Context.interpreter().getClass(),
                                                          this.getClass().getClassLoader());

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
        }
        deviceService.removeListener(deviceListener);
        topologyService.removeListener(topologyListener);
        hostService.removeListener(hostListener);
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
            contextFlags = Maps.newConcurrentMap();
        }

        // Start flow rules generator...
        spawnTask(() -> generateFlowRules(topologyService.currentTopology(), Sets.newHashSet(hostService.getHosts())));
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
     * Perform device initialization. Returns true if the operation was successful, false otherwise.
     *
     * @param deviceId a device id
     * @return a boolean value
     */
    public abstract boolean initDevice(DeviceId deviceId);

    /**
     * Generates a list of flow rules for the given leaf switch, source host, destination hosts, spine switches and
     * topology.
     *
     * @param leaf     a leaf device id
     * @param srcHost  a source host
     * @param dstHosts a collection of destination hosts
     * @param spines   a collection of spine device IDs
     * @param topology a topology
     * @return a list of flow rules
     * @throws FlowRuleGeneratorException if flow rules cannot be generated
     */
    public abstract List<FlowRule> generateLeafRules(DeviceId leaf, Host srcHost, Collection<Host> dstHosts,
                                                     Collection<DeviceId> spines, Topology topology)
            throws FlowRuleGeneratorException;

    /**
     * Generates a list of flow rules for the given spine switch, destination hosts and topology.
     *
     * @param deviceId a spine device id
     * @param dstHosts a collection of destination hosts
     * @param topology a topology
     * @return a list of flow rules
     * @throws FlowRuleGeneratorException if flow rules cannot be generated
     */
    public abstract List<FlowRule> generateSpineRules(DeviceId deviceId, Collection<Host> dstHosts, Topology topology)
            throws FlowRuleGeneratorException;

    private void deployAllDevices() {
        if (otherAppFound && otherApp.appActive) {
            log.info("Deactivating other app...");
            appService.deactivate(otherApp.appId);
            try {
                Thread.sleep(CLEANUP_SLEEP);
            } catch (InterruptedException e) {
                log.warn("Cleanup sleep interrupted!");
                Thread.interrupted();
            }
        }

        Stream.concat(leafSwitches.stream(), spineSwitches.stream())
                .map(deviceService::getDevice)
                .forEach(device -> spawnTask(() -> deployDevice(device)));
    }

    /**
     * Executes a device deploy.
     *
     * @param device a device
     */
    public void deployDevice(Device device) {

        DeviceId deviceId = device.id();

        // Synchronize executions over the same device.
        Lock lock = deviceLocks.computeIfAbsent(deviceId, k -> new ReentrantLock());
        lock.lock();

        try {
            // Set context if not already done.
            if (!contextFlags.getOrDefault(deviceId, false)) {
                log.info("Setting context to {} for {}...", configurationName, deviceId);
                bmv2ContextService.setContext(deviceId, bmv2Context);
                contextFlags.put(device.id(), true);
            }

            // Initialize device.
            if (!initDevice(deviceId)) {
                log.warn("Failed to initialize device {}", deviceId);
            }

            // Install rules.
            if (!ruleFlags.getOrDefault(deviceId, false)) {
                List<FlowRule> rules = deviceFlowRules.getOrDefault(deviceId, Collections.emptyList());
                if (rules.size() > 0) {
                    log.info("Installing rules for {}...", deviceId);
                    installFlowRules(rules);
                    ruleFlags.put(deviceId, true);
                }
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

    private void removeFlowRules(Collection<FlowRule> rules) {
        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();
        rules.forEach(opsBuilder::remove);
        flowRuleService.apply(opsBuilder.build());
    }

    /**
     * Generates the flow rules to provide host-to-host connectivity for the given topology and hosts.
     *
     * @param topo  a topology
     * @param hosts a collection of hosts
     */
    private synchronized void generateFlowRules(Topology topo, Collection<Host> hosts) {

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

        if (spines.size() != NUM_SPINES || leafs.size() != NUM_LEAFS) {
            log.info("Invalid leaf/spine switches count, aborting... > leafCount={}, spineCount={}",
                     spines.size(), leafs.size());
            return;
        }

        for (DeviceId did : spines) {
            int portCount = deviceService.getPorts(did).size();
            // Expected port count: num leafs + 1 redundant leaf link
            if (portCount != (NUM_LEAFS + 1)) {
                log.info("Invalid port count for spine, aborting... > deviceId={}, portCount={}", did, portCount);
                return;
            }
        }
        for (DeviceId did : leafs) {
            int portCount = deviceService.getPorts(did).size();
            // Expected port count: num spines + host port + 1 redundant spine link
            if (portCount != (NUM_SPINES + 2)) {
                log.info("Invalid port count for leaf, aborting... > deviceId={}, portCount={}", did, portCount);
                return;
            }
        }

        // Check hosts, number and exactly one per leaf
        Map<DeviceId, Host> hostMap = Maps.newHashMap();
        hosts.forEach(h -> hostMap.put(h.location().deviceId(), h));
        if (hosts.size() != NUM_LEAFS || !leafs.equals(hostMap.keySet())) {
            log.info("Wrong host configuration, aborting... > hostCount={}, hostMapz={}", hosts.size(), hostMap);
            return;
        }

        List<FlowRule> newFlowRules = Lists.newArrayList();

        try {
            for (DeviceId deviceId : leafs) {
                Host srcHost = hostMap.get(deviceId);
                Set<Host> dstHosts = hosts.stream().filter(h -> h != srcHost).collect(toSet());
                newFlowRules.addAll(generateLeafRules(deviceId, srcHost, dstHosts, spines, topo));
            }
            for (DeviceId deviceId : spines) {
                newFlowRules.addAll(generateSpineRules(deviceId, hosts, topo));
            }
        } catch (FlowRuleGeneratorException e) {
            log.warn("Exception while executing flow rule generator: ", e.toString());
            return;
        }

        if (newFlowRules.size() == 0) {
            // Something went wrong
            log.error("0 flow rules generated, BUG?");
            return;
        }

        // All good!
        // Divide flow rules per device id...
        ImmutableMap.Builder<DeviceId, List<FlowRule>> mapBuilder = ImmutableMap.builder();
        concat(spines.stream(), leafs.stream())
                .map(deviceId -> ImmutableList.copyOf(newFlowRules
                                                              .stream()
                                                              .filter(fr -> fr.deviceId().equals(deviceId))
                                                              .iterator()))
                .forEach(frs -> mapBuilder.put(frs.get(0).deviceId(), frs));
        this.deviceFlowRules = mapBuilder.build();

        this.leafSwitches = ImmutableSet.copyOf(leafs);
        this.spineSwitches = ImmutableSet.copyOf(spines);

        // Avoid other executions to modify the generated flow rules.
        flowRuleGenerated = true;

        log.info("Generated {} flow rules for {} devices", newFlowRules.size(), spines.size() + leafs.size());

        spawnTask(this::deployAllDevices);
    }

    /**
     * Returns a new, pre-configured flow rule builder.
     *
     * @param did       a device id
     * @param tableName a table name
     * @return a new flow rule builder
     */
    protected FlowRule.Builder flowRuleBuilder(DeviceId did, String tableName) throws FlowRuleGeneratorException {
        Map<String, Integer> tableMap = bmv2Context.interpreter().tableIdMap().inverse();
        if (tableMap.get(tableName) == null) {
            throw new FlowRuleGeneratorException("Unknown table " + tableName);
        }
        return DefaultFlowRule.builder()
                .forDevice(did)
                .forTable(tableMap.get(tableName))
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
        return topologyService.isInfrastructure(topology, new ConnectPoint(port.element().id(), port.number()));
    }

    /**
     * A listener of topology events that executes a flow rule generation task each time a device is added.
     */
    private class InternalTopologyListener implements TopologyListener {

        @Override
        public void event(TopologyEvent event) {
            spawnTask(() -> generateFlowRules(event.subject(), Sets.newHashSet(hostService.getHosts())));
        }

        @Override
        public boolean isRelevant(TopologyEvent event) {
            return !appFreezed &&
                    // If at least one reason is of type DEVICE_ADDED.
                    event.reasons().stream().
                            filter(r -> r instanceof DeviceEvent)
                            .filter(r -> ((DeviceEvent) r).type() == DEVICE_ADDED)
                            .findAny()
                            .isPresent();
        }
    }

    /**
     * A listener of device events that executes a device deploy task each time a device is added, updated or
     * re-connects.
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
     * A listener of host events that generates flow rules each time a new host is added.
     */
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            spawnTask(() -> generateFlowRules(topologyService.currentTopology(),
                                              Sets.newHashSet(hostService.getHosts())));
        }

        @Override
        public boolean isRelevant(HostEvent event) {
            return !appFreezed && event.type() == HOST_ADDED;
        }
    }

    /**
     * An exception occurred while generating flow rules for this fabric.
     */
    public class FlowRuleGeneratorException extends Exception {

        public FlowRuleGeneratorException() {
        }

        public FlowRuleGeneratorException(String msg) {
            super(msg);
        }

        public FlowRuleGeneratorException(Exception cause) {
            super(cause);
        }
    }
}