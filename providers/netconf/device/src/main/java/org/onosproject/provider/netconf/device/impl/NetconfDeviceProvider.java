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

import com.google.common.base.Preconditions;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
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
import org.onosproject.net.behaviour.PortStatsQuery;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an NETCONF controller to detect device.
 */
@Component(immediate = true)
public class NetconfDeviceProvider extends AbstractProvider
        implements DeviceProvider {

    public static final String ACTIVE = "active";
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
    protected ClusterService clusterService;

    private static final String APP_NAME = "org.onosproject.netconf";
    private static final String SCHEME_NAME = "netconf";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.netconf.provider.device";
    private static final String UNKNOWN = "unknown";
    protected static final String ISNULL = "NetconfDeviceInfo is null";
    private static final String IPADDRESS = "ipaddress";
    private static final String NETCONF = "netconf";
    private static final String PORT = "port";
    //FIXME eventually a property
    private static final int ISREACHABLE_TIMEOUT = 2000;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/netconfdeviceprovider",
                                                           "device-installer-%d", log));

    private DeviceProviderService providerService;
    private NetconfDeviceListener innerNodeListener = new InnerNetconfDeviceListener();
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private NodeId localNodeId;

    private final ConfigFactory factory =
            new ConfigFactory<ApplicationId, NetconfProviderConfig>(APP_SUBJECT_FACTORY,
                                                                    NetconfProviderConfig.class,
                                                                    "devices",
                                                                    true) {
                @Override
                public NetconfProviderConfig createConfig() {
                    return new NetconfProviderConfig();
                }
            };
    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private ApplicationId appId;
    private boolean active;

    private static final int POLL_PERIOD = 5_000; // milliseconds
    private final ScheduledExecutorService scheduledExecutorService = SharedScheduledExecutors.getPoolThreadExecutor();
    private ScheduledFuture<?> poller;


    @Activate
    public void activate() {
        active = true;
        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgListener);
        controller.addDeviceListener(innerNodeListener);
        deviceService.addListener(deviceListener);
        executor.execute(NetconfDeviceProvider.this::connectDevices);
        localNodeId = clusterService.getLocalNode().id();
        /* Poll devices every 1000 milliseconds */
        poller = scheduledExecutorService.scheduleAtFixedRate(this::pollDevices, 1_000, POLL_PERIOD, MILLISECONDS);
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
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
        cfgService.unregisterConfigFactory(factory);
        if (poller != null) {
            poller.cancel(false);
        }
        executor.shutdown();
        log.info("Stopped");
    }

    public NetconfDeviceProvider() {
        super(new ProviderId(SCHEME_NAME, DEVICE_PROVIDER_PACKAGE));
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO: This will be implemented later.
        log.info("Triggering probe on device {}", deviceId);
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
        Socket socket = null;
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
        try {
            socket = new Socket(ip, port);
            log.debug("rechability of {}, {}, {}", deviceId, socket.isConnected() && !socket.isClosed());
            return socket.isConnected() && !socket.isClosed();
        } catch (IOException e) {
            log.info("Device {} is not reachable", deviceId);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.debug("Test Socket failed {} ", deviceId);
                    return false;
                }
            }
        }
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        // TODO if required
    }

    private void pollDevices() {
        for (Device device: deviceService.getAvailableDevices(ROUTER)) {
            if (mastershipService.isLocalMaster(device.id())) {
                executor.execute(() -> pollingTask(device.id()));
            }
        }
    }

    private void pollingTask(DeviceId deviceId) {
        log.debug("Polling device {}...", deviceId);
        if (isReachable(deviceId)) {
            log.debug("Netconf device {} is reachable, updating ports and stats", deviceId);
            updatePortsAndStats(deviceId);
        } else {
            log.debug("Netconf device {} is unreachable, disconnecting", deviceId);
            disconnectDevice(deviceId);
        }
    }

    private void updatePortsAndStats(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        /* Update port description first */
        discoverPorts(deviceId);
        /* If driver has PortStatsQuery feature implemented, query port stats on the device
         * and push the stats into ONOS core
         */
        if (device.is(PortStatsQuery.class)) {
            PortStatsQuery statsQuery = device.as(PortStatsQuery.class);
            Collection<PortStatistics> portStats = statsQuery.getPortStatistics(deviceId);
            if (portStats != null) {
                providerService.updatePortStatistics(deviceId, portStats);
            }
        } else {
            log.warn("No portStatsQuery behaviour for device {}", deviceId);
        }
    }

    private void disconnectDevice(DeviceId deviceId) {
        Preconditions.checkNotNull(deviceId, ISNULL);
        log.debug("Netconf device {} removed from Netconf subController", deviceId);
        deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(deviceId.toString()));
        controller.disconnectDevice(deviceId, true);
        providerService.deviceDisconnected(deviceId);
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
            log.debug("Netconf device {} removed from Netconf subController", deviceId);
            providerService.deviceDisconnected(deviceId);
        }
    }

    private void connectDevices() {
        NetconfProviderConfig cfg = cfgService.getConfig(appId, NetconfProviderConfig.class);
        if (cfg != null) {
            try {
                cfg.getDevicesAddresses().stream().forEach(addr -> {
                    DeviceId deviceId = getDeviceId(addr.ip().toString(), addr.port());
                    Preconditions.checkNotNull(deviceId, ISNULL);
                    //Netconf configuration object
                    ChassisId cid = new ChassisId();
                    String ipAddress = addr.ip().toString();
                    SparseAnnotations annotations = DefaultAnnotations.builder()
                            .set(IPADDRESS, ipAddress)
                            .set(PORT, String.valueOf(addr.port()))
                            .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase())
                            .build();
                    DeviceDescription deviceDescription = new DefaultDeviceDescription(
                            deviceId.uri(),
                            Device.Type.SWITCH,
                            UNKNOWN, UNKNOWN,
                            UNKNOWN, UNKNOWN,
                            cid,
                            annotations);
                    deviceKeyAdminService.addKey(
                            DeviceKey.createDeviceKeyUsingUsernamePassword(
                                    DeviceKeyId.deviceKeyId(deviceId.toString()),
                                    null, addr.name(), addr.password()));
                    providerService.deviceConnected(deviceId, deviceDescription);


                });
            } catch (ConfigException e) {
                log.error("Cannot read config error " + e);
            }
        }
    }

    private void initiateConnection(DeviceId deviceId, MastershipRole newRole) {
        try {
            if (isReachable(deviceId)) {
                controller.connectDevice(deviceId);
                providerService.receivedRoleReply(deviceId, newRole, MastershipRole.MASTER);
            } else {
                return;
            }
        } catch (Exception e) {
            if (deviceService.getDevice(deviceId) != null) {
                providerService.deviceDisconnected(deviceId);
            }
            deviceKeyAdminService.removeKey(DeviceKeyId.deviceKeyId(deviceId.toString()));
            throw new RuntimeException(new NetconfException(
                    "Can't connect to NETCONF " + "device on " + deviceId + ":" + deviceId, e));

        }
    }

    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            providerService.updatePorts(deviceId,
                                        portConfig.getPorts());
        } else {
            log.warn("No portGetter behaviour for device {}", deviceId);
        }
    }

    /**
     * Return the DeviceId about the device containing the URI.
     *
     * @param ip IP address
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

    /**
     * Listener for configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(NetconfDeviceProvider.this::connectDevices);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(NetconfProviderConfig.class) &&
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
                deviceService.getDevice(event.subject().id()).annotations().keys();
                controller.disconnectDevice(event.subject().id(), true);
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            if (mastershipService.getMasterFor(event.subject().id()) == null) {
                return true;
            }
            return event.subject().annotations().value(AnnotationKeys.PROTOCOL)
                    .equals(SCHEME_NAME.toUpperCase()) &&
                    mastershipService.getMasterFor(event.subject().id()).equals(localNodeId);
        }
    }
}
