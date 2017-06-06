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
package org.onosproject.ovsdb.providers.device;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbNodeListener;
import org.slf4j.Logger;

/**
 * Provider which uses an ovsdb controller to detect device.
 */
@Component(immediate = true)
@Service
public class OvsdbDeviceProvider extends AbstractProvider
        implements DeviceProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private DeviceProviderService providerService;
    private OvsdbNodeListener innerNodeListener = new InnerOvsdbNodeListener();
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    protected static final String ISNOTNULL = "OvsdbNodeId is not null";
    protected static final String SCHEME_NAME = "ovsdb";
    private static final String UNKNOWN = "unknown";

    protected ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/ovsdbdeviceprovider",
                                                           "device-installer-%d", log));

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addNodeListener(innerNodeListener);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removeNodeListener(innerNodeListener);
        providerRegistry.unregister(this);
        deviceService.removeListener(deviceListener);
        waitForTasksToEnd();
        providerService = null;
        log.info("Stopped");
    }

    public OvsdbDeviceProvider() {
        super(new ProviderId("ovsdb", "org.onosproject.ovsdb.provider.device"));
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        log.info("Triggering probe on device {}", deviceId);
        if (!isReachable(deviceId)) {
            log.error("Failed to probe device {}", deviceId);
            providerService.deviceDisconnected(deviceId);
        } else {
            log.trace("Confirmed device {} connection", deviceId);
        }
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        // TODO: This will be implemented later.
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        OvsdbClientService ovsdbClient = controller.getOvsdbClient(changeDeviceIdToNodeId(deviceId));
        return !(ovsdbClient == null || !ovsdbClient.isConnected());
    }

    private class InnerOvsdbNodeListener implements OvsdbNodeListener {

        @Override
        public void nodeAdded(OvsdbNodeId nodeId) {
            checkNotNull(nodeId, ISNOTNULL);
            DeviceId deviceId = DeviceId.deviceId(nodeId.toString());
            URI uri = URI.create(nodeId.toString());
            ChassisId cid = new ChassisId();
            String ipAddress = nodeId.getIpAddress();
            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set("ipaddress", ipAddress).build();
            DeviceDescription deviceDescription = new DefaultDeviceDescription(
                                                                               uri,
                                                                               Device.Type.CONTROLLER,
                                                                               UNKNOWN, UNKNOWN,
                                                                               UNKNOWN, UNKNOWN,
                                                                               cid,
                                                                               annotations);
            providerService.deviceConnected(deviceId, deviceDescription);

        }

        @Override
        public void nodeRemoved(OvsdbNodeId nodeId) {
            checkNotNull(nodeId, ISNOTNULL);
            DeviceId deviceId = DeviceId.deviceId(nodeId.toString());
            providerService.deviceDisconnected(deviceId);

        }
    }

    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] strings = deviceId.toString().split(":");
        if (strings.length < 1) {
            return null;
        }
        return new OvsdbNodeId(IpAddress.valueOf(strings[1]), 0);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        // TODO if required
    }

    private void discoverPorts(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device.is(DeviceDescriptionDiscovery.class)) {
            DeviceDescriptionDiscovery deviceDescriptionDiscovery = device.as(DeviceDescriptionDiscovery.class);
            providerService.updatePorts(deviceId, deviceDescriptionDiscovery.discoverPortDetails());
        } else {
            log.warn("Device " + deviceId + " does not support behaviour DeviceDescriptionDiscovery");
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            if (!isRelevant(deviceId)) {
                return;
            }
            if ((event.type() == DeviceEvent.Type.DEVICE_ADDED)) {
                executor.execute(() -> discoverPorts(deviceId));
            } else if ((event.type() == DeviceEvent.Type.DEVICE_REMOVED)) {
                log.debug("removing device {}", event.subject().id());
                OvsdbNodeId ovsdbNodeId = changeDeviceIdToNodeId(deviceId);
                OvsdbClientService client = controller.getOvsdbClient(ovsdbNodeId);
                if (client != null) {
                    client.disconnect();
                }
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            return isRelevant(deviceId) && mastershipService.isLocalMaster(deviceId);
        }

        private boolean isRelevant(DeviceId deviceId) {
            return deviceId.uri().getScheme().equals(SCHEME_NAME);
        }
    }

    private void waitForTasksToEnd() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Timeout while waiting for child threads to finish because: " + e.getMessage());
        }
    }
}
