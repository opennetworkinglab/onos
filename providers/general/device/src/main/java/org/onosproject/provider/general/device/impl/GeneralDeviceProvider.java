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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.ChassisId;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.SharedScheduledExecutors;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceAdminService;
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
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiPipeconfWatchdogEvent;
import org.onosproject.net.pi.service.PiPipeconfWatchdogListener;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.general.device.impl.DeviceTaskExecutor.DeviceTaskException;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.provider.general.device.impl.OsgiPropertyConstants.CHECKUP_INTERVAL;
import static org.onosproject.provider.general.device.impl.OsgiPropertyConstants.CHECKUP_INTERVAL_DEFAULT;
import static org.onosproject.provider.general.device.impl.OsgiPropertyConstants.STATS_POLL_INTERVAL;
import static org.onosproject.provider.general.device.impl.OsgiPropertyConstants.STATS_POLL_INTERVAL_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses drivers to discover devices, perform initial handshake,
 * and notify the core of disconnection events. The implementation listens for
 * events from netcfg or the drivers (via {@link DeviceAgentListener}) andP
 * schedules task for each event.
 */
@Component(immediate = true,
        property = {
                CHECKUP_INTERVAL + ":Integer=" + CHECKUP_INTERVAL_DEFAULT,
                STATS_POLL_INTERVAL + ":Integer=" + STATS_POLL_INTERVAL_DEFAULT
        })
