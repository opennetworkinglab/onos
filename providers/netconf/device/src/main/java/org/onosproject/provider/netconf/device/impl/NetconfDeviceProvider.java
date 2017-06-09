/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DefaultDeviceDescription;
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
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.config.NetconfDeviceConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an NETCONF controller to detect device.
 */
@Component(immediate = true)
public class NetconfDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceKeyAdminService deviceKeyAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;


    protected static final String APP_NAME = "org.onosproject.netconf";
    protected static final String SCHEME_NAME = "netconf";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.netconf.provider.device";
    private static final String UNKNOWN = "unknown";
    protected static final String ISNULL = "NetconfDeviceInfo is null";
    private static final String IPADDRESS = "ipaddress";
    private static final String NETCONF = "netconf";
    private static final String PORT = "port";
    private static final int CORE_POOL_SIZE = 10;

    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 30;
    @Property(name = "pollFrequency", intValue = DEFAULT_POLL_FREQUENCY_SECONDS,
            label = "Configure poll frequency for port status and statistics; " +
                    "default is 30 sec")
    private int pollFrequency = DEFAULT_POLL_FREQUENCY_SECONDS;

    private static final int DEFAULT_MAX_RETRIES = 5;
    @Property(name = "maxRetries", intValue = DEFAULT_MAX_RETRIES,
            label = "Configure maximum allowed number of retries for obtaining list of ports; " +
                    "default is 5 times")
    private int maxRetries = DEFAULT_MAX_RETRIES;

    protected ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/netconfdeviceprovider",
                                                           "device-installer-%d", log));
    protected ScheduledExecutorService connectionExecutor
            = newScheduledThreadPool(CORE_POOL_SIZE,
                                     groupedThreads("onos/netconfdeviceprovider",
                                                    "connection-executor-%d", log));

    protected DeviceProviderService providerService;
    private NetconfDeviceListener innerNodeListener = new InnerNetconfDeviceListener();
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final Map<DeviceId, AtomicInteger> retriedPortDiscoveryMap = new ConcurrentHashMap<>();
    protected ScheduledFuture<?> scheduledTask;

    protected final List<ConfigFactory> factories = ImmutableList.of(
            // TODO consider moving Config registration to NETCONF ctl bundle
            new ConfigFactory<DeviceId, NetconfDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    NetconfDeviceConfig.class, NetconfDeviceConfig.CONFIG_KEY) {
                @Override
                public NetconfDeviceConfig createConfig() {
                    return new NetconfDeviceConfig();
                }
            },
            new ConfigFactory<ApplicationId, NetconfProviderConfig>(APP_SUBJECT_FACTORY,
                                                                    NetconfProviderConfig.class,
                                                                    "netconf_devices",
                                                                    true) {
                @Override
                public NetconfProviderConfig createConfig() {
                    return new NetconfProviderConfig();
                }
            });

    protected final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private ApplicationId appId;
    private boolean active;


    @Activate
    public void activate(ComponentContext context) {
        active = true;
        componentConfigService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        factories.forEach(cfgService::registerConfigFactory);
        cfgService.addListener(cfgListener);
        controller.addDeviceListener(innerNodeListener);
        deviceService.addListener(deviceListener);
        translateConfig();
        executor.execute(NetconfDeviceProvider.this::connectDevices);
        modified(context);
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        deviceService.removeListener(deviceListener);
        active = false;
        controller.getNetconfDevices().forEach(id -> {
            deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(id.toString()));
            controller.disconnectDevice(id, true);
        });
        controller.removeDeviceListener(innerNodeListener);
        deviceService.removeListener(deviceListener);
        providerRegistry.unregister(this);
        providerService = null;
        retriedPortDiscoveryMap.clear();
        factories.forEach(cfgService::unregisterConfigFactory);
        scheduledTask.cancel(true);
        executor.shutdown();
        log.info("Stopped");
    }


    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            pollFrequency = Tools.getIntegerProperty(properties, "pollFrequency",
                                                     DEFAULT_POLL_FREQUENCY_SECONDS);
            log.info("Configured. Poll frequency is configured to {} seconds", pollFrequency);

            maxRetries = Tools.getIntegerProperty(properties, "maxRetries",
                    DEFAULT_MAX_RETRIES);
            log.info("Configured. Number of retries is configured to {} times", maxRetries);
        }
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = schedulePolling();
    }

    public NetconfDeviceProvider() {
        super(new ProviderId(SCHEME_NAME, DEVICE_PROVIDER_PACKAGE));
    }

    // Checks connection to devices in the config file
    // every DEFAULT_POLL_FREQUENCY_SECONDS seconds.
    private ScheduledFuture schedulePolling() {
        return connectionExecutor.scheduleAtFixedRate(exceptionSafe(this::checkAndUpdateDevices),
                                                      pollFrequency / 10,
                                                      pollFrequency, TimeUnit.SECONDS);
    }

    private Runnable exceptionSafe(Runnable runnable) {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("Unhandled Exception", e);
                }
            }
        };
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO: This will be implemented later.
        log.debug("Should be triggering probe on device {}", deviceId);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        if (active) {
            switch (newRole) {
                case MASTER:
                    initiateConnection(deviceId, newRole);
                    log.debug("Accepting mastership role change to {} for device {}", newRole, deviceId);
                    break;
                case STANDBY:
                    controller.disconnectDevice(deviceId, false);
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
    public boolean isReachable(DeviceId deviceId) {
        //FIXME this is a workaround util device state is shared
        // between controller instances.
        Device device = deviceService.getDevice(deviceId);
        String ip;
        int port;
        if (device != null) {
            ip = device.annotations().value(IPADDRESS);
            port = Integer.parseInt(device.annotations().value(PORT));
        } else {
            String[] info = deviceId.toString().split(":");
            if (info.length == 3) {
                ip = info[1];
                port = Integer.parseInt(info[2]);
            } else {
                ip = Arrays.asList(info).stream().filter(el -> !el.equals(info[0])
                        && !el.equals(info[info.length - 1]))
                        .reduce((t, u) -> t + ":" + u)
                        .get();
                log.debug("ip v6 {}", ip);
                port = Integer.parseInt(info[info.length - 1]);
            }
        }
        //test connection to device opening a socket to it.
        try (Socket socket = new Socket(ip, port)) {
            log.debug("rechability of {}, {}, {}", deviceId, socket.isConnected(), !socket.isClosed());
            return socket.isConnected() && !socket.isClosed();
        } catch (IOException e) {
            log.info("Device {} is not reachable", deviceId);
            return false;
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        // TODO if required
    }

    private class InnerNetconfDeviceListener implements NetconfDeviceListener {


        @Override
        public void deviceAdded(DeviceId deviceId) {
            //no-op
            log.debug("Netconf device {} added to Netconf subController", deviceId);
        }

        @Override
        public void deviceRemoved(DeviceId deviceId) {
            Preconditions.checkNotNull(deviceId, ISNULL);

            if (deviceService.getDevice(deviceId) != null) {
                providerService.deviceDisconnected(deviceId);
                retriedPortDiscoveryMap.remove(deviceId);
                log.debug("Netconf device {} removed from Netconf subController", deviceId);
            } else {
                log.warn("Netconf device {} does not exist in the store, " +
                                 "it may already have been removed", deviceId);
            }
        }
    }

    private void connectDevices() {
        Set<DeviceId> deviceSubjects =
                cfgService.getSubjects(DeviceId.class, NetconfDeviceConfig.class);
        deviceSubjects.forEach(deviceId -> {
            connectDevice(cfgService.getConfig(deviceId, NetconfDeviceConfig.class));
        });
    }


    private void connectDevice(NetconfDeviceConfig config) {
        if (config == null) {
            return;
        }
        DeviceId deviceId = config.subject();
        if (!deviceId.uri().getScheme().equals(SCHEME_NAME)) {
            // not under my scheme, skipping
            log.trace("{} not my scheme, skipping", deviceId);
            return;
        }
        DeviceDescription deviceDescription = createDeviceRepresentation(deviceId, config);
        log.debug("Connecting NETCONF device {}, on {}:{} with username {}",
                  deviceId, config.ip(), config.port(), config.username());
        storeDeviceKey(config.sshKey(), config.username(), config.password(), deviceId);
        retriedPortDiscoveryMap.putIfAbsent(deviceId, new AtomicInteger(0));
        if (deviceService.getDevice(deviceId) == null) {
            providerService.deviceConnected(deviceId, deviceDescription);
        }
        try {
            checkAndUpdateDevice(deviceId, deviceDescription);
        } catch (Exception e) {
            log.error("Unhandled exception checking {}", deviceId, e);
        }
    }

    private void checkAndUpdateDevice(DeviceId deviceId, DeviceDescription deviceDescription) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            log.debug("Device {} has not been added to store, " +
                             "since it's not reachable", deviceId);
        } else {
            boolean isReachable = isReachable(deviceId);
            if (isReachable && !deviceService.isAvailable(deviceId)) {
                if (device.is(DeviceDescriptionDiscovery.class)) {
                    if (mastershipService.isLocalMaster(deviceId)) {
                        DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                                device.as(DeviceDescriptionDiscovery.class);
                        DeviceDescription updatedDeviceDescription =
                                deviceDescriptionDiscovery.discoverDeviceDetails();
                        if (updatedDeviceDescription != null &&
                                !descriptionEquals(device, updatedDeviceDescription)) {
                            providerService.deviceConnected(
                                    deviceId, new DefaultDeviceDescription(
                                            updatedDeviceDescription, true,
                                            updatedDeviceDescription.annotations()));
                        } else if (updatedDeviceDescription == null) {
                            providerService.deviceConnected(
                                    deviceId, new DefaultDeviceDescription(
                                            deviceDescription, true,
                                            deviceDescription.annotations()));
                        }
                    }
                } else {
                    log.warn("No DeviceDescriptionDiscovery behaviour for device {} " +
                                     "using DefaultDeviceDescription", deviceId);
                    providerService.deviceConnected(
                            deviceId, new DefaultDeviceDescription(
                                    deviceDescription, true, deviceDescription.annotations()));
                }
            } else if (!isReachable && deviceService.isAvailable(deviceId)) {
                providerService.deviceDisconnected(deviceId);
            } else if (isReachable && deviceService.isAvailable(deviceId) &&
                    mastershipService.isLocalMaster(deviceId)) {

                //if ports are not discovered, retry the discovery
                if (deviceService.getPorts(deviceId).isEmpty() &&
                        retriedPortDiscoveryMap.get(deviceId).getAndIncrement() < maxRetries) {
                    discoverPorts(deviceId);
                }
                updatePortStatistics(device);
            }
        }
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

    private boolean descriptionEquals(Device device, DeviceDescription updatedDeviceDescription) {
        return Objects.equal(device.id().uri(), updatedDeviceDescription.deviceUri())
                && Objects.equal(device.type(), updatedDeviceDescription.type())
                && Objects.equal(device.manufacturer(), updatedDeviceDescription.manufacturer())
                && Objects.equal(device.hwVersion(), updatedDeviceDescription.hwVersion())
                && Objects.equal(device.swVersion(), updatedDeviceDescription.swVersion())
                && Objects.equal(device.serialNumber(), updatedDeviceDescription.serialNumber())
                && Objects.equal(device.chassisId(), updatedDeviceDescription.chassisId())
                && Objects.equal(device.annotations(), updatedDeviceDescription.annotations());
    }

    private void checkAndUpdateDevices() {
        Set<DeviceId> deviceSubjects =
                cfgService.getSubjects(DeviceId.class, NetconfDeviceConfig.class);
        deviceSubjects.forEach(deviceId -> {
            NetconfDeviceConfig config =
                    cfgService.getConfig(deviceId, NetconfDeviceConfig.class);
            DeviceDescription deviceDescription = createDeviceRepresentation(deviceId, config);
            storeDeviceKey(config.sshKey(), config.username(), config.password(), deviceId);
            checkAndUpdateDevice(deviceId, deviceDescription);
        });
    }

    private DeviceDescription createDeviceRepresentation(DeviceId deviceId, NetconfDeviceConfig config) {
        Preconditions.checkNotNull(deviceId, ISNULL);
        //Netconf configuration object
        ChassisId cid = new ChassisId();
        String ipAddress = config.ip().toString();
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(IPADDRESS, ipAddress)
                .set(PORT, String.valueOf(config.port()))
                .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase())
                .build();
        return new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.SWITCH,
                UNKNOWN, UNKNOWN,
                UNKNOWN, UNKNOWN,
                cid, false,
                annotations);
    }

    private void storeDeviceKey(String sshKey, String username, String password, DeviceId deviceId) {
        if (sshKey.equals("")) {
            deviceKeyAdminService.addKey(
                    DeviceKey.createDeviceKeyUsingUsernamePassword(
                            DeviceKeyId.deviceKeyId(deviceId.toString()),
                            null, username, password));
        } else {
            deviceKeyAdminService.addKey(
                    DeviceKey.createDeviceKeyUsingSshKey(
                            DeviceKeyId.deviceKeyId(deviceId.toString()),
                            null, username, password,
                            sshKey));
        }
    }

    private void initiateConnection(DeviceId deviceId, MastershipRole newRole) {
        try {
            if (isReachable(deviceId)) {
                controller.connectDevice(deviceId);
                providerService.receivedRoleReply(deviceId, newRole, MastershipRole.MASTER);
            }
        } catch (Exception e) {
            if (deviceService.getDevice(deviceId) != null) {
                providerService.deviceDisconnected(deviceId);
            }
            deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(deviceId.toString()));
            throw new RuntimeException(new NetconfException(
                    "Can't connect to NETCONF device " + deviceId, e));

        }
    }

    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        //TODO remove when PortDiscovery is removed from master
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            providerService.updatePorts(deviceId,
                                        portConfig.getPorts());
        } else if (device.is(DeviceDescriptionDiscovery.class)) {
            DeviceDescriptionDiscovery deviceDescriptionDiscovery =
                    device.as(DeviceDescriptionDiscovery.class);
            providerService.updatePorts(deviceId,
                                        deviceDescriptionDiscovery.discoverPortDetails());
        } else {
            log.warn("No portGetter behaviour for device {}", deviceId);
        }

        // Port statistics discovery
        updatePortStatistics(device);
    }

    /**
     * Return the DeviceId about the device containing the URI.
     *
     * @param ip   IP address
     * @param port port number
     * @return DeviceId
     */
    public DeviceId getDeviceId(String ip, int port) {
        try {
            return DeviceId.deviceId(new URI(NETCONF, ip + ":" + port, null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to build deviceID for device "
                                                       + ip + ":" + port, e);
        }
    }


    protected void translateConfig() {
        NetconfProviderConfig cfg = cfgService.getConfig(appId, NetconfProviderConfig.class);
        if (cfg != null) {
            try {
                cfg.getDevicesAddresses().forEach(addr -> {
                    DeviceId deviceId = getDeviceId(addr.ip().toString(), addr.port());
                    log.info("Translating config for device {}", deviceId);
                    if (cfgService.getConfig(deviceId, NetconfDeviceConfig.class) == null) {
                        ObjectMapper mapper = new ObjectMapper();
                        ObjectNode device = mapper.createObjectNode();
                        device.put("ip", addr.ip().toString());
                        device.put("port", addr.port());
                        device.put("username", addr.name());
                        device.put("password", addr.password());
                        device.put("sshkey", addr.sshkey());
                        cfgService.applyConfig(deviceId, NetconfDeviceConfig.class, device);
                    } else {
                        // This is a corner case where new updated config is
                        // pushed with old /app tree after an initial with the
                        // new device/ tree. Since old method will be deprecated
                        // it's ok to ignore
                        log.warn("Config for device {} already exists, ignoring", deviceId);
                    }

                });
            } catch (ConfigException e) {
                log.error("Cannot read config error " + e);
            }
        }
    }

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(NetconfDeviceConfig.class)) {
                executor.execute(() -> connectDevice((NetconfDeviceConfig) event.config().get()));
            } else {
                log.warn("Injecting device via this Json is deprecated, " +
                                 "please put configuration under devices/ as shown in the wiki");
                translateConfig();
            }

        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.configClass().equals(NetconfDeviceConfig.class) ||
                    event.configClass().equals(NetconfProviderConfig.class)) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if ((event.type() == DeviceEvent.Type.DEVICE_ADDED)) {
                executor.execute(() -> discoverPorts(event.subject().id()));
            } else if ((event.type() == DeviceEvent.Type.DEVICE_REMOVED)) {
                log.debug("removing device {}", event.subject().id());
                controller.disconnectDevice(event.subject().id(), true);
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            if (mastershipService.getMasterFor(event.subject().id()) == null) {
                return true;
            }
            return (SCHEME_NAME.equalsIgnoreCase(event.subject().annotations().value(AnnotationKeys.PROTOCOL)) ||
                    (SCHEME_NAME.equalsIgnoreCase(event.subject().id().uri().getScheme()))) &&
                    mastershipService.isLocalMaster(event.subject().id());
        }
    }
}
