/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.config.impl;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.config.basics.SubjectFactories.DEVICE_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ChassisId;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.inject.DeviceInjectionConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

// TODO In theory just @Component should be sufficient,
//      but won't work without @Service. Need investigation.
/**
 * Component to monitor DeviceInjectionConfig changes.
 */
@Beta
@Service(value = DeviceInjectionConfigMonitor.class)
@Component(immediate = true)
public class DeviceInjectionConfigMonitor {

    private final Logger log = getLogger(getClass());

    private final ProviderId pid = new ProviderId("inject", "org.onosproject.inject");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netcfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netcfgRegistry;

    private final List<ConfigFactory<?, ?>> factories = ImmutableList.of(
         new ConfigFactory<DeviceId, DeviceInjectionConfig>(DEVICE_SUBJECT_FACTORY,
                 DeviceInjectionConfig.class,
                 DeviceInjectionConfig.CONFIG_KEY) {
             @Override
             public DeviceInjectionConfig createConfig() {
                 return new DeviceInjectionConfig();
             }
         });


    private final InternalConfigListener listener = new InternalConfigListener();

    private ExecutorService worker;

    private InternalDeviceProvider deviceProvider = new InternalDeviceProvider();

    private DeviceProviderService providerService;


    @Activate
    public void activate() {
        worker = newSingleThreadExecutor(groupedThreads("onos/inject",
                                                        "worker",
                                                        log));
        providerService = deviceProviderRegistry.register(deviceProvider);

        netcfgService.addListener(listener);

        factories.forEach(netcfgRegistry::registerConfigFactory);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        netcfgService.removeListener(listener);

        deviceProviderRegistry.unregister(deviceProvider);

        worker.shutdown();
        try {
            worker.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted.", e);
            Thread.currentThread().interrupt();
        }
        factories.forEach(netcfgRegistry::unregisterConfigFactory);

        log.info("Stopped");
    }

    private void removeDevice(DeviceId did) {
        providerService.deviceDisconnected(did);
    }

    private void injectDevice(DeviceId did) {
        Optional<BasicDeviceConfig> basic =
                Optional.ofNullable(netcfgService.getConfig(did, BasicDeviceConfig.class));
        Optional<DeviceDescriptionDiscovery> discovery = basic
            .map(BasicDeviceConfig::driver)
            .map(driverService::getDriver)
            .filter(drvr -> drvr.hasBehaviour(DeviceDescriptionDiscovery.class))
            .map(drvr -> drvr.createBehaviour(new DefaultDriverHandler(new DefaultDriverData(drvr, did)),
                                              DeviceDescriptionDiscovery.class));

        if (discovery.isPresent()) {
            providerService.deviceConnected(did,
                                            discovery.get().discoverDeviceDetails());
            providerService.updatePorts(did,
                                        discovery.get().discoverPortDetails());
        } else {

            String unk = "UNKNOWN";
            DefaultDeviceDescription desc = new DefaultDeviceDescription(
                     did.uri(),
                     basic.map(BasicDeviceConfig::type).orElse(Type.SWITCH),
                     basic.map(BasicDeviceConfig::manufacturer).orElse(unk),
                     basic.map(BasicDeviceConfig::hwVersion).orElse(unk),
                     basic.map(BasicDeviceConfig::swVersion).orElse(unk),
                     basic.map(BasicDeviceConfig::serial).orElse(unk),
                     new ChassisId(),
                     true);
            providerService.deviceConnected(did, desc);

            Optional<DeviceInjectionConfig> inject =
                Optional.ofNullable(netcfgService.getConfig(did, DeviceInjectionConfig.class));

            String ports = inject.map(DeviceInjectionConfig::ports).orElse("0");
            int numPorts = Integer.parseInt(ports);
            List<PortDescription> portDescs = new ArrayList<>(numPorts);
            for (int i = 1; i <= numPorts; ++i) {
                // TODO inject port details if something like BasicPortConfig was created
                PortNumber number = portNumber(i);
                boolean isEnabled = true;
                portDescs.add(new DefaultPortDescription(number, isEnabled));
            }
            providerService.updatePorts(did, portDescs);
        }
    }

    final class InternalDeviceProvider implements DeviceProvider {
        @Override
        public ProviderId id() {
            return pid;
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
            worker.execute(() -> injectDevice(deviceId));
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
            providerService.receivedRoleReply(deviceId, newRole, newRole);
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            return true;
        }

        @Override
        public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                    boolean enable) {
            // TODO handle request to change port state from controller
        }
    }

    /**
     * Listens for Config updates.
     */
    final class InternalConfigListener
            implements NetworkConfigListener {

        /**
         * Relevant {@link NetworkConfigEvent} type.
         */
        private final Set<NetworkConfigEvent.Type> relevant
            = ImmutableSet.copyOf(EnumSet.of(
                         NetworkConfigEvent.Type.CONFIG_ADDED,
                         NetworkConfigEvent.Type.CONFIG_UPDATED,
                         NetworkConfigEvent.Type.CONFIG_REMOVED));

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass() == DeviceInjectionConfig.class &&
                    relevant.contains(event.type());
        }

        @Override
        public void event(NetworkConfigEvent event) {
            worker.execute(() -> processEvent(event));
        }

        /**
         * Process Config add/update/remove event.
         * <p>
         * Note: will be executed in the worker thread.
         *
         * @param event add/update/remove event
         */
        protected void processEvent(NetworkConfigEvent event) {

            DeviceId did = (DeviceId) event.subject();
            if (!did.uri().getScheme().equals(pid.scheme())) {
                log.warn("Attempt to inject unexpected scheme {}", did);
                return;
            }
            log.debug("{} to {}: {}", event.type(), did, event);

            // TODO if there's exist one probably better to follow master
//            if (deviceService.getRole(did) != MastershipRole.MASTER) {
//                log.debug("Not the master, ignoring. {}", event);
//                return;
//            }

            switch (event.type()) {
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
                injectDevice(did);
                break;
            case CONFIG_REMOVED:
                removeDevice(did);
                break;

            case CONFIG_REGISTERED:
            case CONFIG_UNREGISTERED:
            default:
                log.warn("Ignoring unexpected event: {}", event);
                break;
            }
        }
    }

}
