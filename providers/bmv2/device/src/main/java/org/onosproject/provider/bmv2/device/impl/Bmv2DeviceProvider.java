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

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
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
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.bmv2.ctl.Bmv2ThriftClient.ping;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * BMv2 device provider.
 */
@Component(immediate = true)
public class Bmv2DeviceProvider extends AbstractDeviceProvider {

    private final Logger log = getLogger(Bmv2DeviceProvider.class);

    public static final String MANUFACTURER = "p4.org";
    public static final String HW_VERSION = "bmv2";
    private static final String APP_NAME = "org.onosproject.bmv2";
    private static final String UNKNOWN = "unknown";
    public static final String SCHEME = "bmv2";

    private final ExecutorService deviceDiscoveryExecutor = Executors
            .newFixedThreadPool(5, groupedThreads("onos/bmv2", "device-discovery", log));

    private final NetworkConfigListener cfgListener = new InternalNetworkConfigListener();

    private final ConfigFactory cfgFactory =
            new ConfigFactory<ApplicationId, Bmv2ProviderConfig>(
                    APP_SUBJECT_FACTORY, Bmv2ProviderConfig.class,
                    "devices", true) {
                @Override
                public Bmv2ProviderConfig createConfig() {
                    return new Bmv2ProviderConfig();
                }
            };

    private final Set<DeviceId> activeDevices = Sets.newConcurrentHashSet();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    /**
     * Creates a Bmv2 device provider with the supplied identifier.
     */
    public Bmv2DeviceProvider() {
        super(new ProviderId("bmv2", "org.onosproject.provider.device"));
    }

    protected static DeviceId deviceIdOf(Bmv2ProviderConfig.Bmv2DeviceInfo info) {
        try {
            return DeviceId.deviceId(new URI(
                    SCHEME, info.ip().toString() + ":" + info.port(), null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Unable to build deviceID for device "
                            + info.ip().toString() + ":" + info.ip().toString(),
                    e);
        }
    }

    @Override
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        netCfgService.registerConfigFactory(cfgFactory);
        netCfgService.addListener(cfgListener);

        super.activate();
    }

    @Override
    protected void deactivate() {
        try {
            activeDevices.stream().forEach(did -> {
                deviceDiscoveryExecutor.execute(() -> disconnectDevice(did));
            });
            deviceDiscoveryExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Device discovery threads did not terminate");
        }
        deviceDiscoveryExecutor.shutdownNow();
        netCfgService.unregisterConfigFactory(cfgFactory);
        netCfgService.removeListener(cfgListener);

        super.deactivate();
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        deviceDiscoveryExecutor.execute(() -> executeProbe(deviceId));
    }

    private void executeProbe(DeviceId did) {
        boolean reachable = isReachable(did);
        log.debug("Probed device: id={}, reachable={}",
                  did.toString(),
                  reachable);
        if (reachable) {
            connectDevice(did);
        } else {
            disconnectDevice(did);
        }
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.debug("roleChanged() is not yet implemented");
        // TODO: implement mastership handling
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return ping(deviceId);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        log.debug("changePortState() is not yet implemented");
        // TODO: implement port handling
    }

    private void connectDevice(DeviceId did) {
        log.debug("Trying to create device on ONOS core: {}", did);
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PROTOCOL, SCHEME)
                .build();
        DeviceDescription descr = new DefaultDeviceDescription(
                did.uri(), Device.Type.SWITCH, MANUFACTURER, HW_VERSION,
                UNKNOWN, UNKNOWN, new ChassisId(), annotations);
        providerService.deviceConnected(did, descr);
        activeDevices.add(did);
        discoverPorts(did);
    }

    private void discoverPorts(DeviceId did) {
        Device device = deviceService.getDevice(did);
        if (device.is(PortDiscovery.class)) {
            PortDiscovery portConfig = device.as(PortDiscovery.class);
            providerService.updatePorts(did, portConfig.getPorts());
        } else {
            log.warn("No PortDiscovery behavior for device {}", did);
        }
    }

    private void disconnectDevice(DeviceId did) {
        log.debug("Trying to remove device from ONOS core: {}", did);
        providerService.deviceDisconnected(did);
        activeDevices.remove(did);
    }

    /**
     * Handles net-cfg events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            Bmv2ProviderConfig cfg = netCfgService.getConfig(appId, Bmv2ProviderConfig.class);
            if (cfg != null) {
                try {
                    cfg.getDevicesInfo().stream().forEach(info -> {
                        triggerProbe(deviceIdOf(info));
                    });
                } catch (ConfigException e) {
                    log.error("Unable to read config: " + e);
                }
            } else {
                log.error("Unable to read config (was null)");
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(Bmv2ProviderConfig.class) &&
                    (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED);
        }
    }
}
