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
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.ChannelEvent;
import org.onosproject.net.device.ChannelListener;
import org.onosproject.net.device.DefaultDeviceDescription;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.device.DeviceEvent.Type;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses drivers to detect device and do initial handshake
 * and channel establishment with devices. Any other provider specific operation
 * is also delegated to the DeviceHandshaker driver.
 */
@Beta
@Component(immediate = true)
public class GeneralDeviceProvider extends AbstractProvider
        implements DeviceProvider {
    public static final String DRIVER = "driver";
    public static final int REACHABILITY_TIMEOUT = 10;
    public static final String DEPLOY = "deploy-";
    public static final String PIPECONF_TOPIC = "-pipeconf";
    public static final String CHECK = "check-";
    public static final String CONNECTION = "-connection";
    private static final String POLL_FREQUENCY = "pollFrequency";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PiPipeconfService piPipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

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

    protected static final String APP_NAME = "org.onosproject.generaldeviceprovider";
    protected static final String URI_SCHEME = "device";
    protected static final String CFG_SCHEME = "generalprovider";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.general.provider.device";
    private static final int CORE_POOL_SIZE = 10;
    private static final String UNKNOWN = "unknown";

    //FIXME this will be removed when the configuration is synced at the source.
    private static final Set<String> PIPELINE_CONFIGURABLE_PROTOCOLS = ImmutableSet.of("p4runtime");

    private static final ConcurrentMap<DeviceId, Lock> ENTRY_LOCKS = Maps.newConcurrentMap();
    //FIXME to be removed when netcfg will issue device events in a bundle or
    //ensures all configuration needed is present
    private Set<DeviceId> deviceConfigured = new CopyOnWriteArraySet<>();
    private Set<DeviceId> driverConfigured = new CopyOnWriteArraySet<>();
    private Set<DeviceId> pipelineConfigured = new CopyOnWriteArraySet<>();

    private final Map<DeviceId, DeviceHandshaker> handshakers = Maps.newConcurrentMap();


    protected ScheduledExecutorService connectionExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/generaldeviceprovider-device",
                    "connection-executor-%d", log));
    protected ScheduledExecutorService portStatsExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/generaldeviceprovider-port-stats",
                    "port-stats-executor-%d", log));
    protected ScheduledExecutorService availabilityCheckExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/generaldeviceprovider-availability-check",
                    "availability-check-executor-%d", log));
    protected ConcurrentMap<DeviceId, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    protected DeviceProviderService providerService;
    private InternalDeviceListener deviceListener = new InternalDeviceListener();

    protected final ConfigFactory factory =
            new ConfigFactory<DeviceId, GeneralProviderDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    GeneralProviderDeviceConfig.class, CFG_SCHEME) {
                @Override
                public GeneralProviderDeviceConfig createConfig() {
                    return new GeneralProviderDeviceConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private ChannelListener channelListener = new InternalChannelListener();


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
                .forEach(did -> connectionExecutor.execute(() -> connectDevice(did)));
        //Initiating a periodic check to see if any device is available again and reconnect it.
        availabilityCheckExecutor.scheduleAtFixedRate(this::scheduleDevicePolling, deviceAvailabilityPollFrequency,
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
                if (!compareScheme(deviceId)) {
                    // not under my scheme, skipping
                    log.debug("{} is not my scheme, skipping", deviceId);
                    return;
                }
                scheduledTasks.put(deviceId, scheduleStatistcsPolling(deviceId, true));
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
        CompletableFuture<MastershipRole> roleReply = getHandshaker(deviceId).roleChanged(newRole);
        roleReply.thenAcceptAsync(mastership -> {
            providerService.receivedRoleReply(deviceId, newRole, mastership);
            if (!mastership.equals(MastershipRole.MASTER) && scheduledTasks.get(deviceId) != null) {
                scheduledTasks.get(deviceId).cancel(false);
                scheduledTasks.remove(deviceId);
            } else if (mastership.equals(MastershipRole.MASTER) && scheduledTasks.get(deviceId) == null) {
                scheduledTasks.put(deviceId, scheduleStatistcsPolling(deviceId, false));
                updatePortStatistics(deviceId);
            }
        });
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        log.debug("Testing reachability for device {}", deviceId);

        DeviceHandshaker handshaker = getHandshaker(deviceId);
        if (handshaker == null) {
            return false;
        }

        CompletableFuture<Boolean> reachable = handshaker.isReachable();
        try {
            return reachable.get(REACHABILITY_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Device {} is not reachable {}", deviceId, e.getMessage());
            log.debug("Exception", e);
            return false;
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        if (deviceService.getDevice(deviceId).is(PortAdmin.class)) {

            PortAdmin portAdmin = getPortAdmin(deviceId);
            CompletableFuture<Boolean> modified;
            if (enable) {
                modified = portAdmin.enable(portNumber);
            } else {
                modified = portAdmin.disable(portNumber);
            }
            modified.thenAcceptAsync(result -> {
                if (!result) {
                    log.warn("Your device {} port {} status can't be changed to {}",
                            deviceId, portNumber, enable);
                }
            });

        } else {
            log.warn("Device {} does not support PortAdmin behaviour", deviceId);
        }
    }

    @Override
    public void triggerDisconnect(DeviceId deviceId) {
        log.debug("Triggering disconnection of device {}", deviceId);
        connectionExecutor.execute(() -> {
            disconnectDevice(deviceId).whenComplete((success, ex) -> {
                checkAndConnect(deviceId);
            });
        });
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
    // method since the device is not pushed to the core so for the connectDevice
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

    //Connects a general device
    private void connectDevice(DeviceId deviceId) {
        //retrieve the configuration
        GeneralProviderDeviceConfig providerConfig =
                cfgService.getConfig(deviceId, GeneralProviderDeviceConfig.class);
        BasicDeviceConfig basicDeviceConfig =
                cfgService.getConfig(deviceId, BasicDeviceConfig.class);

        if (providerConfig == null || basicDeviceConfig == null) {
            log.error("Configuration is NULL: basic config {}, general provider " +
                    "config {}", basicDeviceConfig, providerConfig);
        } else {
            log.info("Connecting to device {} with driver {}", deviceId, basicDeviceConfig.driver());

            DeviceHandshaker handshaker = getHandshaker(deviceId);
            if (handshaker == null) {
                log.error("Device {} does not support DeviceHandshaker behaviour", deviceId);
                return;
            }
            Driver driver = handshaker.handler().driver();

            addConfigData(providerConfig, handshaker.data());

            //Connecting to the device
            CompletableFuture<Boolean> connected = handshaker.connect();

            connected.thenAcceptAsync(result -> {
                if (result) {
                    handshaker.addChannelListener(channelListener);
                    //Populated with the default values obtained by the driver
                    ChassisId cid = new ChassisId();
                    SparseAnnotations annotations = DefaultAnnotations.builder()
                            .set(AnnotationKeys.PROTOCOL,
                                    providerConfig.protocolsInfo().keySet().toString())
                            .build();
                    DeviceDescription description =
                            new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                                    driver.manufacturer(), driver.hwVersion(),
                                    driver.swVersion(), UNKNOWN,
                                    cid, true, annotations);
                    //Empty list of ports
                    List<PortDescription> ports = new ArrayList<>();

                    DeviceDescriptionDiscovery deviceDiscovery = getBehaviour(driver,
                            DeviceDescriptionDiscovery.class, handshaker.data());
                    if (deviceDiscovery != null) {
                        DeviceDescription newdescription = deviceDiscovery.discoverDeviceDetails();
                        if (newdescription != null) {
                            description = newdescription;
                        }
                        ports = deviceDiscovery.discoverPortDetails();
                    } else {
                        log.info("No Device Description Discovery for device {}, no update for " +
                                "description or ports.", deviceId);
                    }

                    if (!handlePipeconf(deviceId, driver, handshaker.data(), true)) {
                        // Something went wrong during handling of pipeconf.
                        // We already logged the error.
                        handshaker.disconnect();
                        return;
                    }
                    advertiseDevice(deviceId, description, ports);

                } else {
                    log.warn("Can't connect to device {}", deviceId);
                }
            });
        }
    }

    private void connectStandbyDevice(DeviceId deviceId) {
        // if device is pipeline programmable we merge pipeconf + base driver for every other role
        GeneralProviderDeviceConfig providerConfig =
                cfgService.getConfig(deviceId, GeneralProviderDeviceConfig.class);

        DeviceHandshaker handshaker = getHandshaker(deviceId);
        if (handshaker == null) {
            log.error("Device {} does not support DeviceHandshaker behaviour", deviceId);
            return;
        }
        addConfigData(providerConfig, handshaker.data());

        //Connecting to the device
        handshaker.connect().thenAcceptAsync(result -> {
            if (result) {
                handshaker.addChannelListener(channelListener);
                handlePipeconf(deviceId, handshaker.handler().driver(), handshaker.data(), false);
            }
        });
    }

    /**
     * Handles the case of a device that is pipeline programmable. Returns true if the operation wa successful and the
     * device can be registered to the core, false otherwise.
     */
    private boolean handlePipeconf(DeviceId deviceId, Driver driver, DriverData driverData, boolean deployPipeconf) {
        PiPipelineProgrammable pipelineProg =
                getBehaviour(driver, PiPipelineProgrammable.class, driverData);

        if (pipelineProg == null) {
            // Device is not pipeline programmable.
            return true;
        }

        PiPipeconf pipeconf = getPipeconf(deviceId, pipelineProg);

        if (pipeconf != null) {
            PiPipeconfId pipeconfId = pipeconf.id();

            try {
                if (deployPipeconf) {
                    if (!pipelineProg.deployPipeconf(pipeconf).get()) {
                        log.error("Unable to deploy pipeconf {} to {}, aborting device discovery",
                                  pipeconfId, deviceId);
                        return false;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Exception occurred while deploying pipeconf {} to device {}", pipeconf.id(), deviceId, e);
                return false;
            }
            try {
                if (!piPipeconfService.bindToDevice(pipeconfId, deviceId).get()) {
                    log.error("Unable to merge driver {} for device {} with pipeconf {}, aborting device discovery",
                              driver.name(), deviceId, pipeconfId);
                    return false;
                }
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Exception occurred while binding pipeconf {} to device {}", pipeconf.id(), deviceId, e);
                return false;
            }
        } else {
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
            log.warn("Device {} is pipeline programmable but no pipeconf can be associated to it.", deviceId);
            return null;
        }

        if (!piPipeconfService.getPipeconf(pipeconfId).isPresent()) {
            log.warn("Pipeconf {} is not registered", pipeconfId);
            return null;
        }

        return piPipeconfService.getPipeconf(pipeconfId).get();
    }

    private void advertiseDevice(DeviceId deviceId, DeviceDescription description, List<PortDescription> ports) {
        providerService.deviceConnected(deviceId, description);
        providerService.updatePorts(deviceId, ports);
    }

    private CompletableFuture<Boolean> disconnectDevice(DeviceId deviceId) {
        log.info("Disconnecting for device {}", deviceId);

        CompletableFuture<Boolean> disconnectError = new CompletableFuture<>();

        DeviceHandshaker handshaker = handshakers.remove(deviceId);
        if (handshaker != null) {
            handshaker.disconnect().thenAcceptAsync(result -> {
                if (result) {
                    log.info("Disconnected device {}", deviceId);
                    providerService.deviceDisconnected(deviceId);
                    disconnectError.complete(true);
                } else {
                    disconnectError.complete(false);
                    log.warn("Device {} was unable to disconnect", deviceId);
                }
            });
        } else {
            //gracefully ignoring.
            log.warn("No DeviceHandshaker for device {}, no guarantees of complete " +
                    "shutdown of communication", deviceId);
            disconnectError.complete(false);
        }
        ScheduledFuture<?> pollingStatisticsTask = scheduledTasks.get(deviceId);
        if (pollingStatisticsTask != null) {
            pollingStatisticsTask.cancel(true);
            scheduledTasks.remove(deviceId);
        }
        return disconnectError;
    }

    //Needed to catch the exception in the executors since are not rethrown otherwise.
    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled Exception", e);
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

    private boolean compareScheme(DeviceId deviceId) {
        return deviceId.uri().getScheme().equals(URI_SCHEME);
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            DeviceId deviceId = (DeviceId) event.subject();
            //Assuming that the deviceId comes with uri 'device:'
            if (!compareScheme(deviceId)) {
                // not under my scheme, skipping
                log.debug("{} is not my scheme, skipping", deviceId);
                return;
            }
            if (deviceService.getDevice(deviceId) != null && deviceService.isAvailable(deviceId)) {
                log.info("Device {} is already connected to ONOS and is available", deviceId);
                return;
            }
            NodeId leaderNodeId = leadershipService.runForLeadership(DEPLOY + deviceId.toString() + PIPECONF_TOPIC)
                    .leader().nodeId();
            NodeId localNodeId = clusterService.getLocalNode().id();
            if (localNodeId.equals(leaderNodeId)) {
                if (processEvent(event, deviceId)) {
                    log.debug("{} is leader for {}, initiating the connection and deploying pipeline", leaderNodeId,
                            deviceId);
                    checkAndSubmitDeviceTask(deviceId);
                }
            } else {
                if (processEvent(event, deviceId)) {
                    log.debug("{} is not leader for {}, initiating connection but not deploying pipeline, {} is LEADER",
                            localNodeId, deviceId, leaderNodeId);
                    connectionExecutor.submit(exceptionSafe(() -> connectStandbyDevice(deviceId)));
                    //FIXME this will be removed when config is synced
                    cleanUpConfigInfo(deviceId);
                }
            }
        }

        private boolean processEvent(NetworkConfigEvent event, DeviceId deviceId) {
            //FIXME to be removed when netcfg will issue device events in a bundle or
            // ensure all configuration needed is present
            Lock lock = ENTRY_LOCKS.computeIfAbsent(deviceId, key -> new ReentrantLock());
            lock.lock();
            try {
                if (event.configClass().equals(GeneralProviderDeviceConfig.class)) {
                    //FIXME we currently assume that p4runtime devices are pipeline configurable.
                    //If we want to connect a p4runtime device with no pipeline
                    if (event.config().isPresent() &&
                            Collections.disjoint(ImmutableSet.copyOf(event.config().get().node().fieldNames()),
                                    PIPELINE_CONFIGURABLE_PROTOCOLS)) {
                        pipelineConfigured.add(deviceId);
                    }
                    deviceConfigured.add(deviceId);
                } else if (event.configClass().equals(BasicDeviceConfig.class)) {
                    if (event.config().isPresent() && event.config().get().node().has(DRIVER)) {
                        //TODO add check for pipeline and add it to the pipeline list if no
                        // p4runtime is present.
                        driverConfigured.add(deviceId);
                    }
                } else if (event.configClass().equals(PiPipeconfConfig.class)) {
                    if (event.config().isPresent()
                            && event.config().get().node().has(PiPipeconfConfig.PIPIPECONFID)) {
                        pipelineConfigured.add(deviceId);
                    }
                }
                //if the device has no "pipeline configurable protocol it will be present
                // in the pipelineConfigured
                if (deviceConfigured.contains(deviceId) && driverConfigured.contains(deviceId)
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

    private void checkAndSubmitDeviceTask(DeviceId deviceId) {
        connectionExecutor.submit(exceptionSafe(() -> connectDevice(deviceId)));
        //FIXME this will be removed when configuration is synced.
        cleanUpConfigInfo(deviceId);

    }

    private void addConfigData(GeneralProviderDeviceConfig providerConfig, DriverData driverData) {
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

    private ScheduledFuture<?> scheduleStatistcsPolling(DeviceId deviceId, boolean randomize) {
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
        // Let's try and reconnect to a device which is stored in the net-cfg.
        // One of the following conditions must be satisfied:
        // 1) device is null in the store meaning that is was never connected or it was administratively removed
        // 2) the device is not available and there is no MASTER instance, meaning the device lost
        //    it's connection to ONOS at some point in the past.
        // We also check that the general device provider config and the driver config are present.
        // We do not check for reachability using isReachable(deviceId) since the behaviour of this method
        // can vary depending on protocol nuances. We leave this check to the device handshaker
        // at later stages of the connection process.
        // IF the conditions are not met but instead the device is present in the store, available and this instance is
        // MASTER but is not reachable we remove it from the store.

        if ((deviceService.getDevice(deviceId) == null || (!deviceService.isAvailable(deviceId) &&
                mastershipService.getMasterFor(deviceId) == null)) && configIsPresent(deviceId)) {
            log.debug("Trying to re-connect to device {}", deviceId);
            NodeId leaderNodeId = leadershipService.runForLeadership(CHECK + deviceId.toString() + CONNECTION)
                    .leader().nodeId();
            NodeId localNodeId = clusterService.getLocalNode().id();
            if (localNodeId.equals(leaderNodeId)) {
                log.debug("{} is leader for {}, initiating the connection", leaderNodeId,
                        deviceId);
                checkAndSubmitDeviceTask(deviceId);
            } else {
                log.debug("{} is not leader for {}, initiating connection, {} is LEADER",
                        localNodeId, deviceId, leaderNodeId);
                connectionExecutor.submit(exceptionSafe(() -> connectStandbyDevice(deviceId)));
                //FIXME this will be removed when config is synced
                cleanUpConfigInfo(deviceId);
            }
        } else if ((deviceService.getDevice(deviceId) != null && deviceService.isAvailable(deviceId)
                && mastershipService.isLocalMaster(deviceId) && !isReachable(deviceId) && configIsPresent(deviceId))) {
            log.info("Removing available but unreachable device {}", deviceId);
            disconnectDevice(deviceId);
            providerService.deviceDisconnected(deviceId);
        }
    }

    private boolean configIsPresent(DeviceId deviceId) {
        boolean present = cfgService.getConfig(deviceId, GeneralProviderDeviceConfig.class) != null
                && cfgService.getConfig(deviceId, BasicDeviceConfig.class) != null;
        if (!present) {
            log.warn("Configuration for device {} is not complete", deviceId);
        }
        return present;
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            // FIXME handling for mastership change scenario missing?

            // For now this is scheduled periodically, when streaming API will
            // be available we check and base it on the streaming API (e.g. gNMI)
            if (mastershipService.isLocalMaster(deviceId)) {
                scheduledTasks.put(deviceId, scheduleStatistcsPolling(deviceId, false));
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type() == Type.DEVICE_ADDED &&
                    event.subject().id().toString().startsWith(URI_SCHEME.toLowerCase());
        }
    }

    /**
     * Listener for device channel events.
     */
    private class InternalChannelListener implements ChannelListener {

        @Override
        public void event(ChannelEvent event) {
            DeviceId deviceId = event.subject();
            if (event.type().equals(ChannelEvent.Type.CHANNEL_DISCONNECTED)) {
                //let's properly handle device disconnection
                CompletableFuture<Boolean> disconnection = disconnectDevice(deviceId);
                disconnection.thenAcceptAsync(result -> {
                    //If master notifying of disconnection to the core.
                    if (mastershipService.isLocalMaster(deviceId)) {
                        log.info("Disconnecting unreachable device {}, due to error on channel", deviceId);
                        providerService.deviceDisconnected(deviceId);
                    }
                });

            }
            //TODO evaluate other type of reactions.
        }

    }
}
