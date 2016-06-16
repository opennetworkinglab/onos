/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
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
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceListener;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private static final String APP_NAME = "org.onosproject.netconf";
    private static final String SCHEME_NAME = "netconf";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.netconf.provider.device";
    private static final String UNKNOWN = "unknown";

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/netconfdeviceprovider", "device-installer-%d", log));

    private DeviceProviderService providerService;
    private NetconfDeviceListener innerNodeListener = new InnerNetconfDeviceListener();

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
    private final NetworkConfigListener cfgLister = new InternalNetworkConfigListener();
    private ApplicationId appId;


    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgLister);
        controller.addDeviceListener(innerNodeListener);
        executor.execute(NetconfDeviceProvider.this::connectDevices);
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        controller.removeDeviceListener(innerNodeListener);
        controller.getNetconfDevices().forEach(id ->
            controller.removeDevice(controller.getDevicesMap().get(id)
                                            .getDeviceInfo()));
        providerRegistry.unregister(this);
        providerService = null;
        cfgService.unregisterConfigFactory(factory);
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
        // TODO: This will be implemented later.
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        NetconfDevice netconfDevice = controller.getNetconfDevice(deviceId);
        if (netconfDevice == null) {
            log.debug("Requested device id: {} is not associated to any " +
                              "NETCONF Device", deviceId.toString());
            return false;
        }
        return netconfDevice.isActive();
    }

    @Override
    public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    @Override
    public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    private class InnerNetconfDeviceListener implements NetconfDeviceListener {

        private static final String IPADDRESS = "ipaddress";
        protected static final String ISNULL = "NetconfDeviceInfo is null";

        @Override
        public void deviceAdded(NetconfDeviceInfo nodeId) {
            Preconditions.checkNotNull(nodeId, ISNULL);
            DeviceId deviceId = nodeId.getDeviceId();
            //Netconf configuration object
            ChassisId cid = new ChassisId();
            String ipAddress = nodeId.ip().toString();
            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set(IPADDRESS, ipAddress)
                    .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase())
                    .build();
            DeviceDescription deviceDescription = new DefaultDeviceDescription(
                    deviceId.uri(),
                    Device.Type.SWITCH,
                    UNKNOWN, UNKNOWN,
                    UNKNOWN, UNKNOWN,
                    cid,
                    annotations);
            providerService.deviceConnected(deviceId, deviceDescription);
        }

        @Override
        public void deviceRemoved(NetconfDeviceInfo nodeId) {
            Preconditions.checkNotNull(nodeId, ISNULL);
            DeviceId deviceId = nodeId.getDeviceId();
            providerService.deviceDisconnected(deviceId);

        }
    }

    private void connectDevices() {
        NetconfProviderConfig cfg = cfgService.getConfig(appId, NetconfProviderConfig.class);
        if (cfg != null) {
            try {
                cfg.getDevicesAddresses().stream()
                        .forEach(addr -> {
                                     try {
                                         NetconfDeviceInfo netconf = new NetconfDeviceInfo(addr.name(),
                                                               addr.password(),
                                                               addr.ip(),
                                                               addr.port());
                                         controller.connectDevice(netconf);
                                         Device device = deviceService.getDevice(netconf.getDeviceId());
                                         if (device.is(PortDiscovery.class)) {
                                             PortDiscovery portConfig = device.as(PortDiscovery.class);
                                             if (portConfig != null) {
                                                 providerService.updatePorts(netconf.getDeviceId(),
                                                                             portConfig.getPorts());
                                             }
                                         } else {
                                             log.warn("No portGetter behaviour for device {}", netconf.getDeviceId());
                                         }

                                     } catch (IOException e) {
                                         throw new RuntimeException(
                                                 new NetconfException(
                                                         "Can't connect to NETCONF " +
                                                                 "device on " + addr.ip() +
                                                                 ":" + addr.port(), e));
                                     }
                                 }
                        );

            } catch (ConfigException e) {
                log.error("Cannot read config error " + e);
            }
        }
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {


        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(NetconfDeviceProvider.this::connectDevices);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            //TODO refactor
            return event.configClass().equals(NetconfProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }
}
