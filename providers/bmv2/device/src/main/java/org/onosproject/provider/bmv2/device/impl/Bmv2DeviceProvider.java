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
import org.onlab.util.HexString;
import org.onlab.util.Timer;
import org.onosproject.bmv2.api.runtime.Bmv2ControlPlaneServer;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.ctl.Bmv2ThriftClient;
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

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.bmv2.api.runtime.Bmv2Device.*;
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
                DefaultAnnotations.Builder annotationsBuilder = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PROTOCOL, SCHEME);
                dumpJsonConfigToAnnotations(did, annotationsBuilder);
                DeviceDescription descr = new DefaultDeviceDescription(
                        did.uri(), Device.Type.SWITCH, MANUFACTURER, HW_VERSION,
                        UNKNOWN, UNKNOWN, new ChassisId(), annotationsBuilder.build());
                // Reset device state (cleanup entries, etc.)
                resetDeviceState(did);
                providerService.deviceConnected(did, descr);
            }
            updatePorts(did);
            return true;
        });
    }

    private void dumpJsonConfigToAnnotations(DeviceId did, DefaultAnnotations.Builder builder) {
        // TODO: store json config string somewhere else, possibly in a Bmv2Controller (see ONOS-4419)
        try {
            String md5 = Bmv2ThriftClient.of(did).getJsonConfigMd5();
            // Convert to hex string for readability.
            md5 = HexString.toHexString(md5.getBytes());
            String jsonString = Bmv2ThriftClient.of(did).dumpJsonConfig();
            builder.set("bmv2JsonConfigMd5", md5);
            builder.set("bmv2JsonConfigValue", jsonString);
        } catch (Bmv2RuntimeException e) {
            LOG.warn("Unable to dump device JSON config from device {}: {}", did, e.toString());
        }
    }

    private void resetDeviceState(DeviceId did) {
        try {
            Bmv2ThriftClient.of(did).resetState();
        } catch (Bmv2RuntimeException e) {
            LOG.warn("Unable to reset {}: {}", did, e.toString());
        }
    }

    private void updatePorts(DeviceId did) {
        Device device = deviceService.getDevice(did);
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            List<PortDescription> portDescriptions = portConfig.getPorts();
            providerService.updatePorts(did, portDescriptions);
        } else {
            LOG.warn("No PortDiscovery behavior for device {}", did);
        }
    }

    private void disconnectDevice(DeviceId did) {
        LOG.debug("Trying to disconnect device from core... deviceId={}", did);

        // Atomically disconnect device.
        activeDevices.compute(did, (k, v) -> {
            if (deviceService.isAvailable(did)) {
                providerService.deviceDisconnected(did);
                // Make sure to close the transport session with device. Do we really need this?
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
                        // TODO: require also bmv2 internal device id from net-cfg (now is default 0)
                        Bmv2Device bmv2Device = new Bmv2Device(info.ip().toString(), info.port(), 0);
                        triggerProbe(bmv2Device.asDeviceId());
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
            triggerProbe(device.asDeviceId());
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
