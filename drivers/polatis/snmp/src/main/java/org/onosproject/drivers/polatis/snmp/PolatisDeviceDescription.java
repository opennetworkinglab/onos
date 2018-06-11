/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.polatis.snmp;

import com.google.common.collect.Lists;
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

import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.get;
import static org.onosproject.drivers.polatis.snmp.PolatisSnmpUtility.getTable;

/**
 * Representation of device information and ports via SNMP for all Polatis
 * optical circuit switches.
 */
public class PolatisDeviceDescription extends AbstractHandlerBehaviour
    implements DeviceDescriptionDiscovery {

    private static final String DEFAULT_MANUFACTURER = "Polatis";
    private static final String DEFAULT_DESCRIPTION_DATA = "Unknown";

    private static final String SOFTWARE_VERSION_OID = ".1.3.6.1.4.1.26592.2.1.2.2.3.0";
    private static final String PRODUCT_CODE_OID = ".1.3.6.1.4.1.26592.2.1.2.2.1.0";
    private static final String SERIAL_NUMBER_OID = ".1.3.6.1.4.1.26592.2.1.2.2.2.0";

    private static final String PORT_ENTRY_OID = ".1.3.6.1.4.1.26592.2.2.2.1.2";
    private static final String PORT_PATCH_OID = PORT_ENTRY_OID + ".1.2";
    private static final String PORT_CURRENT_STATE_OID = PORT_ENTRY_OID + ".1.3";

    public static final int POLATIS_NUM_OF_WAVELENGTHS = 39;

    private final Logger log = getLogger(getClass());

    /**
     * Discovers device details, for Polatis Snmp device by getting the system
     * information.
     *
     * @return device description
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            return new DefaultDeviceDescription(deviceId.uri(), Device.Type.FIBER_SWITCH,
                    DEFAULT_MANUFACTURER, DEFAULT_DESCRIPTION_DATA,
                    DEFAULT_DESCRIPTION_DATA, DEFAULT_DESCRIPTION_DATA,
                    new ChassisId());
        }
        String hardwareVersion = DEFAULT_DESCRIPTION_DATA;
        try {
            hardwareVersion = hardwareVersion();
        } catch (IOException e) {
            log.error("Error reading hardware version for device {} exception ", deviceId, e);
        }

        String softwareVersion = DEFAULT_DESCRIPTION_DATA;
        try {
            softwareVersion = softwareVersion();
        } catch (IOException e) {
            log.error("Error reading software version for device {} exception {}", deviceId, e);
        }

        String serialNumber = DEFAULT_DESCRIPTION_DATA;
        try {
            serialNumber = serialNumber();
        } catch (IOException e) {
            log.error("Error reading serial number for device {} exception {}", deviceId, e);
        }

        return new DefaultDeviceDescription(deviceId.uri(), Device.Type.FIBER_SWITCH,
                                            DEFAULT_MANUFACTURER, hardwareVersion,
                                            softwareVersion, serialNumber,
                                            device.chassisId(), (SparseAnnotations) device.annotations());
    }

    /**
     * Discovers port details, for Polatis Snmp device.
     *
     * @return port list
     */
    @Override
    public List<PortDescription> discoverPortDetails() {
        List<PortDescription> ports = Lists.newArrayList();
        List<TableEvent> events;
        DeviceId deviceId = handler().data().deviceId();

        try {
            OID[] columnOIDs = {new OID(PORT_CURRENT_STATE_OID)};
            events = getTable(handler(), columnOIDs);
        } catch (IOException e) {
            log.error("Error reading ports table for device {} exception {}", deviceId, e);
            return ports;
        }

        if (events == null) {
            log.error("Error reading ports table for device {}", deviceId);
            return ports;
        }

        for (TableEvent event : events) {
            if (event == null) {
                log.error("Error reading event for device {}", deviceId);
                continue;
            }
            VariableBinding[] columns = event.getColumns();
            if (columns == null) {
                log.error("Error reading columns for device {} event {}", deviceId, event);
                continue;
            }

            VariableBinding portColumn = columns[0];
            if (portColumn == null) {
                continue;
            }

            int port = event.getIndex().last();
            boolean enabled = (portColumn.getVariable().toInt() == 1);
            PortNumber portNumber = PortNumber.portNumber(port);
            DefaultAnnotations annotations = DefaultAnnotations.builder().build();
            double opticalBand = Spectrum.O_BAND_MIN.asGHz() - Spectrum.L_BAND_MAX.asGHz();
            Frequency opticalGrid = Frequency.ofGHz(opticalBand / POLATIS_NUM_OF_WAVELENGTHS);
            PortDescription p = omsPortDescription(portNumber,
                    enabled,
                    Spectrum.O_BAND_MIN,
                    Spectrum.L_BAND_MAX,
                    opticalGrid,
                    annotations);
            ports.add(p);
        }

        return ports;
    }

    private String hardwareVersion() throws IOException {
        return get(handler(), PRODUCT_CODE_OID).toString();
    }

    private String softwareVersion() throws IOException {
        return get(handler(), SOFTWARE_VERSION_OID).toString();
    }

    private String serialNumber() throws IOException {
        return get(handler(), SERIAL_NUMBER_OID).toString();
    }
}
