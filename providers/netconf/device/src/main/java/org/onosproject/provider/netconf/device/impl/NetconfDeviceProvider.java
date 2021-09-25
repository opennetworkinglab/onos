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

package org.onosproject.provider.netconf.device.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Triple;
import com.google.common.util.concurrent.Striped;
import org.onlab.packet.ChassisId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.config.NetconfDeviceConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.netconf.NetconfDeviceInfo.extractIpPortPath;
import static org.onosproject.provider.netconf.device.impl.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an NETCONF controller to detect device.
 */
@Component(immediate = true,
        property = {
                POLL_FREQUENCY_SECONDS + ":Integer=" + POLL_FREQUENCY_SECONDS_DEFAULT,
                MAX_RETRIES + ":Integer=" + MAX_RETRIES_DEFAULT,
                RETRY_FREQUENCY_SECONDS + ":Integer=" + RETRY_FREQUENCY_SECONDS_DEFAULT,
                FORCE_PORT_UPDATES + ":Boolean=" + FORCE_PORT_UPDATES_DEFAULT
        })
public class NetconfDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetconfController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceKeyAdminService deviceKeyAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;


    protected static final String APP_NAME = "org.onosproject.netconf";
    protected static final String SCHEME_NAME = "netconf";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.netconf.provider.device";
    private static final String UNKNOWN = "unknown";
    protected static final String ISNULL = "NetconfDeviceInfo is null";
    private static final String IPADDRESS = "ipaddress";
    private static final String PORT = "port";
    private static final String PATH = "path";
    private static final int CORE_POOL_SIZE = 10;

    /**
     * Configure poll frequency for port status and statistics; default is 30 sec.
     */
    private int pollFrequency = POLL_FREQUENCY_SECONDS_DEFAULT;

    /**
     * Configure maximum allowed number of retries for obtaining list of ports; default is 5 times.
     */
    private int maxRetries = MAX_RETRIES_DEFAULT;

    /**
     * Configure retry frequency for connecting with device again; default is 30 sec.
     */
    private int retryFrequency = RETRY_FREQUENCY_SECONDS_DEFAULT;

    /**
     * Configure option to allow ports to be periodically updated; default is false.
     */
    private boolean forcePortUpdates = FORCE_PORT_UPDATES_DEFAULT;

    protected ExecutorService connectionExecutor = Executors.newFixedThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/netconfDeviceProviderConnection",
                    "connection-executor-%d", log));
    protected ScheduledExecutorService pollingExecutor = newScheduledThreadPool(CORE_POOL_SIZE,
            groupedThreads("onos/netconfDeviceProviderPoll",
                    "polling-executor-%d", log));
    protected ScheduledExecutorService reconnectionExecutor = newSingleThreadScheduledExecutor(
            groupedThreads("onos/netconfDeviceProviderReconnection",
                           "reconnection-executor-%d", log));
    protected DeviceProviderService providerService;
    private final Map<DeviceId, AtomicInteger> retriedPortDiscoveryMap = new ConcurrentHashMap<>();
    protected ScheduledFuture<?> scheduledTask;
    protected ScheduledFuture<?> scheduledReconnectionTask;
    private final Striped<Lock> deviceLocks = Striped.lock(30);

    protected final ConfigFactory factory =
            // TODO consider moving Config registration to NETCONF ctl bundle
            new ConfigFactory<DeviceId, NetconfDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    NetconfDeviceConfig.class, NetconfDeviceConfig.CONFIG_KEY) {
                @Override
                public NetconfDeviceConfig createConfig() {
                    return new NetconfDeviceConfig();
                }
            };

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private NetconfDeviceListener innerNodeListener = new InnerNetconfDeviceListener();
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private boolean active;

    private ForkJoinPool scheduledTaskPool = new ForkJoinPool(CORE_POOL_SIZE);

    @Activate
    public void activate(ComponentContext context) {
        active = true;
        componentConfigService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        controller.addDeviceListener(innerNodeListener);
        deviceService.addListener(deviceListener);
        scheduledTask = schedulePolling();
        scheduledReconnectionTask = scheduleConnectDevices();
        modified(context);
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        cfgService.removeListener(cfgListener);
        componentConfigService.unregisterProperties(getClass(), false);
        deviceService.removeListener(deviceListener);
        active = false;
        controller.getNetconfDevices().forEach(id -> {
            deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(id.toString()));
            controller.disconnectDevice(id, true);
        });
        controller.removeDeviceListener(innerNodeListener);
        providerRegistry.unregister(this);
        providerService = null;
        retriedPortDiscoveryMap.clear();
        cfgService.unregisterConfigFactory(factory);
        scheduledTask.cancel(true);
        connectionExecutor.shutdown();
        pollingExecutor.shutdown();
        reconnectionExecutor.shutdown();
        log.info("Stopped");
    }


    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            int newPollFrequency = Tools.getIntegerProperty(properties, POLL_FREQUENCY_SECONDS,
                    POLL_FREQUENCY_SECONDS_DEFAULT);

            if (newPollFrequency != pollFrequency) {
                pollFrequency = newPollFrequency;

                if (scheduledTask != null) {
                    scheduledTask.cancel(false);
                }
                scheduledTask = schedulePolling();
                log.info("Configured. Poll frequency is configured to {} seconds", pollFrequency);
            }

            int newRetryFrequency = Tools.getIntegerProperty(properties, RETRY_FREQUENCY_SECONDS,
                                                            RETRY_FREQUENCY_SECONDS_DEFAULT);

            if (newRetryFrequency != retryFrequency) {
                retryFrequency = newRetryFrequency;
                if (scheduledReconnectionTask != null) {
                    scheduledReconnectionTask.cancel(false);
                }
                scheduledReconnectionTask = scheduleConnectDevices();
                log.info("Configured. Retry frequency is configured to {} seconds", retryFrequency);
            }

            boolean newForcePortUpdates = Tools.isPropertyEnabled(properties, FORCE_PORT_UPDATES,
                                          FORCE_PORT_UPDATES_DEFAULT);
            if (newForcePortUpdates != forcePortUpdates) {
                forcePortUpdates = newForcePortUpdates;
            }
            log.info("Configured. Force port updates flag is set to {}", forcePortUpdates);

            maxRetries = Tools.getIntegerProperty(properties, MAX_RETRIES, MAX_RETRIES_DEFAULT);
            log.info("Configured. Number of retries is configured to {} times", maxRetries);
        }
    }

    public NetconfDeviceProvider() {
        super(new ProviderId(SCHEME_NAME, DEVICE_PROVIDER_PACKAGE));
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        //Not implemented, unused in netconf cases.
        log.debug("Probing {} not implemented, not useful for NETCONF", deviceId);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.debug("Request role change {}, {}", deviceId, newRole);
        if (active) {
            switch (newRole) {
                case MASTER:
                    if (controller.getNetconfDevice(deviceId) == null ||
                               !controller.getNetconfDevice(deviceId).isMasterSession()) {
                        connectionExecutor.execute(exceptionSafe(() -> withDeviceLock(
                                () -> initiateConnection(deviceId), deviceId).run()));
                        log.debug("Accepting mastership role change to {} for device {}", newRole, deviceId);
                    }
                    break;
                case STANDBY:
                    //TODO this issue a warning on the first election/connection
                    controller.disconnectDevice(deviceId, false);
                    withDeviceLock(
                            () -> initiateConnection(deviceId, newRole), deviceId).run();
                    providerService.receivedRoleReply(deviceId, newRole, MastershipRole.STANDBY);
                    //else no-op
                    break;
                case NONE:
                    controller.disconnectDevice(deviceId, false);
                    providerService.receivedRoleReply(deviceId, newRole, MastershipRole.NONE);
                    break;
                default:
                    log.error("Unimplemented Mastership state : {}", newRole);

            }
        }
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        boolean isReachable = isTcpConnectionAvailable(deviceId);
        if (isReachable) {
            return controller.pingDevice(deviceId);
        }
        return false;
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        boolean sessionExists =
                Optional.ofNullable(controller.getDevicesMap().get(deviceId))
                        .map(NetconfDevice::isActive)
                        .orElse(false);
        if (sessionExists) {
            return true;
        }

        //FIXME this is a workaround util device state is shared
        // between controller instances.
        return isTcpConnectionAvailable(deviceId);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            log.error("Device {} is not present in the store", deviceId);
            return;
        }
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.info("Not master but {}, not changing port state", mastershipService.getLocalRole(deviceId));
            return;
        }
        if (!device.is(PortAdmin.class)) {
            log.warn("Device {} does not support Port Admin", deviceId);
            return;
        }
        PortAdmin portAdmin = device.as(PortAdmin.class);
        CompletableFuture<Boolean> modified;
        if (enable) {
            modified = portAdmin.enable(portNumber);
        } else {
            modified = portAdmin.disable(portNumber);
        }
        modified.thenAccept(result -> {
            if (result) {
                Port port = deviceService.getPort(deviceId, portNumber);
                //rebuilding port description with admin state changed.
                providerService.portStatusChanged(deviceId,
                        DefaultPortDescription.builder()
                                .withPortNumber(portNumber)
                                .isEnabled(enable)
                                .isRemoved(false)
                                .type(port.type())
                                .portSpeed(port.portSpeed())
                                .annotations((SparseAnnotations) port.annotations())
                                .build());
            } else {
                log.warn("Your device {} port {} status can't be changed to {}",
                        deviceId, portNumber, enable);
            }
        });
    }

    @Override
    public void triggerDisconnect(DeviceId deviceId) {
        log.debug("Forcing disconnect for device {}", deviceId);
        controller.disconnectDevice(deviceId, true);
    }

    private boolean isTcpConnectionAvailable(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        String ip;
        int port;
        if (device != null) {
            ip = device.annotations().value(IPADDRESS);
            port = Integer.parseInt(device.annotations().value(PORT));
        } else {
            Triple<String, Integer, Optional<String>> info = extractIpPortPath(deviceId);
            ip = info.getLeft();
            port = info.getMiddle();
        }
        // FIXME just opening TCP session probably is not the appropriate
        // method to test reachability.
        //test connection to device opening a socket to it.
        log.debug("Testing reachability for {}:{}", ip, port);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), 1000);
            log.debug("rechability of {}, {}, {}", deviceId, socket.isConnected(), !socket.isClosed());
            boolean isConnected = socket.isConnected() && !socket.isClosed();
            socket.close();
            return isConnected;
        } catch (IOException e) {
            log.info("Device {} is not reachable", deviceId);
            log.debug("  error details", e);
            return false;
        }
    }

    private ScheduledFuture schedulePolling() {
        return pollingExecutor.scheduleAtFixedRate(exceptionSafe(this::checkAndUpdateDevices),
                pollFrequency / 10,
                pollFrequency, TimeUnit.SECONDS);
    }

    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled Exception", e);
            }
        };
    }

    private ScheduledFuture scheduleConnectDevices() {
        return reconnectionExecutor.scheduleAtFixedRate(this::connectDevices, 0, retryFrequency,
                                                        TimeUnit.SECONDS);
    }

    /* Connecting devices with initial config. This will keep on retrying infinitely for all devices which are not
    connecting with ONOS. To stop retry, please remove device from netcfg*/
    private void connectDevices() {
        Set<DeviceId> deviceSubjects = cfgService.getSubjects(DeviceId.class, NetconfDeviceConfig.class);
        deviceSubjects.parallelStream().filter(deviceId -> !deviceService.isAvailable(deviceId)).forEach(deviceId -> {
            connectionExecutor.execute(exceptionSafe(() -> runElectionFor(deviceId)));
        });
    }

    //updating keys and device info
    private void checkAndUpdateDevices() {
        Set<DeviceId> deviceSubjects = cfgService.getSubjects(DeviceId.class, NetconfDeviceConfig.class);
        try {
            scheduledTaskPool.submit(() -> {
                deviceSubjects.parallelStream().forEach(deviceId -> {
                    log.debug("check and update {}", deviceId);
                    NetconfDeviceConfig config = cfgService.getConfig(deviceId, NetconfDeviceConfig.class);
                    storeDeviceKey(config.sshKey(), config.username(), config.password(), deviceId);
                    discoverOrUpdatePorts(deviceId);
                });
            }).get();
        } catch (ExecutionException e) {
            log.error("Can't update the devices due to {}", e.getMessage());
        } catch (InterruptedException | CancellationException e) {
            log.info("Update device is cancelled due to {}", e.getMessage());
        }
    }

    //Saving device keys in the store
    private void storeDeviceKey(String sshKey, String username, String password, DeviceId deviceId) {
        if (sshKey.equals("")) {
            deviceKeyAdminService.addKey(
                    DeviceKey.createDeviceKeyUsingUsernamePassword(
                            DeviceKeyId.deviceKeyId(deviceId.toString()), null, username, password));
        } else {
            deviceKeyAdminService.addKey(
                    DeviceKey.createDeviceKeyUsingSshKey(
                            DeviceKeyId.deviceKeyId(deviceId.toString()), null, username, password, sshKey));
        }
    }

    //running an election and applying the role to a given device
    private void runElectionFor(DeviceId deviceId) {
        //Triggering an election for the deviceId thus only master will connect
        if (!deviceId.uri().getScheme().equals(SCHEME_NAME)) {
            // not under my scheme, skipping
            log.debug("{} not of schema {}, skipping", deviceId, SCHEME_NAME);
            return;
        }
        connectionExecutor.submit(exceptionSafe(() -> {
            CompletableFuture<MastershipRole> role = mastershipService.requestRoleFor(deviceId);
            try {
                roleChanged(deviceId, role.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Can't get role for {} ", deviceId, e);
            }
        }));
    }

    //initiating the SSh connection the a given device.
    private void initiateConnection(DeviceId deviceId) {

        if (!isReachable(deviceId)) {
            log.warn("Can't connect to device {}", deviceId);
            return;
        }

        try {
            NetconfDevice deviceNetconf = controller.connectDevice(deviceId);
            if (deviceNetconf != null) {
                //storeDeviceKey(config.sshKey(), config.username(), config.password(), deviceId);
                NetconfDeviceConfig config = cfgService.getConfig(deviceId, NetconfDeviceConfig.class);
                //getting the device description
                DeviceDescription deviceDescription = getDeviceDescription(deviceId, config);
                //connecting device to ONOS
                log.debug("Connected NETCONF device {}, on {}:{} {} with username {}",
                        deviceId, config.ip(), config.port(),
                        (config.path().isPresent() ? "/" + config.path().get() : ""),
                        config.username());
                providerService.deviceConnected(deviceId, deviceDescription);
            } else {
                mastershipService.relinquishMastership(deviceId);
                deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(deviceId.toString()));
                log.error("Can't connect to NETCONF device {}", deviceId);
            }
        } catch (Exception e) {
            mastershipService.relinquishMastership(deviceId);
            deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(deviceId.toString()));
            throw new IllegalStateException(new NetconfException(
                    "Can't connect to NETCONF device " + deviceId, e));

        }

    }

    private DeviceDescription getDeviceDescription(DeviceId deviceId, NetconfDeviceConfig config) {
        Driver driver = driverService.getDriver(deviceId);
        if (driver.hasBehaviour(DeviceDescriptionDiscovery.class)) {
            final DriverData data = new DefaultDriverData(driver, deviceId);
            final DriverHandler handler = new DefaultDriverHandler(data);
            //creating the behaviour because the core has yet no notion of device.
            DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                    driver.createBehaviour(handler, DeviceDescriptionDiscovery.class);
            return getDeviceRepresentation(deviceId, config, deviceDescriptionDiscovery);
        } else {
            return existingOrEmptyDescription(deviceId, config);
        }
    }

    private DeviceDescription getDeviceRepresentation(DeviceId deviceId, NetconfDeviceConfig config,
                                                      DeviceDescriptionDiscovery deviceDescriptionDiscovery) {

        DeviceDescription existingOrEmptyDescription = existingOrEmptyDescription(deviceId, config);
        DeviceDescription newDescription = deviceDescriptionDiscovery.discoverDeviceDetails();
        if (newDescription == null) {
            return existingOrEmptyDescription;
        }
        //merging and returning
        return new DefaultDeviceDescription(newDescription, true,
                DefaultAnnotations.merge((DefaultAnnotations) newDescription.annotations(),
                        existingOrEmptyDescription.annotations()));
    }

    private DeviceDescription existingOrEmptyDescription(DeviceId deviceId, NetconfDeviceConfig config) {
        Device device = deviceService.getDevice(deviceId);

        if (deviceService.getDevice(deviceId) != null) {
            //getting the previous description
            return new DefaultDeviceDescription(device.id().uri(), device.type(),
                    device.manufacturer(), device.hwVersion(),
                    device.swVersion(), device.serialNumber(),
                    device.chassisId(), (SparseAnnotations) device.annotations());
        }

        ChassisId cid = new ChassisId();
        String ipAddress = config.ip().toString();
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(IPADDRESS, ipAddress)
                .set(PORT, String.valueOf(config.port()))
                .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase());
        if (config.path().isPresent()) {
            annotations.set(PATH, config.path().get());
        }
        return new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, cid, true, annotations.build());
    }

    /**
     * Will appropriately create connections with the device.
     * For Master role: will create secure transport and proxy sessions.
     * For Standby role: will create only proxy session and disconnect secure transport session.
     * For none role: will disconnect all sessions.
     *
     * @param deviceId device id
     * @param newRole new role
     */
    private void initiateConnection(DeviceId deviceId, MastershipRole newRole) {
        try {
            if (isReachable(deviceId)) {
                NetconfDevice device = null;
                if (newRole.equals(MastershipRole.MASTER)) {
                    device = controller.connectDevice(deviceId, true);
                } else if (newRole.equals(MastershipRole.STANDBY)) {
                    device = controller.connectDevice(deviceId, false);
                }

                if (device != null) {
                    providerService.receivedRoleReply(deviceId, newRole, newRole);
                } else {
                    providerService.receivedRoleReply(deviceId, newRole, MastershipRole.NONE);
                }

            }
        } catch (Exception e) {
            if (deviceService.getDevice(deviceId) != null) {
                providerService.deviceDisconnected(deviceId);
            }
            deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(deviceId.toString()));
            throw new IllegalStateException(new NetconfException(
                    "Can't connect to NETCONF device " + deviceId, e));
        }
    }

    private void discoverOrUpdatePorts(DeviceId deviceId) {
        retriedPortDiscoveryMap.put(deviceId, new AtomicInteger(0));
        AtomicInteger count = retriedPortDiscoveryMap.get(deviceId);
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            log.debug("Can't reach device {}, not updating ports", deviceId);
            return;
        }
        if (forcePortUpdates || (deviceService.getPorts(deviceId).isEmpty()
                && count != null && count.getAndIncrement() < maxRetries)) {
            if (device.is(DeviceDescriptionDiscovery.class)) {
                providerService.updatePorts(deviceId,
                        device.as(DeviceDescriptionDiscovery.class).discoverPortDetails());
            } else {
                log.warn("No DeviceDescription behaviour for device {}", deviceId);
            }

        }
        updatePortStatistics(device);
    }

    private void updatePortStatistics(Device device) {
        if (device.is(PortStatisticsDiscovery.class)) {
            PortStatisticsDiscovery d = device.as(PortStatisticsDiscovery.class);
            Collection<PortStatistics> portStatistics = d.discoverPortStatistics();
            if (portStatistics != null) {
                providerService.updatePortStatistics(device.id(),
                        portStatistics);
            }
        } else {
            log.debug("No port statistics getter behaviour for device {}",
                    device.id());
        }
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

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(NetconfDeviceConfig.class) && event.config().isPresent()) {
                connectionExecutor.execute(exceptionSafe(() ->
                        runElectionFor((DeviceId) event.config().get().subject())));
            } else {
                log.warn("Incorrect or absent Class for Netconf Configuration");
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.configClass().equals(NetconfDeviceConfig.class)) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    /**
     * Listener for Netconf Controller Events.
     */
    private class InnerNetconfDeviceListener implements NetconfDeviceListener {

        @Override
        public void deviceAdded(DeviceId deviceId) {
            //no-op
            log.debug("Netconf device {} added to Netconf controller", deviceId);
        }

        @Override
        public void deviceRemoved(DeviceId deviceId) {
            Preconditions.checkNotNull(deviceId, ISNULL);

            if (deviceService.getDevice(deviceId) != null) {
                providerService.deviceDisconnected(deviceId);
                retriedPortDiscoveryMap.remove(deviceId);
                log.debug("Netconf device {} removed from Netconf controller", deviceId);
            } else {
                log.warn("Netconf device {} does not exist in the store, " +
                        "it may already have been removed", deviceId);
            }
        }
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            connectionExecutor.submit(exceptionSafe(() -> discoverOrUpdatePorts(event.subject().id())));
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            if (event.type() != DeviceEvent.Type.DEVICE_ADDED &&
                    event.type() != DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED) {
                return false;
            }
            return (SCHEME_NAME.equalsIgnoreCase(event.subject().annotations().value(AnnotationKeys.PROTOCOL)) ||
                    (SCHEME_NAME.equalsIgnoreCase(event.subject().id().uri().getScheme()))) &&
                    mastershipService.isLocalMaster(event.subject().id());
        }
    }
}
