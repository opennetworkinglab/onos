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

package org.onosproject.provider.general.device.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Striped;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfConfig;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.general.device.api.GeneralProviderDeviceConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses drivers to detect device and do initial handshake and
 * channel establishment with devices. Any other provider specific operation is
 * also delegated to the DeviceHandshaker driver.
 */
@Beta
@Component(immediate = true)
public class GeneralDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    private static final String DRIVER = "driver";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfService piPipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LeadershipService leadershipService;

    private static final String STATS_POLL_FREQUENCY = "deviceStatsPollFrequency";
    private static final int DEFAULT_STATS_POLL_FREQUENCY = 10;
    @Property(name = STATS_POLL_FREQUENCY, intValue = DEFAULT_STATS_POLL_FREQUENCY,
            label = "Configure poll frequency for port status and statistics; " +
                    "default is 10 sec")
    private int statsPollFrequency = DEFAULT_STATS_POLL_FREQUENCY;

    private static final String PROBE_FREQUENCY = "deviceProbeFrequency";
    private static final int DEFAULT_PROBE_FREQUENCY = 10;
    @Property(name = PROBE_FREQUENCY, intValue = DEFAULT_PROBE_FREQUENCY,
            label = "Configure probe frequency for checking device availability; " +
                    "default is 10 sec")
    private int probeFrequency = DEFAULT_PROBE_FREQUENCY;

    private static final String OP_TIMEOUT_SHORT = "deviceOperationTimeoutShort";
    private static final int DEFAULT_OP_TIMEOUT_SHORT = 10;
    @Property(name = OP_TIMEOUT_SHORT, intValue = DEFAULT_OP_TIMEOUT_SHORT,
            label = "Configure timeout in seconds for device operations " +
                    "that are supposed to take a short time " +
                    "(e.g. checking device reachability); default is 10 seconds")
    private int opTimeoutShort = DEFAULT_OP_TIMEOUT_SHORT;

    private static final String OP_TIMEOUT_LONG = "deviceOperationTimeoutLong";
    private static final int DEFAULT_OP_TIMEOUT_LONG = 60;
    @Property(name = OP_TIMEOUT_LONG, intValue = DEFAULT_OP_TIMEOUT_LONG,
            label = "Configure timeout in seconds for device operations " +
                    "that are supposed to take a relatively long time " +
                    "(e.g. pushing a large pipeline configuration with slow " +
                    "network); default is 60 seconds")
    private int opTimeoutLong = DEFAULT_OP_TIMEOUT_LONG;

    private static final String APP_NAME = "org.onosproject.generaldeviceprovider";
    private static final String URI_SCHEME = "device";
    private static final String CFG_SCHEME = "generalprovider";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.general.provider.device";
    private static final int CORE_POOL_SIZE = 10;
    private static final String UNKNOWN = "unknown";

    //FIXME this will be removed when the configuration is synced at the source.
    private static final Set<String> PIPELINE_CONFIGURABLE_PROTOCOLS = ImmutableSet.of("p4runtime");

    //FIXME to be removed when netcfg will issue device events in a bundle or
    //ensures all configuration needed is present
    private Set<DeviceId> deviceConfigured = new CopyOnWriteArraySet<>();
    private Set<DeviceId> driverConfigured = new CopyOnWriteArraySet<>();
    private Set<DeviceId> pipelineConfigured = new CopyOnWriteArraySet<>();

    private final Map<DeviceId, DeviceHandshaker> handshakers = Maps.newConcurrentMap();
    private final Map<DeviceId, MastershipRole> requestedRoles = Maps.newConcurrentMap();
    private final Striped<Lock> deviceLocks = Striped.lock(30);

    private ExecutorService connectionExecutor
            = newFixedThreadPool(CORE_POOL_SIZE, groupedThreads(
            "onos/generaldeviceprovider-device-connect", "%d", log));
    private ScheduledExecutorService statsExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE, groupedThreads(
            "onos/generaldeviceprovider-stats-poll", "%d", log));
    private ConcurrentMap<DeviceId, ScheduledFuture<?>> statsPollingTasks = new ConcurrentHashMap<>();
    private ScheduledExecutorService probeExecutor
            = newSingleThreadScheduledExecutor(groupedThreads(
            "onos/generaldeviceprovider-probe-", "%d", log));
    private ScheduledFuture<?> probeTask = null;

    private DeviceProviderService providerService;
    private InternalDeviceListener deviceListener = new InternalDeviceListener();

    private final ConfigFactory factory =
            new ConfigFactory<DeviceId, GeneralProviderDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    GeneralProviderDeviceConfig.class, CFG_SCHEME) {
                @Override
                public GeneralProviderDeviceConfig createConfig() {
                    return new GeneralProviderDeviceConfig();
                }
            };

    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private final DeviceAgentListener deviceAgentListener = new InternalDeviceAgentListener();


    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        componentConfigService.registerProperties(getClass());
        coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        deviceService.addListener(deviceListener);
        handshakers.clear();
        //This will fail if ONOS has CFG and drivers which depend on this provider
        // are activated, failing due to not finding the driver.
        cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class)
                .forEach(this::triggerConnect);
        //Initiating a periodic check to see if any device is available again and reconnect it.
        rescheduleProbeTask();
        modified(context);
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        final int oldStatsPollFrequency = statsPollFrequency;
        statsPollFrequency = Tools.getIntegerProperty(
                properties, STATS_POLL_FREQUENCY, DEFAULT_STATS_POLL_FREQUENCY);
        log.info("Configured. {} is configured to {} seconds",
                 STATS_POLL_FREQUENCY, statsPollFrequency);
        final int oldProbeFrequency = probeFrequency;
        probeFrequency = Tools.getIntegerProperty(
                properties, PROBE_FREQUENCY, DEFAULT_PROBE_FREQUENCY);
        log.info("Configured. {} is configured to {} seconds",
                 PROBE_FREQUENCY, probeFrequency);
        opTimeoutShort = Tools.getIntegerProperty(
                properties, OP_TIMEOUT_SHORT, DEFAULT_OP_TIMEOUT_SHORT);
        log.info("Configured. {} is configured to {} seconds",
                 OP_TIMEOUT_SHORT, opTimeoutShort);
        opTimeoutLong = Tools.getIntegerProperty(
                properties, OP_TIMEOUT_LONG, DEFAULT_OP_TIMEOUT_LONG);
        log.info("Configured. {} is configured to {} seconds",
                 OP_TIMEOUT_LONG, opTimeoutLong);

        if (oldStatsPollFrequency != statsPollFrequency) {
            rescheduleStatsPollingTasks();
        }

        if (oldProbeFrequency != probeFrequency) {
            rescheduleProbeTask();
        }
    }

    private synchronized void rescheduleProbeTask() {
        if (probeTask != null) {
            probeTask.cancel(false);
        }
        probeTask = probeExecutor.scheduleAtFixedRate(
                this::triggerProbeAllDevices, probeFrequency,
                probeFrequency, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        statsExecutor.shutdown();
        probeExecutor.shutdown();
        componentConfigService.unregisterProperties(getClass(), false);
        cfgService.removeListener(cfgListener);
        //Not Removing the device so they can still be used from other driver providers
        //cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class)
        //          .forEach(did -> connectionExecutor.execute(() -> disconnectDevice(did)));
        connectionExecutor.shutdown();
        deviceService.removeListener(deviceListener);
        providerRegistry.unregister(this);
        handshakers.clear();
        providerService = null;
        cfgService.unregisterConfigFactory(factory);
        log.info("Stopped");
    }

    public GeneralDeviceProvider() {
        super(new ProviderId(URI_SCHEME, DEVICE_PROVIDER_PACKAGE));
    }


    @Override
    public void triggerProbe(DeviceId deviceId) {
        connectionExecutor.execute(withDeviceLock(
                () -> doDeviceProbe(deviceId), deviceId));
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.info("Received role {} for device {}", newRole, deviceId);
        requestedRoles.put(deviceId, newRole);
        connectionExecutor.execute(() -> doRoleChanged(deviceId, newRole));
    }

    private void doRoleChanged(DeviceId deviceId, MastershipRole newRole) {
        final DeviceHandshaker handshaker = getHandshaker(deviceId);
        if (handshaker == null) {
            log.error("Null handshaker. Unable to notify new role {} to {}",
                      newRole, deviceId);
            return;
        }
        handshaker.roleChanged(newRole);
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        log.debug("Testing reachability for device {}", deviceId);
        final DeviceHandshaker handshaker = getHandshaker(deviceId);
        if (handshaker == null) {
            return false;
        }
        return getFutureWithDeadline(
                handshaker.isReachable(), "checking reachability",
                deviceId, false, opTimeoutShort);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        connectionExecutor.execute(
                () -> doChangePortState(deviceId, portNumber, enable));
    }

    private void doChangePortState(DeviceId deviceId, PortNumber portNumber,
                                   boolean enable) {
        if (!deviceService.getDevice(deviceId).is(PortAdmin.class)) {
            log.warn("Missing PortAdmin behaviour on {}, aborting port state change",
                     deviceId);
            return;
        }
        final PortAdmin portAdmin = deviceService.getDevice(deviceId)
                .as(PortAdmin.class);
        final CompletableFuture<Boolean> modifyTask = enable
                ? portAdmin.enable(portNumber)
                : portAdmin.disable(portNumber);
        final String descr = (enable ? "enabling" : "disabling") + " port " + portNumber;
        getFutureWithDeadline(
                modifyTask, descr, deviceId, null, opTimeoutShort);
    }

    @Override
    public void triggerDisconnect(DeviceId deviceId) {
        log.debug("Triggering disconnection of device {}", deviceId);
        connectionExecutor.execute(withDeviceLock(
                () -> doDisconnectDevice(deviceId), deviceId));
    }

    private DeviceHandshaker getHandshaker(DeviceId deviceId) {
        return handshakers.computeIfAbsent(deviceId, id -> {
            Driver driver = getDriver(deviceId);
            return driver == null ? null : getBehaviour(
                    driver, DeviceHandshaker.class,
                    new DefaultDriverData(driver, deviceId));
        });
    }

    private Driver getDriver(DeviceId deviceId) {
        try {
            // DriverManager checks first using basic device config.
            return driverService.getDriver(deviceId);
        } catch (ItemNotFoundException e) {
            log.error("Driver not found for {}", deviceId);
            return null;
        }
    }

    private <T extends Behaviour> T getBehaviour(Driver driver, Class<T> type,
                                                 DriverData data) {
        // Allows obtaining behavior implementations before the device is pushed
        // to the core.
        if (driver != null && driver.hasBehaviour(type)) {
            DefaultDriverHandler handler = new DefaultDriverHandler(data);
            return driver.createBehaviour(handler, type);
        } else {
            return null;
        }
    }

    private void doConnectDevice(DeviceId deviceId) {
        // Some operations can be performed by one node only.
        final boolean isLocalLeader = leadershipService.runForLeadership(
                GeneralProviderDeviceConfig.class.getName() + deviceId)
                .leader().nodeId().equals(clusterService.getLocalNode().id());

        if (deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId)) {
            log.info("Device {} is already connected to ONOS and is available",
                     deviceId);
            return;
        }
        // Retrieve config
        final GeneralProviderDeviceConfig providerConfig = cfgService.getConfig(
                deviceId, GeneralProviderDeviceConfig.class);
        final BasicDeviceConfig basicDeviceConfig = cfgService.getConfig(
                deviceId, BasicDeviceConfig.class);
        if (providerConfig == null || basicDeviceConfig == null) {
            log.error("Configuration missing, cannot connect to {}. " +
                              "basicDeviceConfig={}, generalProvider={}",
                      deviceId, basicDeviceConfig, providerConfig);
            return;
        }
        log.info("Initiating connection to device {} with driver {} ... asMaster={}",
                 deviceId, basicDeviceConfig.driver(), isLocalLeader);
        // Get handshaker, driver and driver data.
        final DeviceHandshaker handshaker = getHandshaker(deviceId);
        if (handshaker == null) {
            log.error("Missing DeviceHandshaker behavior for {}, aborting connection",
                      deviceId);
            return;
        }
        final Driver driver = handshaker.handler().driver();
        // Enhance driver data with info in GDP config.
        augmentConfigData(providerConfig, handshaker.data());
        final DriverData driverData = handshaker.data();
        // Start connection via handshaker.
        final Boolean connectSuccess = getFutureWithDeadline(
                handshaker.connect(), "initiating connection",
                deviceId, null, opTimeoutShort);
        if (connectSuccess == null) {
            // Error logged by getFutureWithDeadline().
            return;
        } else if (!connectSuccess) {
            log.warn("Unable to connect to {}", deviceId);
            return;
        }
        // Handle pipeconf (if device is capable)
        if (!handlePipeconf(deviceId, driver, driverData, isLocalLeader)) {
            // We already logged the error.
            getFutureWithDeadline(
                    handshaker.disconnect(), "performing disconnection",
                    deviceId, null, opTimeoutShort);
            return;
        }
        // Add device agent listener.
        handshaker.addDeviceAgentListener(deviceAgentListener);
        // All good. Notify core (if master).
        if (isLocalLeader) {
            advertiseDevice(deviceId, driver, providerConfig, driverData);
        }
    }


    private void advertiseDevice(DeviceId deviceId, Driver driver,
                                 GeneralProviderDeviceConfig providerConfig,
                                 DriverData driverData) {
        // Obtain device and port description and advertise device to core.
        DeviceDescription description = null;
        final List<PortDescription> ports;

        final DeviceDescriptionDiscovery deviceDiscovery = getBehaviour(
                driver, DeviceDescriptionDiscovery.class, driverData);

        if (deviceDiscovery != null) {
            description = deviceDiscovery.discoverDeviceDetails();
            ports = deviceDiscovery.discoverPortDetails();
        } else {
            log.warn("Missing DeviceDescriptionDiscovery behavior for {}, " +
                             "no update for description or ports.", deviceId);
            ports = new ArrayList<>();
        }

        if (description == null) {
            // Generate one here.
            // FIXME: a behavior impl should not return a null description
            // (e.g. as GnmiDeviceDescriptionDiscovery). This case should apply
            // only if a the behavior is not available.
            description = new DefaultDeviceDescription(
                    deviceId.uri(), Device.Type.SWITCH,
                    driver.manufacturer(), driver.hwVersion(),
                    driver.swVersion(), UNKNOWN,
                    new ChassisId(), true,
                    DefaultAnnotations.builder()
                            .set(AnnotationKeys.PROTOCOL,
                                 providerConfig.protocolsInfo().keySet().toString())
                            .build());
        }

        providerService.deviceConnected(deviceId, description);
        providerService.updatePorts(deviceId, ports);
    }

    /**
     * Handles the case of a device that is pipeline programmable. Returns true
     * if the operation wa successful and the device can be registered to the
     * core, false otherwise.
     */
    private boolean handlePipeconf(DeviceId deviceId, Driver driver,
                                   DriverData driverData, boolean asMaster) {
        final PiPipelineProgrammable pipelineProg = getBehaviour(
                driver, PiPipelineProgrammable.class, driverData);
        if (pipelineProg == null) {
            // Device is not pipeline programmable.
            return true;
        }

        final PiPipeconf pipeconf = getPipeconf(deviceId, pipelineProg);
        if (pipeconf == null) {
            return false;
        }
        final PiPipeconfId pipeconfId = pipeconf.id();

        final String mergedDriverName = piPipeconfService.mergeDriver(
                deviceId, pipeconfId);
        if (mergedDriverName == null) {
            log.error("Unable to get merged driver for {} and {}, aborting device discovery",
                      deviceId, pipeconfId);
            return false;
        }

        if (!asMaster) {
            // From now one only the master.
            return true;
        }

        if (!setDriverViaCfg(deviceId, mergedDriverName)) {
            return false;
        }

        // FIXME: we just introduced a race condition as it might happen that a
        // node does not receive the new cfg (with the merged driver) before the
        // device is advertised to the core. Perhaps we should be waiting for a
        // NetworkConfig event signaling that the driver has been updated on all
        // nodes? The effect is mitigated by deploying the pipeconf (slow
        // operation), after calling setDriverViaCfg().

        piPipeconfService.bindToDevice(pipeconfId, deviceId);

        final Boolean deploySuccess = getFutureWithDeadline(
                pipelineProg.deployPipeconf(pipeconf),
                "deploying pipeconf", deviceId, null,
                opTimeoutLong);
        if (deploySuccess == null) {
            // Error logged by getFutureWithDeadline().
            return false;
        } else if (!deploySuccess) {
            log.error("Unable to deploy pipeconf {} to {}, aborting device discovery",
                      pipeconfId, deviceId);
            return false;
        }

        return true;
    }

    private boolean setDriverViaCfg(DeviceId deviceId, String driverName) {
        BasicDeviceConfig cfg = cfgService.getConfig(deviceId, BasicDeviceConfig.class);
        if (cfg == null) {
            log.error("Unable to get basic device config for {}, aborting device discovery",
                      deviceId);
            return false;
        }
        ObjectNode newCfg = (ObjectNode) cfg.node();
        newCfg = newCfg.put(DRIVER, driverName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode newCfgNode = mapper.convertValue(newCfg, JsonNode.class);
        cfgService.applyConfig(deviceId, BasicDeviceConfig.class, newCfgNode);
        return true;
    }

    private PiPipeconf getPipeconf(DeviceId deviceId, PiPipelineProgrammable pipelineProg) {
        PiPipeconfId pipeconfId = getPipeconfFromCfg(deviceId);
        if (pipeconfId == null || pipeconfId.id().isEmpty()) {
            // No pipeconf has been provided in the cfg.
            // Check if device driver provides a default one.
            if (pipelineProg.getDefaultPipeconf().isPresent()) {
                final PiPipeconf defaultPipeconf = pipelineProg.getDefaultPipeconf().get();
                log.info("Using default pipeconf {} for {}", defaultPipeconf.id(), deviceId);
                pipeconfId = defaultPipeconf.id();
            } else {
                log.warn("Device {} is pipeline programmable but no pipeconf can be associated to it", deviceId);
                return null;
            }
        }
        // Check if registered
        if (!piPipeconfService.getPipeconf(pipeconfId).isPresent()) {
            log.warn("Pipeconf {} is not registered", pipeconfId);
            return null;
        }
        return piPipeconfService.getPipeconf(pipeconfId).get();
    }

    private void doDisconnectDevice(DeviceId deviceId) {
        log.debug("Initiating disconnection from {}...", deviceId);
        final DeviceHandshaker handshaker = handshakers.remove(deviceId);
        final boolean isAvailable = deviceService.isAvailable(deviceId);
        // Signal disconnection to core (if master).
        if (isAvailable && mastershipService.isLocalMaster(deviceId)) {
            providerService.deviceDisconnected(deviceId);
        }
        // Cancel tasks.
        cancelStatsPolling(deviceId);
        // Disconnect device.
        if (handshaker == null) {
            if (isAvailable) {
                // If not available don't bother logging. We are probably
                // invoking this method multiple times for the same device.
                log.warn("Missing DeviceHandshaker behavior for {}, " +
                                 "no guarantees of complete disconnection",
                         deviceId);
            }
            return;
        }
        handshaker.removeDeviceAgentListener(deviceAgentListener);
        final boolean disconnectSuccess = getFutureWithDeadline(
                handshaker.disconnect(), "performing disconnection",
                deviceId, false, opTimeoutShort);
        if (!disconnectSuccess) {
            log.warn("Unable to disconnect from {}", deviceId);
        }
    }

    // Needed to catch the exception in the executors since are not rethrown otherwise.
    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled Exception", e);
            }
        };
    }

    private <U> U withDeviceLock(Supplier<U> task, DeviceId deviceId) {
        final Lock lock = deviceLocks.get(deviceId);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    private Runnable withDeviceLock(Runnable task, DeviceId deviceId) {
        // Wrapper of withDeviceLock(Supplier, ...) for void tasks.
        return () -> withDeviceLock(() -> {
            task.run();
            return null;
        }, deviceId);
    }

    private void updatePortStatistics(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && deviceService.isAvailable(deviceId) &&
                device.is(PortStatisticsDiscovery.class)) {
            Collection<PortStatistics> statistics = device.as(PortStatisticsDiscovery.class)
                    .discoverPortStatistics();
            //updating statistcs only if not empty
            if (!statistics.isEmpty()) {
                providerService.updatePortStatistics(deviceId, statistics);
            }
        } else {
            log.debug("Can't update port statistics for device {}", deviceId);
        }
    }

    private boolean notMyScheme(DeviceId deviceId) {
        return !deviceId.uri().getScheme().equals(URI_SCHEME);
    }

    private void triggerConnect(DeviceId deviceId) {
        connectionExecutor.execute(withDeviceLock(
                () -> doConnectDevice(deviceId), deviceId));
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            connectionExecutor.execute(() -> consumeConfigEvent(event));
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.configClass().equals(GeneralProviderDeviceConfig.class) ||
                    event.configClass().equals(BasicDeviceConfig.class) ||
                    event.configClass().equals(PiPipeconfConfig.class)) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }

        private void consumeConfigEvent(NetworkConfigEvent event) {
            DeviceId deviceId = (DeviceId) event.subject();
            //Assuming that the deviceId comes with uri 'device:'
            if (notMyScheme(deviceId)) {
                // not under my scheme, skipping
                log.debug("{} is not my scheme, skipping", deviceId);
                return;
            }
            final boolean configComplete = withDeviceLock(
                    () -> isDeviceConfigComplete(event, deviceId), deviceId);
            if (!configComplete) {
                // Still waiting for some configuration.
                return;
            }
            // Good to go.
            triggerConnect(deviceId);
            cleanUpConfigInfo(deviceId);
        }

        private boolean isDeviceConfigComplete(NetworkConfigEvent event, DeviceId deviceId) {
            // FIXME to be removed when netcfg will issue device events in a bundle or
            // ensure all configuration needed is present
            if (event.configClass().equals(GeneralProviderDeviceConfig.class)) {
                //FIXME we currently assume that p4runtime devices are pipeline configurable.
                //If we want to connect a p4runtime device with no pipeline
                if (event.config().isPresent()) {
                    deviceConfigured.add(deviceId);
                    final boolean isNotPipelineConfigurable = Collections.disjoint(
                            ImmutableSet.copyOf(event.config().get().node().fieldNames()),
                            PIPELINE_CONFIGURABLE_PROTOCOLS);
                    if (isNotPipelineConfigurable) {
                        // Skip waiting for a pipeline if we can't support it.
                        pipelineConfigured.add(deviceId);
                    }
                }
            } else if (event.configClass().equals(BasicDeviceConfig.class)) {
                if (event.config().isPresent() && event.config().get().node().has(DRIVER)) {
                    driverConfigured.add(deviceId);
                }
            } else if (event.configClass().equals(PiPipeconfConfig.class)) {
                if (event.config().isPresent()
                        && event.config().get().node().has(PiPipeconfConfig.PIPIPECONFID)) {
                    pipelineConfigured.add(deviceId);
                }
            }

            if (deviceConfigured.contains(deviceId)
                    && driverConfigured.contains(deviceId)
                    && pipelineConfigured.contains(deviceId)) {
                return true;
            } else {
                if (deviceConfigured.contains(deviceId) && driverConfigured.contains(deviceId)) {
                    log.debug("Waiting for pipeline configuration for device {}", deviceId);
                } else if (pipelineConfigured.contains(deviceId) && driverConfigured.contains(deviceId)) {
                    log.debug("Waiting for device configuration for device {}", deviceId);
                } else if (pipelineConfigured.contains(deviceId) && deviceConfigured.contains(deviceId)) {
                    log.debug("Waiting for driver configuration for device {}", deviceId);
                } else if (driverConfigured.contains(deviceId)) {
                    log.debug("Only driver configuration for device {}", deviceId);
                } else if (deviceConfigured.contains(deviceId)) {
                    log.debug("Only device configuration for device {}", deviceId);
                }
            }
            return false;
        }
    }

    private void augmentConfigData(GeneralProviderDeviceConfig providerConfig, DriverData driverData) {
        //Storing deviceKeyId and all other config values
        // as data in the driver with protocol_<info>
        // name as the key. e.g protocol_ip
        providerConfig.protocolsInfo()
                .forEach((protocol, deviceInfoConfig) -> {
                    deviceInfoConfig.configValues()
                            .forEach((k, v) -> driverData.set(protocol + "_" + k, v));
                    driverData.set(protocol + "_key", deviceInfoConfig.deviceKeyId());
                });
    }

    private void cleanUpConfigInfo(DeviceId deviceId) {
        deviceConfigured.remove(deviceId);
        driverConfigured.remove(deviceId);
        pipelineConfigured.remove(deviceId);
    }

    private void startStatsPolling(DeviceId deviceId, boolean withRandomDelay) {
        statsPollingTasks.compute(deviceId, (did, oldTask) -> {
            if (oldTask != null) {
                oldTask.cancel(false);
            }
            final int delay = withRandomDelay
                    ? new SecureRandom().nextInt(10) : 0;
            return statsExecutor.scheduleAtFixedRate(
                    exceptionSafe(() -> updatePortStatistics(deviceId)),
                    delay, statsPollFrequency, TimeUnit.SECONDS);
        });
    }

    private void cancelStatsPolling(DeviceId deviceId) {
        statsPollingTasks.computeIfPresent(deviceId, (did, task) -> {
            task.cancel(false);
            return null;
        });
    }

    private void rescheduleStatsPollingTasks() {
        statsPollingTasks.keySet().forEach(deviceId -> {
            // startStatsPolling cancels old one if present.
            startStatsPolling(deviceId, true);
        });
    }

    private void triggerProbeAllDevices() {
        // Async trigger a task for all devices in the cfg.
        cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class)
                .forEach(deviceId -> connectionExecutor.execute(withDeviceLock(
                        () -> doDeviceProbe(deviceId), deviceId)));
    }

    private PiPipeconfId getPipeconfFromCfg(DeviceId deviceId) {
        PiPipeconfConfig config = cfgService.getConfig(
                deviceId, PiPipeconfConfig.class);
        if (config == null) {
            return null;
        }
        return config.piPipeconfId();
    }

    private void doDeviceProbe(DeviceId deviceId) {
        if (!configIsPresent(deviceId)) {
            return;
        }
        final boolean isAvailable = deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId);
        final boolean isLocalMaster = mastershipService.isLocalMaster(deviceId);
        if (isAvailable) {
            if (!isLocalMaster) {
                return;
            }
            if (!isReachable(deviceId)) {
                log.info("Disconnecting available but unreachable device {}...",
                         deviceId);
                triggerDisconnect(deviceId);
            }
        } else {
            // We do not check for reachability using isReachable()
            // since the behaviour of this method can vary depending on protocol
            // nuances. We leave this check to the device handshaker at later
            // stages of the connection process.
            triggerConnect(deviceId);
        }
    }

    private boolean configIsPresent(DeviceId deviceId) {
        final boolean present =
                cfgService.getConfig(deviceId, GeneralProviderDeviceConfig.class) != null
                        && cfgService.getConfig(deviceId, BasicDeviceConfig.class) != null;
        if (!present) {
            log.warn("Configuration for device {} is not complete", deviceId);
        }
        return present;
    }

    private void handleChannelClosed(DeviceId deviceId) {
        log.info("Disconnecting device {}, due to channel closed event",
                 deviceId);
        triggerDisconnect(deviceId);
    }

    private void handleMastershipResponse(DeviceId deviceId, MastershipRole response) {
        //Notify core about response.
        if (!requestedRoles.containsKey(deviceId)) {
            return;
        }
        providerService.receivedRoleReply(deviceId, requestedRoles.get(deviceId), response);
        if (response.equals(MastershipRole.MASTER)) {
            startStatsPolling(deviceId, false);
        } else {
            cancelStatsPolling(deviceId);
        }
    }

    private <U> U getFutureWithDeadline(CompletableFuture<U> future, String opDescription,
                                        DeviceId deviceId, U defaultValue, int timeout) {
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Thread interrupted while {} on {}", opDescription, deviceId);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Exception while {} on {}", opDescription, deviceId, e.getCause());
        } catch (TimeoutException e) {
            log.error("Operation TIMEOUT while {} on {}", opDescription, deviceId);
        }
        return defaultValue;
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            // For now this is scheduled periodically, when streaming API will
            // be available we check and base it on the streaming API (e.g. gNMI)
            if (mastershipService.isLocalMaster(deviceId)) {
                startStatsPolling(deviceId, true);
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type() == Type.DEVICE_ADDED &&
                    event.subject().id().toString().startsWith(URI_SCHEME.toLowerCase());
        }
    }

    /**
     * Listener for device agent events.
     */
    private class InternalDeviceAgentListener implements DeviceAgentListener {

        @Override
        public void event(DeviceAgentEvent event) {
            DeviceId deviceId = event.subject();
            switch (event.type()) {
                case CHANNEL_OPEN:
                    // Ignore.
                    break;
                case CHANNEL_CLOSED:
                    handleChannelClosed(deviceId);
                    break;
                case CHANNEL_ERROR:
                    // TODO evaluate other reaction to channel error.
                    log.warn("Received CHANNEL_ERROR from {}. Is the channel still open?",
                             deviceId);
                    break;
                case ROLE_MASTER:
                    handleMastershipResponse(deviceId, MastershipRole.MASTER);
                    break;
                case ROLE_STANDBY:
                    handleMastershipResponse(deviceId, MastershipRole.STANDBY);
                    break;
                case ROLE_NONE:
                    handleMastershipResponse(deviceId, MastershipRole.NONE);
                    break;
                default:
                    log.warn("Unrecognized device agent event {}", event.type());
            }
        }

    }
}
