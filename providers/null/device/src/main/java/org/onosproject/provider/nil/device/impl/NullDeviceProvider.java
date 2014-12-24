/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.provider.nil.device.impl;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which advertises fake/nonexistant devices to the core.
 * To be used for benchmarking only.
 */
@Component(immediate = true)
public class NullDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger log = getLogger(NullDeviceProvider.class);
    private static final String SCHEME = "null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    private DeviceProviderService providerService;

    private ExecutorService deviceBuilder = Executors.newFixedThreadPool(1,
                                                     namedThreads("null-device-creator"));



    //currently hardcoded. will be made configurable via rest/cli.
    private static final int NUMDEVICES = 10;
    private static final int NUMPORTSPERDEVICE = 10;

    //Delay between events in ms.
    private static final int EVENTINTERVAL = 5;

    private final Map<Integer, DeviceDescription> descriptions = Maps.newHashMap();

    private DeviceCreator creator;


    /**
     *
     * Creates a provider with the supplier identifier.
     *
     */
    public NullDeviceProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        deviceBuilder.submit(new DeviceCreator(true));
        log.info("Started");

    }

    @Deactivate
    public void deactivate() {
        deviceBuilder.submit(new DeviceCreator(false));
        try {
            deviceBuilder.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Device builder did not terminate");
        }
        deviceBuilder.shutdownNow();
        providerRegistry.unregister(this);
        providerService = null;

        log.info("Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {}

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {}

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return descriptions.values().stream()
                .anyMatch(desc -> desc.deviceURI().equals(deviceId.uri()));
    }


    private class DeviceCreator implements Runnable {

        private boolean setup;

        public DeviceCreator(boolean setup) {
            this.setup = setup;
        }

        @Override
        public void run() {
            if (setup) {
                advertiseDevices();
            } else {
                removeDevices();
            }
        }

        private void removeDevices() {
            for (DeviceDescription desc : descriptions.values()) {
                providerService.deviceDisconnected(
                        DeviceId.deviceId(desc.deviceURI()));
                delay(EVENTINTERVAL);
            }
            descriptions.clear();
        }

        private void advertiseDevices() {
            DeviceId did;
            ChassisId cid;
            for (int i = 0; i < NUMDEVICES; i++) {
                did = DeviceId.deviceId(String.format("%s:%d", SCHEME, i));
                cid = new ChassisId(i);
                DeviceDescription desc =
                        new DefaultDeviceDescription(did.uri(), Device.Type.SWITCH,
                                                     "ON.Lab", "0.0.1", "0.0.1", "1234",
                                                     cid);
                descriptions.put(i, desc);
                providerService.deviceConnected(did, desc);
                providerService.updatePorts(did, buildPorts());
                delay(EVENTINTERVAL);
            }
        }

        private List<PortDescription> buildPorts() {
            List<PortDescription> ports = Lists.newArrayList();
            for (int i = 0; i < NUMPORTSPERDEVICE; i++) {
                ports.add(new DefaultPortDescription(PortNumber.portNumber(i), true,
                                                     Port.Type.COPPER,
                                                     (long) 0));
            }
            return ports;
        }
    }
}
