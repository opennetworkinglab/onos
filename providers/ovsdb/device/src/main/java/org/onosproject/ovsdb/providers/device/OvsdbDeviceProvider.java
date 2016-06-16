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
package org.onosproject.ovsdb.providers.device;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
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
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    private DeviceProviderService providerService;
    private OvsdbNodeListener innerNodeListener = new InnerOvsdbNodeListener();
    protected static final String ISNOTNULL = "OvsdbNodeId is not null";
    private static final String UNKNOWN = "unknown";

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addNodeListener(innerNodeListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
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
            return;
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

    @Override
    public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
    }

    @Override
    public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        //TODO
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
}
