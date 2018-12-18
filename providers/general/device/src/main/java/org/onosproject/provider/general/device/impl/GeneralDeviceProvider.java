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
import org.onosproject.core.CoreService;
import org.onosproject.gnmi.api.GnmiController;
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
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.general.device.api.GeneralProviderDeviceConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
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

    private final Logger log = getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.gdp";
    private static final String URI_SCHEME = "device";
    private static final String CFG_SCHEME = "generalprovider";
    private static final String DEVICE_PROVIDER_PACKAGE =
            "org.onosproject.general.provider.device";
    private static final int CORE_POOL_SIZE = 10;
    private static final String UNKNOWN = "unknown";
    private static final String DRIVER = "driver";
    private static final Set<String> PIPELINE_CONFIGURABLE_PROTOCOLS =
            ImmutableSet.of("p4runtime");

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
    private PiPipeconfService pipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfWatchdogService pipeconfWatchdogService;

    // FIXME: no longer general if we add a dependency to a protocol-specific
    // service. Possible solutions are: rename this provider to
    // StratumDeviceProvider, find a way to allow this provider to register for
    // protocol specific events (e.g. port events) via drivers (similar to
    // DeviceAgentListener).
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private GnmiController gnmiController;

    private GnmiDeviceStateSubscriber gnmiDeviceStateSubscriber;

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

    //FIXME to be removed when netcfg will issue device events in a bundle or
    //ensures all configuration needed is present
    private final Set<DeviceId> deviceConfigured = new CopyOnWriteArraySet<>();
    private final Set<DeviceId> driverConfigured = new CopyOnWriteArraySet<>();
    private final Set<DeviceId> pipelineConfigured = new CopyOnWriteArraySet<>();

    private final Map<DeviceId, MastershipRole> requestedRoles = Maps.newConcurrentMap();
    private final ConcurrentMap<DeviceId, ScheduledFuture<?>> statsPollingTasks = new ConcurrentHashMap<>();
    private final Map<DeviceId, DeviceHandshaker> handshakersWithListeners = Maps.newConcurrentMap();
    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final InternalPipeconfWatchdogListener pipeconfWatchdogListener = new InternalPipeconfWatchdogListener();
    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private final DeviceAgentListener deviceAgentListener = new InternalDeviceAgentListener();
    private final ConfigFactory factory = new InternalConfigFactory();
    private final Striped<Lock> deviceLocks = Striped.lock(30);

    private ExecutorService connectionExecutor;
    private ScheduledExecutorService statsExecutor;
    private ScheduledExecutorService probeExecutor;
    private ScheduledFuture<?> probeTask;
    private DeviceProviderService providerService;

    public GeneralDeviceProvider() {
        super(new ProviderId(URI_SCHEME, DEVICE_PROVIDER_PACKAGE));
    }

    @Activate
    public void activate(ComponentContext context) {
        connectionExecutor = newFixedThreadPool(CORE_POOL_SIZE, groupedThreads(
                "onos/gdp-connect", "%d", log));
        statsExecutor = newScheduledThreadPool(CORE_POOL_SIZE, groupedThreads(
                "onos/gdp-stats", "%d", log));
        probeExecutor = newSingleThreadScheduledExecutor(groupedThreads(
                "onos/gdp-probe", "%d", log));
        providerService = providerRegistry.register(this);
        componentConfigService.registerProperties(getClass());
        coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        deviceService.addListener(deviceListener);
        pipeconfWatchdogService.addListener(pipeconfWatchdogListener);
        rescheduleProbeTask(false);
        modified(context);
        gnmiDeviceStateSubscriber = new GnmiDeviceStateSubscriber(gnmiController,
                deviceService, mastershipService, providerService);
        gnmiDeviceStateSubscriber.activate();
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

        if (oldStatsPollFrequency != statsPollFrequency) {
            rescheduleStatsPollingTasks();
        }

        if (oldProbeFrequency != probeFrequency) {
            rescheduleProbeTask(true);
        }
    }

    private void rescheduleProbeTask(boolean deelay) {
        synchronized (this) {
            if (probeTask != null) {
                probeTask.cancel(false);
            }
            probeTask = probeExecutor.scheduleAtFixedRate(
                    this::triggerProbeAllDevices,
                    deelay ? probeFrequency : 0,
                    probeFrequency,
                    TimeUnit.SECONDS);
        }
    }

    @Deactivate
    public void deactivate() {
        // Shutdown stats polling tasks.
        statsPollingTasks.keySet().forEach(this::cancelStatsPolling);
        statsPollingTasks.clear();
        statsExecutor.shutdownNow();
        try {
            statsExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("statsExecutor not terminated properly");
        }
        statsExecutor = null;
        // Shutdown probe executor.
        probeTask.cancel(true);
        probeTask = null;
        probeExecutor.shutdownNow();
        try {
            probeExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("probeExecutor not terminated properly");
        }
        probeExecutor = null;
        // Shutdown connection executor.
        connectionExecutor.shutdownNow();
        try {
            connectionExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("connectionExecutor not terminated properly");
        }
        connectionExecutor = null;
        // Remove all device agent listeners
        handshakersWithListeners.values().forEach(h -> h.removeDeviceAgentListener(id()));
        handshakersWithListeners.clear();
        // Other cleanup.
        componentConfigService.unregisterProperties(getClass(), false);
        cfgService.removeListener(cfgListener);
        deviceService.removeListener(deviceListener);
        pipeconfWatchdogService.removeListener(pipeconfWatchdogListener);
        providerRegistry.unregister(this);
        providerService = null;
        cfgService.unregisterConfigFactory(factory);
        gnmiDeviceStateSubscriber.deactivate();
        gnmiDeviceStateSubscriber = null;
        log.info("Stopped");
    }


    @Override
    public void triggerProbe(DeviceId deviceId) {
        connectionExecutor.execute(withDeviceLock(
                () -> doDeviceProbe(deviceId), deviceId));
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.info("Notifying role {} to device {}", newRole, deviceId);
        requestedRoles.put(deviceId, newRole);
        connectionExecutor.execute(() -> doRoleChanged(deviceId, newRole));
    }

    private void doRoleChanged(DeviceId deviceId, MastershipRole newRole) {
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
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
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            return false;
        }
        return getFutureWithDeadline(
                handshaker.isReachable(), "checking reachability",
                deviceId, false, opTimeoutShort);
    }

    private boolean isConnected(DeviceId deviceId) {
        log.debug("Testing connection to device {}", deviceId);
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            return false;
        }
        return handshaker.isConnected();
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

    private Driver getDriver(DeviceId deviceId) {
        try {
            // DriverManager checks first using basic device config.
            return driverService.getDriver(deviceId);
        } catch (ItemNotFoundException e) {
            log.error("Driver not found for {}", deviceId);
            return null;
        }
    }

    private <T extends Behaviour> T getBehaviour(DeviceId deviceId, Class<T> type) {
        // Get handshaker.

        Driver driver = getDriver(deviceId);
        if (driver == null) {
            return null;
        }
        if (!driver.hasBehaviour(type)) {
            return null;
        }
        final DriverData data = new DefaultDriverData(driver, deviceId);
        // Storing deviceKeyId and all other config values as data in the driver
        // with protocol_<info> name as the key. e.g protocol_ip.
        final GeneralProviderDeviceConfig providerConfig = cfgService.getConfig(
                deviceId, GeneralProviderDeviceConfig.class);
        if (providerConfig != null) {
            providerConfig.protocolsInfo().forEach((protocol, info) -> {
                info.configValues().forEach(
                        (k, v) -> data.set(protocol + "_" + k, v));
                data.set(protocol + "_key", info.deviceKeyId());
            });
        }
        final DefaultDriverHandler handler = new DefaultDriverHandler(data);
        return driver.createBehaviour(handler, type);
    }

    private void doConnectDevice(DeviceId deviceId) {
        log.debug("Initiating connection to device {}...", deviceId);
        // Retrieve config
        if (configIsMissing(deviceId)) {
            return;
        }
        // Bind pipeconf (if any and if device is capable).
        if (!bindPipeconfIfRequired(deviceId)) {
            // We already logged the error.
            return;
        }
        // Get handshaker.
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            log.error("Missing handshaker behavior for {}, aborting connection",
                      deviceId);
            return;
        }
        // Add device agent listener.
        handshaker.addDeviceAgentListener(id(), deviceAgentListener);
        handshakersWithListeners.put(deviceId, handshaker);
        // Start connection via handshaker.
        final Boolean connectSuccess = getFutureWithDeadline(
                handshaker.connect(), "initiating connection",
                deviceId, false, opTimeoutShort);
        if (!connectSuccess) {
            log.warn("Unable to connect to {}", deviceId);
        }
    }

    private void triggerAdvertiseDevice(DeviceId deviceId) {
        connectionExecutor.execute(withDeviceLock(
                () -> doAdvertiseDevice(deviceId), deviceId));
    }

    private void doAdvertiseDevice(DeviceId deviceId) {
        // Retrieve config
        if (configIsMissing(deviceId)) {
            return;
        }
        // Obtain device and port description.
        final boolean isPipelineReady = isPipelineReady(deviceId);
        final DeviceDescription description = getDeviceDescription(
                deviceId, isPipelineReady);
        final List<PortDescription> ports = getPortDetails(deviceId);
        // Advertise to core.
        if (deviceService.getDevice(deviceId) == null ||
                (description.isDefaultAvailable() &&
                        !deviceService.isAvailable(deviceId))) {
            if (!isPipelineReady) {
                log.info("Advertising device {} to core with available={} as " +
                                 "device pipeline is not ready yet",
                         deviceId, description.isDefaultAvailable());
            }
            providerService.deviceConnected(deviceId, description);
        }
        providerService.updatePorts(deviceId, ports);
        // If pipeline is not ready, encourage watchdog to perform probe ASAP.
        if (!isPipelineReady) {
            pipeconfWatchdogService.triggerProbe(deviceId);
        }
    }

    private DeviceDescription getDeviceDescription(
            DeviceId deviceId, boolean defaultAvailable) {
        // Get one from driver or forge.
        final DeviceDescriptionDiscovery deviceDiscovery = getBehaviour(
                deviceId, DeviceDescriptionDiscovery.class);
        if (deviceDiscovery == null) {
            return forgeDeviceDescription(deviceId, defaultAvailable);
        }

        final DeviceDescription d = deviceDiscovery.discoverDeviceDetails();
        if (d == null) {
            return forgeDeviceDescription(deviceId, defaultAvailable);
        }
        // Enforce defaultAvailable flag over the one obtained from driver.
        return new DefaultDeviceDescription(d, defaultAvailable, d.annotations());
    }

    private List<PortDescription> getPortDetails(DeviceId deviceId) {
        final DeviceDescriptionDiscovery deviceDiscovery = getBehaviour(
                deviceId, DeviceDescriptionDiscovery.class);
        if (deviceDiscovery != null) {
            return deviceDiscovery.discoverPortDetails();
        } else {
            return Collections.emptyList();
        }
    }

    private DeviceDescription forgeDeviceDescription(
            DeviceId deviceId, boolean defaultAvailable) {
        // Uses handshaker and provider config to get driver data.
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        final Driver driver = handshaker != null
                ? handshaker.handler().driver() : null;
        final GeneralProviderDeviceConfig cfg = cfgService.getConfig(
                deviceId, GeneralProviderDeviceConfig.class);
        final DefaultAnnotations.Builder annBuilder = DefaultAnnotations.builder();
        // If device is pipeline programmable, let this provider decide when the
        // device can be marked online.
        annBuilder.set(AnnotationKeys.PROVIDER_MARK_ONLINE,
                       String.valueOf(isPipelineProgrammable(deviceId)));
        if (cfg != null) {
            StringJoiner protoStringBuilder = new StringJoiner(", ");
            cfg.protocolsInfo().keySet().forEach(protoStringBuilder::add);
            annBuilder.set(AnnotationKeys.PROTOCOL, protoStringBuilder.toString());
        }
        return new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.SWITCH,
                driver != null ? driver.manufacturer() : UNKNOWN,
                driver != null ? driver.hwVersion() : UNKNOWN,
                driver != null ? driver.swVersion() : UNKNOWN,
                UNKNOWN,
                new ChassisId(),
                defaultAvailable,
                annBuilder.build());
    }

    private void triggerMarkAvailable(DeviceId deviceId) {
        connectionExecutor.execute(withDeviceLock(
                () -> doMarkAvailable(deviceId), deviceId));
    }

    private void doMarkAvailable(DeviceId deviceId) {
        if (deviceService.isAvailable(deviceId)) {
            return;
        }
        final DeviceDescription descr = getDeviceDescription(deviceId, true);
        // It has been observed that devices that were marked offline (e.g.
        // after device disconnection) might end up with no master. Here we
        // trigger a new master election (if device has no master).
        mastershipService.requestRoleForSync(deviceId);
        providerService.deviceConnected(deviceId, descr);
    }

    private boolean bindPipeconfIfRequired(DeviceId deviceId) {
        if (pipeconfService.ofDevice(deviceId).isPresent()
                || !isPipelineProgrammable(deviceId)) {
            // Nothing to do, all good.
            return true;
        }
        // Get pipeconf from netcfg or driver (default one).
        final PiPipelineProgrammable pipelineProg = getBehaviour(
                deviceId, PiPipelineProgrammable.class);
        final PiPipeconfId pipeconfId = getPipeconfId(deviceId, pipelineProg);
        if (pipeconfId == null) {
            return false;
        }
        // Store binding in pipeconf service.
        pipeconfService.bindToDevice(pipeconfId, deviceId);
        return true;
    }

    private PiPipeconfId getPipeconfId(DeviceId deviceId, PiPipelineProgrammable pipelineProg) {
        // Places to look for a pipeconf ID (in priority order)):
        // 1) netcfg
        // 2) device driver (default one)
        final PiPipeconfId pipeconfId = getPipeconfFromCfg(deviceId);
        if (pipeconfId != null && !pipeconfId.id().isEmpty()) {
            return pipeconfId;
        }
        if (pipelineProg != null
                && pipelineProg.getDefaultPipeconf().isPresent()) {
            final PiPipeconf defaultPipeconf = pipelineProg.getDefaultPipeconf().get();
            log.info("Using default pipeconf {} for {}", defaultPipeconf.id(), deviceId);
            return defaultPipeconf.id();
        } else {
            log.warn("Unable to associate a pipeconf to {}", deviceId);
            return null;
        }
    }

    private void doDisconnectDevice(DeviceId deviceId) {
        log.debug("Initiating disconnection from {}...", deviceId);
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
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
        handshaker.removeDeviceAgentListener(id());
        handshakersWithListeners.remove(deviceId);
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

    private boolean isPipelineProgrammable(DeviceId deviceId) {
        final GeneralProviderDeviceConfig providerConfig = cfgService.getConfig(
                deviceId, GeneralProviderDeviceConfig.class);
        if (providerConfig == null) {
            return false;
        }
        return !Collections.disjoint(
                ImmutableSet.copyOf(providerConfig.node().fieldNames()),
                PIPELINE_CONFIGURABLE_PROTOCOLS);
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

    private boolean isPipelineReady(DeviceId deviceId) {
        final boolean isPipelineProg = isPipelineProgrammable(deviceId);
        final boolean isPipeconfReady = pipeconfWatchdogService
                .getStatus(deviceId)
                .equals(PiPipeconfWatchdogService.PipelineStatus.READY);
        return !isPipelineProg || isPipeconfReady;
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
                .forEach(this::triggerDeviceProbe);
    }

    private PiPipeconfId getPipeconfFromCfg(DeviceId deviceId) {
        PiPipeconfConfig config = cfgService.getConfig(
                deviceId, PiPipeconfConfig.class);
        if (config == null) {
            return null;
        }
        return config.piPipeconfId();
    }

    private void triggerDeviceProbe(DeviceId deviceId) {
        connectionExecutor.execute(withDeviceLock(
                () -> doDeviceProbe(deviceId), deviceId));
    }

    private void doDeviceProbe(DeviceId deviceId) {
        log.debug("Probing device {}...", deviceId);
        if (configIsMissing(deviceId)) {
            return;
        }
        if (!isConnected(deviceId)) {
            if (deviceService.isAvailable(deviceId)) {
                providerService.deviceDisconnected(deviceId);
            }
            triggerConnect(deviceId);
        }
    }

    private boolean configIsMissing(DeviceId deviceId) {
        final boolean present =
                cfgService.getConfig(deviceId, GeneralProviderDeviceConfig.class) != null
                        && cfgService.getConfig(deviceId, BasicDeviceConfig.class) != null;
        if (!present) {
            log.warn("Configuration for device {} is not complete", deviceId);
        }
        return !present;
    }

    private void handleMastershipResponse(DeviceId deviceId, MastershipRole response) {
        // Notify core about mastership response.
        final MastershipRole request = requestedRoles.get(deviceId);
        final boolean isAvailable = deviceService.isAvailable(deviceId);
        if (request == null || !isAvailable) {
            return;
        }
        log.debug("Device {} asserted role {} (requested was {})",
                  deviceId, response, request);
        providerService.receivedRoleReply(deviceId, request, response);
        // FIXME: this should be based on assigned mastership, not what returned by device
        if (response.equals(MastershipRole.MASTER)) {
            startStatsPolling(deviceId, false);
        } else {
            cancelStatsPolling(deviceId);
        }
    }

    private void handleNotMaster(DeviceId deviceId) {
        log.warn("Device {} notified that this node is not master, " +
                         "relinquishing mastership...", deviceId);
        mastershipService.relinquishMastership(deviceId);
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
                    triggerAdvertiseDevice(deviceId);
                    break;
                case CHANNEL_CLOSED:
                case CHANNEL_ERROR:
                    triggerDeviceProbe(deviceId);
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
                case NOT_MASTER:
                    handleNotMaster(deviceId);
                    break;
                default:
                    log.warn("Unrecognized device agent event {}", event.type());
            }
        }

    }

    private class InternalPipeconfWatchdogListener implements PiPipeconfWatchdogListener {
        @Override
        public void event(PiPipeconfWatchdogEvent event) {
            triggerMarkAvailable(event.subject());
        }

        @Override
        public boolean isRelevant(PiPipeconfWatchdogEvent event) {
            return event.type().equals(PiPipeconfWatchdogEvent.Type.PIPELINE_READY);
        }
    }

    private class InternalConfigFactory
            extends ConfigFactory<DeviceId, GeneralProviderDeviceConfig> {

        InternalConfigFactory() {
            super(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                  GeneralProviderDeviceConfig.class, CFG_SCHEME);
        }

        @Override
        public GeneralProviderDeviceConfig createConfig() {
            return new GeneralProviderDeviceConfig();
        }
    }
}
