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

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
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
import org.onosproject.cluster.NodeId;
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
import java.util.Objects;
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
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newScheduledThreadPool;
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

    // Timeout in seconds for operations on devices.
    private static final int DEVICE_OP_TIMEOUT = 10;

    private static final String DRIVER = "driver";
    public static final String FIRST_CONNECTION_TOPIC = "first-connection-";
    private static final String CHECK_CONNECTION_TOPIC = "check-connection-";
    private static final String POLL_FREQUENCY = "pollFrequency";

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

    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 10;
    @Property(name = POLL_FREQUENCY, intValue = DEFAULT_POLL_FREQUENCY_SECONDS,
            label = "Configure poll frequency for port status and statistics; " +
                    "default is 10 sec")
    private int pollFrequency = DEFAULT_POLL_FREQUENCY_SECONDS;

    private static final int DEVICE_AVAILABILITY_POLL_FREQUENCY_SECONDS = 10;
    @Property(name = "deviceAvailabilityPollFrequency", intValue = DEVICE_AVAILABILITY_POLL_FREQUENCY_SECONDS,
            label = "Configure poll frequency for checking device availability; " +
                    "default is 10 sec")
    private int deviceAvailabilityPollFrequency = DEVICE_AVAILABILITY_POLL_FREQUENCY_SECONDS;

    private static final String APP_NAME = "org.onosproject.generaldeviceprovider";
    private static final String URI_SCHEME = "device";
    private static final String CFG_SCHEME = "generalprovider";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.general.provider.device";
    private static final int CORE_POOL_SIZE = 10;
    private static final String UNKNOWN = "unknown";

    //FIXME this will be removed when the configuration is synced at the source.
    private static final Set<String> PIPELINE_CONFIGURABLE_PROTOCOLS = ImmutableSet.of("p4runtime");

    private static final ConcurrentMap<DeviceId, Lock> DEVICE_LOCKS = Maps.newConcurrentMap();
    //FIXME to be removed when netcfg will issue device events in a bundle or
    //ensures all configuration needed is present
    private Set<DeviceId> deviceConfigured = new CopyOnWriteArraySet<>();
    private Set<DeviceId> driverConfigured = new CopyOnWriteArraySet<>();
    private Set<DeviceId> pipelineConfigured = new CopyOnWriteArraySet<>();

    private final Map<DeviceId, DeviceHandshaker> handshakers = Maps.newConcurrentMap();

    private final Map<DeviceId, MastershipRole> requestedRoles = Maps.newConcurrentMap();


    private ExecutorService connectionExecutor
            = newFixedThreadPool(CORE_POOL_SIZE, groupedThreads(
            "onos/generaldeviceprovider-device-connect", "%d", log));
    private ScheduledExecutorService portStatsExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE, groupedThreads(
            "onos/generaldeviceprovider-port-stats", "%d", log));
    private ScheduledExecutorService availabilityCheckExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE, groupedThreads(
            "onos/generaldeviceprovider-availability-check", "%d", log));
    private ConcurrentMap<DeviceId, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

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
                .forEach(did -> triggerConnectWithLeadership(
                        did, FIRST_CONNECTION_TOPIC + did.toString()));
        //Initiating a periodic check to see if any device is available again and reconnect it.
        availabilityCheckExecutor.scheduleAtFixedRate(
                this::scheduleDevicePolling, deviceAvailabilityPollFrequency,
                deviceAvailabilityPollFrequency, TimeUnit.SECONDS);
        modified(context);
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            pollFrequency = Tools.getIntegerProperty(properties, POLL_FREQUENCY,
                                                     DEFAULT_POLL_FREQUENCY_SECONDS);
            log.info("Configured. Poll frequency is configured to {} seconds", pollFrequency);
        }

        if (!scheduledTasks.isEmpty()) {
            //cancel all previous tasks
            scheduledTasks.values().forEach(task -> task.cancel(false));
            //resubmit task with new timeout.
            Set<DeviceId> deviceSubjects =
                    cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class);
            deviceSubjects.forEach(deviceId -> {
                if (notMyScheme(deviceId)) {
                    // not under my scheme, skipping
                    log.debug("{} is not my scheme, skipping", deviceId);
                    return;
                }
                scheduledTasks.put(deviceId, scheduleStatsPolling(deviceId, true));
            });
        }
    }

    @Deactivate
    public void deactivate() {
        portStatsExecutor.shutdown();
        availabilityCheckExecutor.shutdown();
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
        // TODO Really don't see the point of this in non OF Context,
        // for now testing reachability, can be moved to no-op
        log.debug("Triggering probe equals testing reachability on device {}", deviceId);
        isReachable(deviceId);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.info("Received role {} for device {}", newRole, deviceId);
        requestedRoles.put(deviceId, newRole);
        connectionExecutor.submit(exceptionSafe(
                () -> doRoleChanged(deviceId, newRole)));
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
                deviceId, false);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        if (!deviceService.getDevice(deviceId).is(PortAdmin.class)) {
            log.warn("Missing PortAdmin behaviour on {}, aborting port state change",
                     deviceId);
            return;
        }
        final PortAdmin portAdmin = getPortAdmin(deviceId);
        final CompletableFuture<Boolean> modified = enable
                ? portAdmin.enable(portNumber)
                : portAdmin.disable(portNumber);
        modified.thenAcceptAsync(result -> {
            if (!result) {
                log.warn("Port {} status cannot be changed on {} (enable={})",
                         portNumber, deviceId, enable);
            }
        });
    }

    @Override
    public void triggerDisconnect(DeviceId deviceId) {
        log.debug("Triggering disconnection of device {}", deviceId);
        connectionExecutor.execute(
                () -> disconnectDevice(deviceId)
                        .thenRunAsync(() -> checkAndConnect(deviceId)));
    }

    private DeviceHandshaker getHandshaker(DeviceId deviceId) {
        return handshakers.computeIfAbsent(deviceId, id -> {
            Driver driver = getDriver(deviceId);
            return driver == null ? null :
                    getBehaviour(driver, DeviceHandshaker.class,
                                 new DefaultDriverData(driver, deviceId));
        });
    }

    private PortAdmin getPortAdmin(DeviceId deviceId) {
        Driver driver = getDriver(deviceId);
        return getBehaviour(driver, PortAdmin.class,
                            new DefaultDriverData(driver, deviceId));

    }

    private Driver getDriver(DeviceId deviceId) {
        Driver driver = null;
        try {
            driver = driverService.getDriver(deviceId);
        } catch (ItemNotFoundException e) {
            log.debug("Falling back to configuration to fetch driver " +
                              "for device {}", deviceId);
            BasicDeviceConfig cfg = cfgService.getConfig(deviceId, BasicDeviceConfig.class);
            if (cfg != null) {
                driver = driverService.getDriver(cfg.driver());
            }
        }
        return driver;
    }

    //needed since the device manager will not return the driver through implementation()
    // method since the device is not pushed to the core so for the connectDeviceAsMaster
    // we need to work around that in order to test before calling
    // store.createOrUpdateDevice
    private <T extends Behaviour> T getBehaviour(Driver driver, Class<T> type,
                                                 DriverData data) {
        if (driver != null && driver.hasBehaviour(type)) {
            DefaultDriverHandler handler = new DefaultDriverHandler(data);
            return driver.createBehaviour(handler, type);
        } else {
            return null;
        }
    }

    private void doConnectDevice(DeviceId deviceId, boolean asMaster) {
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
                 deviceId, basicDeviceConfig.driver(), asMaster);
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
                deviceId, null);
        if (connectSuccess == null) {
            // Error logged by getFutureWithDeadline().
            return;
        } else if (!connectSuccess) {
            log.warn("Unable to connect to {}", deviceId);
            return;
        }
        // Handle pipeconf (if device is capable)
        if (!handlePipeconf(deviceId, driver, driverData, asMaster)) {
            // We already logged the error.
            handshaker.disconnect();
            return;
        }
        // Add device agent listener.
        handshaker.addDeviceAgentListener(deviceAgentListener);
        // All good. Notify core (if master).
        if (asMaster) {
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
                                   DriverData driverData, boolean deployPipeconf) {
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

        if (deployPipeconf) {
            final Boolean deploySuccess = getFutureWithDeadline(
                    pipelineProg.deployPipeconf(pipeconf),
                    "deploying pipeconf", deviceId, null);
            if (deploySuccess == null) {
                // Error logged by getFutureWithDeadline().
                return false;
            } else if (!deploySuccess) {
                log.error("Unable to deploy pipeconf {} to {}, aborting device discovery",
                          pipeconfId, deviceId);
                return false;
            }
        }

        final Boolean mergeSuccess = getFutureWithDeadline(
                piPipeconfService.bindToDevice(pipeconfId, deviceId),
                "merging driver", deviceId, null);
        if (mergeSuccess == null) {
            // Error logged by getFutureWithDeadline().
            return false;
        } else if (!mergeSuccess) {
            log.error("Unable to merge pipeconf driver for {}, aborting device discovery",
                      pipeconfId, deviceId);
            return false;
        }

        return true;
    }

    private PiPipeconf getPipeconf(DeviceId deviceId, PiPipelineProgrammable pipelineProg) {
        PiPipeconfId pipeconfId = piPipeconfService.ofDevice(deviceId).orElseGet(() -> {
            // No pipeconf has been associated with this device.
            // Check if device driver provides a default one.
            if (pipelineProg.getDefaultPipeconf().isPresent()) {
                PiPipeconf defaultPipeconf = pipelineProg.getDefaultPipeconf().get();
                log.info("Using default pipeconf {} for {}", defaultPipeconf.id(), deviceId);
                return defaultPipeconf.id();
            } else {
                return null;
            }
        });
        if (pipeconfId == null) {
            log.warn("Device {} is pipeline programmable but no pipeconf can be associated to it", deviceId);
            return null;
        }
        if (!piPipeconfService.getPipeconf(pipeconfId).isPresent()) {
            log.warn("Pipeconf {} is not registered", pipeconfId);
            return null;
        }
        return piPipeconfService.getPipeconf(pipeconfId).get();
    }

    private CompletableFuture<?> disconnectDevice(DeviceId deviceId) {
        log.info("Disconnecting for device {}", deviceId);
        // Remove from core (if master)
        if (mastershipService.isLocalMaster(deviceId)) {
            providerService.deviceDisconnected(deviceId);
        }
        // Cancel tasks
        if (scheduledTasks.containsKey(deviceId)) {
            scheduledTasks.remove(deviceId).cancel(true);
        }
        // Perform disconnection with device.
        final DeviceHandshaker handshaker = handshakers.remove(deviceId);
        if (handshaker == null) {
            // Gracefully ignore
            log.warn("Missing DeviceHandshaker behavior for {}, " +
                             "no guarantees of complete disconnection",
                     deviceId);
            return CompletableFuture.completedFuture(false);
        }
        handshaker.removeDeviceAgentListener(deviceAgentListener);
        return handshaker.disconnect()
                .thenApplyAsync(result -> {
                    if (result) {
                        log.info("Disconnected device {}", deviceId);
                    } else {
                        log.warn("Device {} was unable to disconnect", deviceId);
                    }
                    return result;
                });
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

    private Runnable withDeviceLock(Runnable runnable, DeviceId deviceId) {
        return () -> {
            Lock lock = DEVICE_LOCKS.computeIfAbsent(deviceId, key -> new ReentrantLock());
            lock.lock();
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        };
    }

    private void updatePortStatistics(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (!Objects.isNull(device) && deviceService.isAvailable(deviceId) &&
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

    private void triggerConnectWithLeadership(DeviceId deviceId,
                                              String leadershipTopic) {
        final NodeId leaderNodeId = leadershipService.runForLeadership(
                leadershipTopic).leader().nodeId();
        final boolean thisNodeMaster = clusterService
                .getLocalNode().id().equals(leaderNodeId);
        connectionExecutor.submit(withDeviceLock(exceptionSafe(
                () -> doConnectDevice(deviceId, thisNodeMaster)), deviceId));
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            DeviceId deviceId = (DeviceId) event.subject();
            //Assuming that the deviceId comes with uri 'device:'
            if (notMyScheme(deviceId)) {
                // not under my scheme, skipping
                log.debug("{} is not my scheme, skipping", deviceId);
                return;
            }
            if (!isDeviceConfigComplete(event, deviceId)) {
                // Still waiting for some configuration.
                return;
            }
            // Good to go.
            triggerConnectWithLeadership(
                    deviceId, FIRST_CONNECTION_TOPIC + deviceId.toString());
            cleanUpConfigInfo(deviceId);
        }

        private boolean isDeviceConfigComplete(NetworkConfigEvent event, DeviceId deviceId) {
            // FIXME to be removed when netcfg will issue device events in a bundle or
            // ensure all configuration needed is present
            Lock lock = DEVICE_LOCKS.computeIfAbsent(deviceId, key -> new ReentrantLock());
            lock.lock();
            try {
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
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.configClass().equals(GeneralProviderDeviceConfig.class) ||
                    event.configClass().equals(BasicDeviceConfig.class) ||
                    event.configClass().equals(PiPipeconfConfig.class)) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
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

    private ScheduledFuture<?> scheduleStatsPolling(DeviceId deviceId, boolean randomize) {
        int delay = 0;
        if (randomize) {
            delay = new SecureRandom().nextInt(10);
        }
        return portStatsExecutor.scheduleAtFixedRate(
                exceptionSafe(() -> updatePortStatistics(deviceId)),
                delay, pollFrequency, TimeUnit.SECONDS);
    }

    private void scheduleDevicePolling() {
        cfgService.getSubjects(DeviceId.class, GeneralProviderDeviceConfig.class).forEach(this::checkAndConnect);
    }

    private void checkAndConnect(DeviceId deviceId) {
        // Let's try and reconnect to a device which is stored in cfg.
        // One of the following conditions must be satisfied:
        // 1) device is null in the store meaning that is was never connected or
        // it was administratively removed
        // 2) the device is not available and there is no MASTER instance,
        // meaning the device lost it's connection to ONOS at some point in the
        // past.
        // We also check that the general device provider config and the driver
        // config are present. We do not check for reachability using
        // isReachable(deviceId) since the behaviour of this method can vary
        // depending on protocol nuances. We leave this check to the device
        // handshaker at later stages of the connection process. IF the
        // conditions are not met but instead the device is present in the
        // store, available and this instance is MASTER but is not reachable we
        // remove it from the store.

        if ((deviceService.getDevice(deviceId) == null
                || (!deviceService.isAvailable(deviceId)
                && mastershipService.getMasterFor(deviceId) == null))
                && configIsPresent(deviceId)) {
            log.debug("Trying to re-connect to device {}", deviceId);
            triggerConnectWithLeadership(
                    deviceId, CHECK_CONNECTION_TOPIC + deviceId.toString());
            cleanUpConfigInfo(deviceId);
        } else if (deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId)
                && mastershipService.isLocalMaster(deviceId)
                && !isReachable(deviceId)
                && configIsPresent(deviceId)) {
            log.info("Removing available but unreachable device {}", deviceId);
            disconnectDevice(deviceId);
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
        disconnectDevice(deviceId);
    }

    private void handleMastershipResponse(DeviceId deviceId, MastershipRole response) {
        //Notify core about response.
        if (!requestedRoles.containsKey(deviceId)) {
            return;
        }
        providerService.receivedRoleReply(deviceId, requestedRoles.get(deviceId), response);
        // If not master, cancel polling tasks, otherwise start them.
        if (!response.equals(MastershipRole.MASTER)
                && scheduledTasks.get(deviceId) != null) {
            scheduledTasks.remove(deviceId).cancel(false);
        } else if (response.equals(MastershipRole.MASTER)
                && scheduledTasks.get(deviceId) == null) {
            scheduledTasks.put(deviceId, scheduleStatsPolling(deviceId, false));
            updatePortStatistics(deviceId);
        }
    }

    private <U> U getFutureWithDeadline(CompletableFuture<U> future, String opDescription,
                                        DeviceId deviceId, U defaultValue) {
        try {
            return future.get(DEVICE_OP_TIMEOUT, TimeUnit.SECONDS);
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
                scheduledTasks.put(deviceId, scheduleStatsPolling(deviceId, false));
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
