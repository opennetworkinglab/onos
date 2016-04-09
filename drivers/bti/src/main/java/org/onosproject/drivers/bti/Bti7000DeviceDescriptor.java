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

package org.onosproject.drivers.bti;

import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.I_Device;
import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0._OidRegistry;
import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.mib_2.System;
import com.btisystems.pronx.ems.core.model.ClassRegistry;
import com.btisystems.pronx.ems.core.model.IClassRegistry;
import com.btisystems.pronx.ems.core.model.NetworkDevice;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.V2cSnmpConfiguration;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Bti 7000 SNMP device description behaviour. Provides device description and port information.
 */
public class Bti7000DeviceDescriptor extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());
    protected static final IClassRegistry CLASS_REGISTRY =
            new ClassRegistry(_OidRegistry.oidRegistry, I_Device.class);
    private static final String UNKNOWN = "unknown";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        SnmpController controller = checkNotNull(handler().get(SnmpController.class));
        DeviceId deviceId = handler().data().deviceId();
        SnmpDevice snmpDevice = controller.getDevice(deviceId);
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(deviceId);
        DeviceDescription desc = null;
        String ipAddress = snmpDevice.getSnmpHost();
        int port = snmpDevice.getSnmpPort();

        ISnmpConfiguration config = new V2cSnmpConfiguration();
        config.setPort(port);

        try (ISnmpSession session = controller.getSession(deviceId)) {
            // Each session will be auto-closed.
            String deviceOid = session.identifyDevice();
            desc = populateDescription(session, device);

        } catch (IOException | RuntimeException ex) {
            log.error("Failed to walk device.", ex.getMessage());
            log.debug("Detailed problem was ", ex);
        }
        return desc;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        //TODO implement
        return ImmutableList.of();
    }

    private DeviceDescription populateDescription(ISnmpSession session, Device device) {
        NetworkDevice networkDevice = new NetworkDevice(CLASS_REGISTRY,
                                                        session.getAddress().getHostAddress());
        try {
            session.walkDevice(networkDevice, Collections.singletonList(CLASS_REGISTRY.getClassToOidMap().get(
                    System.class)));

            com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.mib_2.System systemTree =
                    (com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.mib_2.System)
                            networkDevice.getRootObject().getEntity(CLASS_REGISTRY.getClassToOidMap().get(
                                    com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.mib_2.System.class));
            if (systemTree != null) {
                String[] systemComponents = systemTree.getSysDescr().split(";");
                return new DefaultDeviceDescription(device.id().uri(), device.type(),
                                                    systemComponents[0], systemComponents[2],
                                                    systemComponents[3], UNKNOWN, device.chassisId(),
                                                    (SparseAnnotations) device.annotations());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Error reading details for device." + session.getAddress(), ex);
        }
        return null;
    }
}
