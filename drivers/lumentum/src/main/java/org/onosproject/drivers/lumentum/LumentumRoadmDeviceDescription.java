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

package org.onosproject.drivers.lumentum;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Device description behaviour for Lumentum Snmp devices.
 */
public class LumentumRoadmDeviceDescription extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    private static final String CTRL_PORT_STATE = ".1.3.6.1.4.1.46184.1.4.1.1.3.";

    private LumentumSnmpDevice snmp;

    @Override
    public DeviceDescription discoverDeviceDetails() {
        //TODO get device description
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);
        return new DefaultDeviceDescription(device.id().uri(), Device.Type.ROADM,
                                            "Lumentum", "SDN ROADM", "1.0", "v1",
                                            device.chassisId(), (SparseAnnotations) device.annotations());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        return ImmutableList.copyOf(this.getPorts());
    }

    private List<PortDescription> getPorts() {
        try {
            snmp = new LumentumSnmpDevice(handler().data().deviceId());
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);

            return Collections.emptyList();
        }

        List<PortDescription> ports = Lists.newLinkedList();

        OID[] oids = {
                new OID(CTRL_PORT_STATE + "1"),
                new OID(CTRL_PORT_STATE + "2")
        };

        for (OID oid : oids) {

            for (TreeEvent event : snmp.get(oid)) {
                if (event != null) {
                    VariableBinding[] varBindings = event.getVariableBindings();
                    for (VariableBinding varBinding : varBindings) {
                        if (varBinding.getVariable().toInt() == 1) {
                            int portNumber = varBinding.getOid().removeLast();
                            int portDirection = varBinding.getOid().removeLast();
                            SparseAnnotations ann = DefaultAnnotations.builder()
                                    .set(AnnotationKeys.PORT_NAME, portDirection + "-" + portNumber)
                                    .build();
                            PortDescription p = omsPortDescription(
                                    PortNumber.portNumber(ports.size() + 1),
                                    true,
                                    LumentumSnmpDevice.START_CENTER_FREQ,
                                    LumentumSnmpDevice.END_CENTER_FREQ,
                                    LumentumSnmpDevice.CHANNEL_SPACING.frequency(),
                                    ann);
                            ports.add(p);
                        }
                    }
                }
            }
        }

        // Create LINE IN and LINE OUT ports as these are not reported through SNMP
        SparseAnnotations annLineIn = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, "LINE IN")
                .build();
        ports.add(omsPortDescription(
                PortNumber.portNumber(ports.size() + 1),
                true,
                LumentumSnmpDevice.START_CENTER_FREQ,
                LumentumSnmpDevice.END_CENTER_FREQ,
                LumentumSnmpDevice.CHANNEL_SPACING.frequency(),
                annLineIn
        ));

        SparseAnnotations annLineOut = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, "LINE OUT")
                .build();
        ports.add(omsPortDescription(
                PortNumber.portNumber(ports.size() + 1),
                true,
                LumentumSnmpDevice.START_CENTER_FREQ,
                LumentumSnmpDevice.END_CENTER_FREQ,
                LumentumSnmpDevice.CHANNEL_SPACING.frequency(),
                annLineOut
        ));

        return ports;
    }
}
