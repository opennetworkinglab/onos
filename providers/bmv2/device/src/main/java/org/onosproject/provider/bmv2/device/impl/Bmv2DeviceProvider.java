/*
 * Copyright 2014-2016 Open Networking Laboratory
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

package org.onosproject.provider.bmv2.device.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.ChassisId;
import org.onlab.util.Timer;
import org.onosproject.bmv2.api.runtime.Bmv2ControlPlaneServer;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.common.net.AbstractDeviceProvider;
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
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.bmv2.ctl.Bmv2ThriftClient.forceDisconnectOf;
import static org.onosproject.bmv2.ctl.Bmv2ThriftClient.ping;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * BMv2 device provider.
 */
@Component(immediate = true)
public class Bmv2DeviceProvider extends AbstractDeviceProvider {

    private static final Logger LOG = getLogger(Bmv2DeviceProvider.class);

    public static final String MANUFACTURER = "p4.org";
    public static final String HW_VERSION = "bmv2";
    public static final String SCHEME = "bmv2";
    private static final String APP_NAME = "org.onosproject.bmv2";
    private static final String UNKNOWN = "unknown";
    private static final int POLL_INTERVAL = 5; // seconds

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2ControlPlaneServer controlPlaneServer;

    private final ExecutorService deviceDiscoveryExecutor = Executors
            .newFixedThreadPool(5, groupedThreads("onos/bmv2", "device-discovery", LOG));

    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();
    private final ConfigFactory cfgFactory = new InternalConfigFactory();
    private final ConcurrentMap<DeviceId, Boolean> activeDevices = Maps.newConcurrentMap();
    private final DevicePoller devicePoller = new DevicePoller();
    private final InternalHelloListener helloListener = new InternalHelloListener();
    private ApplicationId appId;

    /**
     * Creates a Bmv2 device provider with the supplied identifier.
     */
    public Bmv2DeviceProvider() {
        super(new ProviderId("bmv2", "org.onosproject.provider.device"));
    }

    private static DeviceId deviceIdOf(String ip, int port) {
        try {
            return DeviceId.deviceId(new URI(SCHEME, ip + ":" + port, null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to build deviceID for device " + ip + ":" + port, e);
        }
    }

    /**
     * Creates a new device ID for the given BMv2 device.
     *
     * @param device a BMv2 device object
     *
     * @return a new device ID
     */
    public static DeviceId deviceIdOf(Bmv2Device device) {
        return deviceIdOf(device.thriftServerHost(), device.thriftServerPort());
    }

    @Override
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        netCfgService.registerConfigFactory(cfgFactory);
        netCfgService.addListener(cfgListener);
        controlPlaneServer.addHelloListener(helloListener);
        devicePoller.start();
        super.activate();
    }

    @Override
    protected void deactivate() {
        devicePoller.stop();
        controlPlaneServer.removeHelloListener(helloListener);
        try {
            activeDevices.forEach((did, value) -> {
                deviceDiscoveryExecutor.execute(() -> disconnectDevice(did));
            });
            deviceDiscoveryExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error("Device discovery threads did not terminate");
        }
        deviceDiscoveryExecutor.shutdownNow();
        netCfgService.unregisterConfigFactory(cfgFactory);
        netCfgService.removeListener(cfgListener);
        super.deactivate();
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // Asynchronously trigger probe task.
        deviceDiscoveryExecutor.execute(() -> executeProbe(deviceId));
    }

    private void executeProbe(DeviceId did) {
        boolean reachable = isReachable(did);
        LOG.debug("Probed device: id={}, reachable={}",
                  did.toString(),
                  reachable);
        if (reachable) {
            discoverDevice(did);
        } else {
            disconnectDevice(did);
        }
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        LOG.debug("roleChanged() is not yet implemented");
        // TODO: implement mastership handling
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return ping(deviceId);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        LOG.debug("changePortState() is not yet implemented");
        // TODO: implement port handling
    }

    private void discoverDevice(DeviceId did) {
        LOG.debug("Starting device discovery... deviceId={}", did);

        // Atomically notify device to core and update port information.
        activeDevices.compute(did, (k, v) -> {
            if (!deviceService.isAvailable(did)) {
                // Device not available in the core, connect it now.
                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PROTOCOL, SCHEME)
                        .build();
                DeviceDescription descr = new DefaultDeviceDescription(
                        did.uri(), Device.Type.SWITCH, MANUFACTURER, HW_VERSION,
                        UNKNOWN, UNKNOWN, new ChassisId(), annotations);
                providerService.deviceConnected(did, descr);
            }
            // Discover ports.
            Device device = deviceService.getDevice(did);
            if (device.is(PortDiscovery.class)) {
                PortDiscovery portConfig = device.as(PortDiscovery.class);
                List<PortDescription> portDescriptions = portConfig.getPorts();
                providerService.updatePorts(did, portDescriptions);
            } else {
                LOG.warn("No PortDiscovery behavior for device {}", did);
            }
            return true;
        });
    }

    private void disconnectDevice(DeviceId did) {
        LOG.debug("Trying to disconnect device from core... deviceId={}", did);

        // Atomically disconnect device.
        activeDevices.compute(did, (k, v) -> {
            if (deviceService.isAvailable(did)) {
                providerService.deviceDisconnected(did);
                // Make sure to close the transport session with device.
                forceDisconnectOf(did);
            }
            return null;
        });
    }

    /**
     * Internal net-cfg config factory.
     */
    private class InternalConfigFactory extends ConfigFactory<ApplicationId, Bmv2ProviderConfig> {

        InternalConfigFactory() {
            super(APP_SUBJECT_FACTORY, Bmv2ProviderConfig.class, "devices", true);
        }

        @Override
        public Bmv2ProviderConfig createConfig() {
            return new Bmv2ProviderConfig();
        }
    }

    /**
     * Internal net-cfg event listener.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            Bmv2ProviderConfig cfg = netCfgService.getConfig(appId, Bmv2ProviderConfig.class);
            if (cfg != null) {
                try {
                    cfg.getDevicesInfo().stream().forEach(info -> {
                        triggerProbe(deviceIdOf(info.ip().toString(), info.port()));
                    });
                } catch (ConfigException e) {
                    LOG.error("Unable to read config: " + e);
                }
            } else {
                LOG.error("Unable to read config (was null)");
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(Bmv2ProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }

    /**
     * Listener triggered by Bmv2ControlPlaneServer each time a hello message is received.
     */
    private class InternalHelloListener implements Bmv2ControlPlaneServer.HelloListener {
        @Override
        public void handleHello(Bmv2Device device) {
            log.debug("Received hello from {}", device);
            triggerProbe(deviceIdOf(device));
        }
    }

    /**
     * Task that periodically trigger device probes.
     */
    private class DevicePoller implements TimerTask {

        private final HashedWheelTimer timer = Timer.getTimer();
        private Timeout timeout;

        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return;
            }
            log.debug("Executing polling on {} devices...", activeDevices.size());
            activeDevices.forEach((did, value) -> triggerProbe(did));
            timeout.getTimer().newTimeout(this, POLL_INTERVAL, TimeUnit.SECONDS);
        }

        /**
         * Starts the collector.
         */
         synchronized void start() {
            LOG.info("Starting device poller...");
            timeout = timer.newTimeout(this, 1, TimeUnit.SECONDS);
        }

        /**
         * Stops the collector.
         */
        synchronized void stop() {
            LOG.info("Stopping device poller...");
            timeout.cancel();
        }
    }
}
