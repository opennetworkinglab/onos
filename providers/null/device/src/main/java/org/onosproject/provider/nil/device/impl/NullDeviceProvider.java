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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which advertises fake/nonexistant devices to the core.
 * nodeID is passed as part of the fake device id so that multiple nodes can run simultaneously.
 * To be used for benchmarking only.
 */
@Component(immediate = true)
public class NullDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger log = getLogger(NullDeviceProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private DeviceProviderService providerService;

    private ExecutorService deviceBuilder =
            Executors.newFixedThreadPool(1, groupedThreads("onos/null", "device-creator"));

    private static final String SCHEME = "null";
    private static final int DEF_NUMDEVICES = 10;
    private static final int DEF_NUMPORTS = 10;

    //Delay between events in ms.
    private static final int EVENTINTERVAL = 5;

    private final Map<Integer, DeviceDescription> descriptions = Maps.newHashMap();

    @Property(name = "devConfigs", value = "", label = "Instance-specific configurations")
    private String devConfigs = null;

    private int numDevices = DEF_NUMDEVICES;

    @Property(name = "numPorts", intValue = 10, label = "Number of ports per devices")
    private int numPorts = DEF_NUMPORTS;

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
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        if (!modified(context)) {
            deviceBuilder.submit(new DeviceCreator(true));
        }
        log.info("Started");

    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
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

    @Modified
    public boolean modified(ComponentContext context) {
        if (context == null) {
            log.info("No configuration file, using defaults: numDevices={}, numPorts={}",
                    numDevices, numPorts);
            return false;
        }

        Dictionary<?, ?> properties = context.getProperties();

        int newDevNum = DEF_NUMDEVICES;
        int newPortNum = DEF_NUMPORTS;
        try {
            String s = get(properties, "devConfigs");
            if (!isNullOrEmpty(s)) {
                newDevNum = getDevicesConfig(s);
            }
            s = get(properties, "numPorts");
            newPortNum = isNullOrEmpty(s) ? DEF_NUMPORTS : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            log.warn(e.getMessage());
            newDevNum = numDevices;
            newPortNum = numPorts;
        }

        boolean chgd = false;
        if (newDevNum != numDevices) {
            numDevices = newDevNum;
            chgd |= true;
        }
        if (newPortNum != numPorts) {
            numPorts = newPortNum;
            chgd |= true;
        }
        log.info("Using settings numDevices={}, numPorts={}", numDevices, numPorts);
        if (chgd) {
            deviceBuilder.submit(new DeviceCreator(true));
        }
        return chgd;
    }

    private int getDevicesConfig(String config) {
        for (String sub : config.split(",")) {
            String[] params = sub.split(":");
            if (params.length == 2) {
                NodeId that = new NodeId(params[0].trim());
                String nd = params[1];
                if (clusterService.getLocalNode().id().equals(that)) {
                    return Integer.parseInt(nd.trim());
                }
                continue;
            }
        }
        return DEF_NUMDEVICES;
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
                try {
                    advertiseDevices();
                } catch (URISyntaxException e) {
                    log.warn("URI creation failed during device adverts {}", e.getMessage());
                }
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

        private void advertiseDevices() throws URISyntaxException {
            DeviceId did;
            ChassisId cid;

            // nodeIdHash takes into account for nodeID to avoid collisions when running multi-node providers.
            long nodeIdHash = clusterService.getLocalNode().id().hashCode() << 16;

            for (int i = 0; i < numDevices; i++) {
                long id = nodeIdHash | i;

                did = DeviceId.deviceId(new URI(SCHEME, toHex(id), null));
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
            for (int i = 0; i < numPorts; i++) {
                ports.add(new DefaultPortDescription(PortNumber.portNumber(i), true,
                                                     Port.Type.COPPER,
                                                     0));
            }
            return ports;
        }
    }
}