public class GeneralDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    private final Logger log = getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.generaldeviceprovider";
    private static final String URI_SCHEME = "device";
    private static final String DEVICE_PROVIDER_PACKAGE =
            "org.onosproject.general.provider.device";
    private static final int CORE_POOL_SIZE = 10;
    private static final String UNKNOWN = "unknown";

    // We have measured a grace period of 5s with the
    // current devices - giving some time more to absorb
    // any fluctuation.
    private static final int GRACE_PERIOD = 8000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService pipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfWatchdogService pipeconfWatchdogService;

    // FIXME: no longer general if we add a dependency to a protocol-specific
    // service. Possible solutions are: rename this provider to
    // StratumDeviceProvider, find a way to allow this provider to register for
    // protocol specific events (e.g. port events) via drivers (similar to
    // DeviceAgentListener).
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private GnmiController gnmiController;

    private GnmiDeviceStateSubscriber gnmiDeviceStateSubscriber;

    /**
     * Configure interval for checking device availability; default is 10 sec.
     */
    private int checkupInterval = CHECKUP_INTERVAL_DEFAULT;

    /**
     * Configure poll frequency for port status and stats; default is 10 sec.
     */
    private int statsPollInterval = STATS_POLL_INTERVAL_DEFAULT;

    private final Map<DeviceId, DeviceHandshaker> handshakersWithListeners = Maps.newConcurrentMap();
    private final Map<DeviceId, Long> lastCheckups = Maps.newConcurrentMap();
    private final InternalPipeconfWatchdogListener pipeconfWatchdogListener = new InternalPipeconfWatchdogListener();
    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private final DeviceAgentListener deviceAgentListener = new InternalDeviceAgentListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ExecutorService mainExecutor;
    private DeviceTaskExecutor<TaskType> taskExecutor;
    private ScheduledFuture<?> checkupTask;
    private StatsPoller statsPoller;
    private DeviceProviderService providerService;

    private final Map<DeviceId, Pair<MastershipRole, Integer>> lastRoleRequest =
            Maps.newConcurrentMap();

    public GeneralDeviceProvider() {
        super(new ProviderId(URI_SCHEME, DEVICE_PROVIDER_PACKAGE));
    }

    protected DeviceProviderService providerService() {
        return providerService;
    }

    @Activate
    public void activate(ComponentContext context) {
        mainExecutor = newFixedThreadPool(CORE_POOL_SIZE, groupedThreads(
                "onos/gdp", "%d", log));
        taskExecutor = new DeviceTaskExecutor<>(mainExecutor, GDP_ALLOWLIST);
        providerService = providerRegistry.register(this);
        componentConfigService.registerProperties(getClass());
        coreService.registerApplication(APP_NAME);
        cfgService.addListener(cfgListener);
        deviceService.addListener(deviceListener);
        pipeconfWatchdogService.addListener(pipeconfWatchdogListener);
        gnmiDeviceStateSubscriber = new GnmiDeviceStateSubscriber(
                gnmiController, deviceService, mastershipService, providerService);
        gnmiDeviceStateSubscriber.activate();
        startOrReschedulePeriodicCheckupTasks();
        statsPoller = new StatsPoller(deviceService, mastershipService, providerService);
        statsPoller.activate(statsPollInterval);
        modified(context);
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        final int oldCheckupInterval = checkupInterval;
        checkupInterval = Tools.getIntegerProperty(
                properties, CHECKUP_INTERVAL, CHECKUP_INTERVAL_DEFAULT);
        log.info("Configured. {} is configured to {} seconds",
                 CHECKUP_INTERVAL, checkupInterval);
        final int oldStatsPollFrequency = statsPollInterval;
        statsPollInterval = Tools.getIntegerProperty(
                properties, STATS_POLL_INTERVAL, STATS_POLL_INTERVAL_DEFAULT);
        log.info("Configured. {} is configured to {} seconds",
                 STATS_POLL_INTERVAL, statsPollInterval);

        if (oldCheckupInterval != checkupInterval) {
            startOrReschedulePeriodicCheckupTasks();
        }

        if (oldStatsPollFrequency != statsPollInterval) {
            statsPoller.reschedule(statsPollInterval);
        }
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);

        // Shutdown stats poller.
        statsPoller.deactivate();
        statsPoller = null;
        // Shutdown periodic checkup task.
        checkupTask.cancel(false);
        checkupTask = null;
        // Shutdown main and task executor.
        taskExecutor.cancel();
        taskExecutor = null;
        mainExecutor.shutdownNow();
        try {
            mainExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("connectionExecutor not terminated properly");
        }
        mainExecutor = null;
        // Remove all device agent listeners
        handshakersWithListeners.values().forEach(h -> h.removeDeviceAgentListener(id()));
        handshakersWithListeners.clear();
        // Other cleanup.
        lastCheckups.clear();
        componentConfigService.unregisterProperties(getClass(), false);
        cfgService.removeListener(cfgListener);
        pipeconfWatchdogService.removeListener(pipeconfWatchdogListener);
        providerRegistry.unregister(this);
        providerService = null;
        gnmiDeviceStateSubscriber.deactivate();
        gnmiDeviceStateSubscriber = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        checkNotNull(deviceId);
        submitTask(deviceId, TaskType.CHECKUP);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        final MastershipInfo mastershipInfo = mastershipService.getMastershipFor(deviceId);
        final NodeId localNodeId = clusterService.getLocalNode().id();

        if (!mastershipInfo.getRole(localNodeId).equals(newRole)) {
            log.warn("Inconsistent mastership info for {}! Requested {}, but " +
                             "mastership service reports {}, will apply the latter...",
                     deviceId, newRole, mastershipInfo.getRole(localNodeId));
            newRole = mastershipInfo.getRole(localNodeId);
        }

        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            log.error("Null handshaker. Unable to notify role {} to {}",
                      newRole, deviceId);
            return;
        }

        // Derive preference value.
        final int preference;
        switch (newRole) {
            case MASTER:
                preference = 0;
                break;
            case STANDBY:
                preference = mastershipInfo.backups().indexOf(localNodeId) + 1;
                if (preference == 0) {
                    // Not found in list.
                    log.error("Unable to derive mastership preference for {}, " +
                                      "requested role {} but local node ID was " +
                                      "not found among list of backup nodes " +
                                      "reported by mastership service",
                              deviceId, newRole);
                    return;
                }
                break;
            case NONE:
                // No preference for NONE, apply as is.
                Pair<MastershipRole, Integer> pairRolePref = Pair.of(newRole, -1);
                if (log.isDebugEnabled()) {
                    log.debug("Notifying role {} to {}", newRole, deviceId);
                } else if (!pairRolePref.equals(lastRoleRequest.get(deviceId))) {
                    log.info("Notifying role {} to {}", newRole, deviceId);
                }
                lastRoleRequest.put(deviceId, pairRolePref);
                handshaker.roleChanged(newRole);
                return;
            default:
                log.error("Unrecognized mastership role {}", newRole);
                return;
        }

        Pair<MastershipRole, Integer> pairRolePref = Pair.of(newRole, preference);
        if (log.isDebugEnabled()) {
            log.debug("Notifying role {} (preference {}) for term {} to {}",
                    newRole, preference, mastershipInfo.term(), deviceId);
        } else if (!pairRolePref.equals(lastRoleRequest.get(deviceId))) {
            log.info("Notifying role {} (preference {}) for term {} to {}",
                    newRole, preference, mastershipInfo.term(), deviceId);
        }
        lastRoleRequest.put(deviceId, pairRolePref);

        try {
            handshaker.roleChanged(preference, mastershipInfo.term());
        } catch (UnsupportedOperationException e) {
            // Preference-based method not supported.
            handshaker.roleChanged(newRole);
        }
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            return false;
        }
        return handshaker.isReachable();
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            return false;
        }
        try {
            // Try without probing the device...
            return handshaker.isAvailable();
        } catch (UnsupportedOperationException e) {
            // Driver does not support that.
            return probeAvailability(handshaker);
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
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
        final String descr = format("%s port %s on %s",
                                    (enable ? "enable" : "disable"),
                                    portNumber, deviceId);
        modifyTask.whenComplete((success, ex) -> {
            if (ex != null) {
                log.error("Exception while trying to " + descr, ex);
            } else if (!success) {
                log.warn("Unable to " + descr);
            }
        });
    }

    @Override
    public void triggerDisconnect(DeviceId deviceId) {
        checkNotNull(deviceId);
        log.info("Triggering disconnection of device {}", deviceId);
        submitTask(deviceId, TaskType.CONNECTION_TEARDOWN);
    }

    @Override
    public CompletableFuture<Boolean> probeReachability(DeviceId deviceId) {
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            return CompletableFuture.completedFuture(false);
        }
        return handshaker.probeReachability();
    }

    @Override
    public int gracePeriod() {
        return GRACE_PERIOD;
    }

    private boolean probeReachabilitySync(DeviceId deviceId) {
        // Wait 3/4 of the checkup interval and make sure the thread
        // is not blocked more than the checkUpInterval
        return Tools.futureGetOrElse(probeReachability(deviceId), (checkupInterval * 3000 / 4),
                TimeUnit.MILLISECONDS, Boolean.FALSE);
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            DeviceId deviceId = (DeviceId) event.subject();
            switch (event.type()) {
                case CONFIG_ADDED:
                    if (configIsPresent(deviceId)) {
                        submitTask(deviceId, TaskType.CONNECTION_SETUP);
                    }
                    break;
                case CONFIG_UPDATED:
                    if (configIsPresent(deviceId) && mgmtAddrUpdated(event)) {
                        submitTask(deviceId, TaskType.CONNECTION_UPDATE);
                    }
                    break;
                case CONFIG_REMOVED:
                    if (event.configClass().equals(BasicDeviceConfig.class)) {
                        submitTask(deviceId, TaskType.CONNECTION_TEARDOWN);
                    }
                    break;
                default:
                    // Ignore
                    break;
            }
        }

        private boolean mgmtAddrUpdated(NetworkConfigEvent event) {
            if (!event.prevConfig().isPresent() || !event.config().isPresent()) {
                return false;
            }
            final BasicDeviceConfig prev = (BasicDeviceConfig) event.prevConfig().get();
            final BasicDeviceConfig current = (BasicDeviceConfig) event.config().get();
            return !Objects.equals(prev.managementAddress(), current.managementAddress());
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(BasicDeviceConfig.class) &&
                    (event.subject() instanceof DeviceId) &&
                    myScheme((DeviceId) event.subject());
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
                    submitTask(deviceId, TaskType.CHANNEL_OPEN);
                    break;
                case CHANNEL_CLOSED:
                case CHANNEL_ERROR:
                    submitTask(deviceId, TaskType.CHANNEL_CLOSED);
                    break;
                case ROLE_MASTER:
                    submitTask(deviceId, TaskType.ROLE_MASTER);
                    break;
                case ROLE_STANDBY:
                    submitTask(deviceId, TaskType.ROLE_STANDBY);
                    break;
                case ROLE_NONE:
                    // FIXME: in case of device disconnection, agents will
                    //  signal role NONE, preventing the DeviceManager to mark
                    //  the device as offline, as only the master can do that. We
                    //  should change the DeviceManager. For now, we disable
                    //  signaling role NONE.
                    // submitTask(deviceId, TaskType.ROLE_NONE);
                    break;
                case NOT_MASTER:
                    submitTask(deviceId, TaskType.NOT_MASTER);
                    break;
                default:
                    log.warn("Unrecognized device agent event {}", event.type());
            }
        }
    }

    /**
     * Pipeline event listener.
     */
    private class InternalPipeconfWatchdogListener implements PiPipeconfWatchdogListener {
        @Override
        public void event(PiPipeconfWatchdogEvent event) {
            final DeviceId deviceId = event.subject();
            switch (event.type()) {
                case PIPELINE_READY:
                    submitTask(deviceId, TaskType.PIPELINE_READY);
                    break;
                case PIPELINE_UNKNOWN:
                    submitTask(deviceId, TaskType.PIPELINE_NOT_READY);
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(PiPipeconfWatchdogEvent event) {
            return myScheme(event.subject());
        }
    }

    private void startOrReschedulePeriodicCheckupTasks() {
        synchronized (this) {
            if (checkupTask != null) {
                checkupTask.cancel(false);
            }
            checkupTask = SharedScheduledExecutors.getPoolThreadExecutor()
                    .scheduleAtFixedRate(
                            this::submitCheckupTasksForAllDevices,
                            1,
                            checkupInterval,
                            TimeUnit.SECONDS,
                            true);
        }
    }

    private void submitCheckupTasksForAllDevices() {
        // Async trigger a task for all devices in the cfg.
        log.debug("Submitting checkup task for all devices...");
        final Set<DeviceId> deviceToCheck = Sets.newHashSet();
        // All devices in the core and in the config that we care about.
        deviceService.getDevices().forEach(d -> {
            if (myScheme(d.id())) {
                deviceToCheck.add(d.id());
            }
        });
        cfgService.getSubjects(DeviceId.class).stream()
                .filter(GeneralDeviceProvider::myScheme)
                .filter(this::configIsPresent)
                .forEach(deviceToCheck::add);
        deviceToCheck.forEach(d -> submitTask(d, TaskType.CHECKUP));
    }

    /**
     * Type of tasks performed by this provider.
     */
    enum TaskType {
        CONNECTION_SETUP,
        CONNECTION_UPDATE,
        CONNECTION_TEARDOWN,
        PIPELINE_READY,
        CHANNEL_OPEN,
        CHANNEL_CLOSED,
        PIPELINE_NOT_READY,
        CHECKUP,
        ROLE_MASTER,
        ROLE_NONE,
        ROLE_STANDBY,
        NOT_MASTER,
    }

    private static final Set<TaskType> GDP_ALLOWLIST = Sets.newHashSet(TaskType.ROLE_MASTER, TaskType.ROLE_NONE,
            TaskType.ROLE_STANDBY, TaskType.NOT_MASTER);

    private void submitTask(DeviceId deviceId, TaskType taskType) {
        taskExecutor.submit(deviceId, taskType, taskRunnable(deviceId, taskType));
    }

    private Runnable taskRunnable(DeviceId deviceId, TaskType taskType) {
        switch (taskType) {
            case CONNECTION_SETUP:
                return () -> handleConnectionSetup(deviceId);
            case CONNECTION_UPDATE:
                return () -> handleConnectionUpdate(deviceId);
            case CONNECTION_TEARDOWN:
                return () -> handleConnectionTeardown(deviceId);
            case CHANNEL_OPEN:
            case CHECKUP:
            case PIPELINE_READY:
                return () -> doCheckupAndRepair(deviceId);
            case CHANNEL_CLOSED:
            case PIPELINE_NOT_READY:
                return () -> markOfflineIfNeeded(deviceId);
            case ROLE_MASTER:
                return () -> handleMastershipResponse(deviceId, MastershipRole.MASTER);
            case ROLE_STANDBY:
                return () -> handleMastershipResponse(deviceId, MastershipRole.STANDBY);
            case ROLE_NONE:
                return () -> handleMastershipResponse(deviceId, MastershipRole.NONE);
            case NOT_MASTER:
                return () -> handleNotMaster(deviceId);
            default:
                throw new IllegalArgumentException("Unrecognized task type " + taskType);
        }
    }

    private void handleConnectionSetup(DeviceId deviceId) {
        assertConfig(deviceId);
        // Bind pipeconf (if any and if device is capable).
        bindPipeconfIfRequired(deviceId);
        // Get handshaker.
        final DeviceHandshaker handshaker = handshakerOrFail(deviceId);
        if (handshaker.hasConnection() || handshakersWithListeners.containsKey(deviceId)) {
            throw new DeviceTaskException("connection already exists");
        }
        // Add device agent listener.
        handshakersWithListeners.put(deviceId, handshaker);
        handshaker.addDeviceAgentListener(id(), deviceAgentListener);
        // Start connection via handshaker.
        if (!handshaker.connect()) {
            // Failed! Remove listeners.
            handshaker.removeDeviceAgentListener(id());
            handshakersWithListeners.remove(deviceId);
            // Clean up connection state leftovers.
            handshaker.disconnect();
            throw new DeviceTaskException("connection failed");
        }
        createOrUpdateDevice(deviceId, false);
        // From here we expect a CHANNEL_OPEN event to update availability.
    }

    private void handleConnectionUpdate(DeviceId deviceId) {
        assertConfig(deviceId);
        final DeviceHandshaker handshaker = handshakerOrFail(deviceId);
        if (!handshaker.hasConnection()) {
            // If driver reports that a connection still exists, perhaps the
            // part of the netcfg that changed does not affect the connection.
            // Otherwise, remove any previous connection state from the old
            // netcfg and create a new one.
            log.warn("Detected change of connection endpoints for {}, will " +
                             "tear down existing connection and set up a new one...",
                     deviceId);
            handleConnectionTeardown(deviceId);
            handleConnectionSetup(deviceId);
        }
    }

    private void createOrUpdateDevice(DeviceId deviceId, boolean available) {
        assertConfig(deviceId);

        if (available) {
            // Push port descriptions. If marking online, make sure to update
            // ports before other subsystems pick up the device  event.
            final List<PortDescription> ports = getPortDetails(deviceId);
            providerService.updatePorts(deviceId, ports);
        }

        DeviceDescription deviceDescription = getDeviceDescription(deviceId, available);
        DeviceDescription storeDescription = providerService.getDeviceDescription(deviceId);
        if (deviceService.getDevice(deviceId) != null &&
                deviceService.isAvailable(deviceId) == available &&
                storeDescription != null) {
            // FIXME SDFAB-650 rethink the APIs and abstractions around the DeviceStore.
            //  Device registration is a two-step process for the GDP. Initially, the device is
            //  registered with default avail. to false. Later, the checkup task will update the
            //  description with the default avail to true in order to mark it available. Today,
            //  there is only one API to mark online a device from the device provider which is
            //  deviceConnected which assumes an update on the device description. The device provider
            //  is the only one able to update the device description and we have to make sure that
            //  the default avail. is flipped to true as it is used to mark as online the device when
            //  it is created or updated. Otherwise, if an ONOS instance fails and restarts, when re-joining
            //  the cluster, it will get the device marked as offline and will not be able to update
            //  its status until it become the master. This process concurs with the markOnline done
            //  by the background thread in the DeviceManager and its the reason why we cannot just check
            //  the device availability but we need to compare also the desc. Checking here the equality,
            //  as in general we may want to upgrade the device description at run time.
            DeviceDescription testDeviceDescription = DefaultDeviceDescription.copyReplacingAnnotation(
                    deviceDescription, storeDescription.annotations());
            if (testDeviceDescription.equals(storeDescription)) {
                return;
            }
        }

        providerService.deviceConnected(deviceId, deviceDescription);
    }

    private boolean probeAvailability(DeviceHandshaker handshaker) {
        return Futures.getUnchecked(handshaker.probeAvailability());
    }

    private void markOfflineIfNeeded(DeviceId deviceId) {
        assertDeviceRegistered(deviceId);
        if (deviceService.isAvailable(deviceId)) {
            providerService.deviceDisconnected(deviceId);
        }
    }

    private void doCheckupAndRepair(DeviceId deviceId) {

        //  This task should be invoked periodically for each device known by
        //  this provider, or as a consequence of events signaling potential
        //  availability changes of the device. We check that everything is in
        //  order, repair what's wrong, and eventually mark the the device as
        //  available (or not) in the core.

        if (!configIsPresent(deviceId)) {
            // We should have a connection only for devices in the config.
            submitTask(deviceId, TaskType.CONNECTION_TEARDOWN);
            return;
        }

        final DeviceHandshaker handshaker = handshakersWithListeners.get(deviceId);
        if (handshaker == null) {
            // Device in config but we have not initiated a connection.
            // Perhaps we missed the config event?
            submitTask(deviceId, TaskType.CONNECTION_SETUP);
            return;
        }

        // If here, we have a handshaker meaning we already connected once to
        // the device...
        if (!handshaker.hasConnection()) {
            // ...  but now the driver reports there is NOT a connection.
            // Perhaps the netcfg changed and we didn't pick the event?
            log.warn("Re-establishing lost connection to {}", deviceId);
            submitTask(deviceId, TaskType.CONNECTION_TEARDOWN);
            submitTask(deviceId, TaskType.CONNECTION_SETUP);
            return;
        }

        // If here, device should be registered in the core.
        assertDeviceRegistered(deviceId);

        if (!handshaker.isReachable() || !probeReachabilitySync(deviceId)) {
            // Device appears to be offline.
            markOfflineIfNeeded(deviceId);
            // We expect the protocol layer to implement some sort of
            // connection backoff mechanism and to signal availability via
            // CHANNEL_OPEN events.
            return;
        }

        // If here, device is reachable. Now do mastership and availability
        // checkups. To avoid overload of checkup tasks which might involve
        // sending messages over the network and triggering mastership
        // elections. We require a minimum interval of 1/3 of the configured
        // checkupInterval between consecutive checkup tasks when the device is
        // known to be available.

        final Long lastCheckup = lastCheckups.get(deviceId);
        final boolean isAvailable = deviceService.isAvailable(deviceId);
        if (isAvailable && lastCheckup != null &&
                (currentTimeMillis() - lastCheckup) < (checkupInterval * 1000 / 3)) {
            if (log.isDebugEnabled()) {
                log.debug("Dropping checkup task for {} as it happened recently",
                          deviceId);
            }
            return;
        }
        lastCheckups.put(deviceId, currentTimeMillis());

        // Make sure device has a valid mastership role.
        final MastershipRole expectedRole = mastershipService.getLocalRole(deviceId);
        if (expectedRole == MastershipRole.NONE) {
            log.debug("Detected invalid role ({}) for {}, waiting for mastership " +
                             "service to fix this...",
                     expectedRole, deviceId);
            // Gentle nudge to fix things...
            mastershipService.requestRoleForSync(deviceId);
            return;
        }

        final MastershipRole deviceRole = handshaker.getRole();
        // FIXME: we should be checking the mastership term as well.
        if (expectedRole != deviceRole) {
            // Let's be greedy, if the role is NONE likely is due to the lazy channel
            if (deviceRole == MastershipRole.NONE) {
                log.warn("Detected role mismatch for {}, core expects {}, " +
                                "but device reports {}, reassert the role... ",
                        deviceId, expectedRole, deviceRole);
                // If we are experiencing a severe issue, eventually
                // the DeviceManager will move the mastership
                roleChanged(deviceId, expectedRole);
            } else {
                log.debug("Detected role mismatch for {}, core expects {}, " +
                                "but device reports {}, waiting for mastership " +
                                "service  to fix this...",
                        deviceId, expectedRole, deviceRole);
                // Gentle nudge to fix things...
                providerService.receivedRoleReply(deviceId, deviceRole);
            }
            return;
        }

        // Check and update availability, which differently from reachability
        // describes the ability of the device to forward packets.
        if (probeAvailability(handshaker)) {
            // Device ready to do its job.
            createOrUpdateDevice(deviceId, true);
        } else {
            markOfflineIfNeeded(deviceId);
            if (isPipelineProgrammable(deviceId)) {
                // If reachable, but not available, and pipeline programmable,
                // there is a high chance it's because the pipeline is not READY
                // (independently from what the pipeconf watchdog reports, as
                // the status there might be outdated). Encourage pipeconf
                // watchdog to perform a pipeline probe ASAP.
                pipeconfWatchdogService.triggerProbe(deviceId);
            }
        }
    }

    private void handleMastershipResponse(DeviceId deviceId, MastershipRole response) {
        assertDeviceRegistered(deviceId);
        log.debug("Device {} asserted role {}", deviceId, response);
        providerService.receivedRoleReply(deviceId, response);
    }

    private void handleNotMaster(DeviceId deviceId) {
        assertDeviceRegistered(deviceId);
        handleMastershipResponse(deviceId, handshakerOrFail(deviceId).getRole());
    }

    private void assertDeviceRegistered(DeviceId deviceId) {
        if (!deviceIsRegistered(deviceId)) {
            throw new DeviceTaskException("device not registered in the core");
        }
    }

    private boolean deviceIsRegistered(DeviceId deviceId) {
        return deviceService.getDevice(deviceId) != null;
    }

    private void handleConnectionTeardown(DeviceId deviceId) {
        if (deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId)) {
            providerService.deviceDisconnected(deviceId);
        }
        final DeviceHandshaker handshaker = handshakerOrFail(deviceId);
        handshaker.removeDeviceAgentListener(id());
        handshakersWithListeners.remove(deviceId);
        handshaker.disconnect();
        lastCheckups.remove(deviceId);
    }

    private void bindPipeconfIfRequired(DeviceId deviceId) {
        if (pipeconfService.getPipeconf(deviceId).isPresent()
                || !isPipelineProgrammable(deviceId)) {
            // Nothing to do.
            // Device has already a pipeconf or is not programmable.
            return;
        }
        // Get pipeconf from netcfg or driver (default one).
        final PiPipelineProgrammable pipelineProg = getBehaviour(
                deviceId, PiPipelineProgrammable.class);
        final PiPipeconfId pipeconfId = getPipeconfId(deviceId, pipelineProg);
        if (pipeconfId == null) {
            throw new DeviceTaskException("unable to find pipeconf");
        }
        if (!pipeconfService.getPipeconf(pipeconfId).isPresent()) {
            throw new DeviceTaskException(format(
                    "pipeconf %s not registered", pipeconfId));
        }
        // Store binding in pipeconf service.
        pipeconfService.bindToDevice(pipeconfId, deviceId);
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
        }
        return null;
    }

    private PiPipeconfId getPipeconfFromCfg(DeviceId deviceId) {
        BasicDeviceConfig config = cfgService.getConfig(
                deviceId, BasicDeviceConfig.class);
        if (config == null) {
            return null;
        }
        return config.pipeconf() != null
                ? new PiPipeconfId(config.pipeconf()) : null;
    }

    private DeviceHandshaker handshakerOrFail(DeviceId deviceId) {
        final DeviceHandshaker handshaker = getBehaviour(
                deviceId, DeviceHandshaker.class);
        if (handshaker == null) {
            throw new DeviceTaskException("missing handshaker behavior");
        }
        return handshaker;
    }

    private boolean configIsPresent(DeviceId deviceId) {
        final BasicDeviceConfig basicDeviceCfg = cfgService.getConfig(
                deviceId, BasicDeviceConfig.class);
        return basicDeviceCfg != null && !isNullOrEmpty(basicDeviceCfg.driver());
    }

    private void assertConfig(DeviceId deviceId) {
        if (!configIsPresent(deviceId)) {
            throw new DeviceTaskException("configuration is not complete");
        }
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
        Driver driver = getDriver(deviceId);
        if (driver == null) {
            return null;
        }
        if (!driver.hasBehaviour(type)) {
            return null;
        }
        final DriverData data = new DefaultDriverData(driver, deviceId);
        final DefaultDriverHandler handler = new DefaultDriverHandler(data);
        return driver.createBehaviour(handler, type);
    }

    private boolean hasBehaviour(DeviceId deviceId, Class<? extends Behaviour> type) {
        Driver driver = getDriver(deviceId);
        if (driver == null) {
            return false;
        }
        return driver.hasBehaviour(type);
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
        return new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.SWITCH,
                driver != null ? driver.manufacturer() : UNKNOWN,
                driver != null ? driver.hwVersion() : UNKNOWN,
                driver != null ? driver.swVersion() : UNKNOWN,
                UNKNOWN,
                new ChassisId(),
                defaultAvailable,
                DefaultAnnotations.EMPTY);
    }

    static boolean myScheme(DeviceId deviceId) {
        return deviceId.uri().getScheme().equals(URI_SCHEME);
    }

    private boolean isPipelineProgrammable(DeviceId deviceId) {
        return hasBehaviour(deviceId, PiPipelineProgrammable.class);
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            log.info("Triggering disconnect for device {}", event.subject().id());
            triggerDisconnect(event.subject().id());
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return DeviceEvent.Type.DEVICE_REMOVED == event.type() && myScheme(event.subject().id());
        }
    }
}
