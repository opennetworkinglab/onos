/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.drivers.ciena.waveserver.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the ports from a Ciena WaveServer Rest device.
 */
//TODO: Use CienaRestDevice
public class CienaWaveserverDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    private static final String PORT_ID = "ptp-index";
    private static final String XML = "xml";
    private static final String ENABLED = "enabled";
    private static final String ADMIN_STATE = "state.admin-state";
    private static final String PORTS = "ws-ptps.ptps";
    private static final String PORT_IN = "properties.line-system.cmd.port-in";
    private static final String PORT_OUT = "properties.line-system.cmd.port-out";

    private static final String CHANNEL_ID =
            "properties.transmitter.line-system-channel-number";

    private static final String PORT_REQUEST =
            "ciena-ws-ptp:ws-ptps?config=true&format=xml&depth=unbounded";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("getting device description");
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);

        if (device == null) {
            return new DefaultDeviceDescription(deviceId.uri(),
                                                Device.Type.OTN,
                                                "Ciena",
                                                "WaveServer",
                                                "Unknown",
                                                "Unknown",
                                                new ChassisId());
        } else {
            return new DefaultDeviceDescription(device.id().uri(),
                                                Device.Type.OTN,
                                                device.manufacturer(),
                                                device.hwVersion(),
                                                device.swVersion(),
                                                device.serialNumber(),
                                                device.chassisId());
        }
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        return getPorts();
    }

    private List<PortDescription> getPorts() {
        /*
         * Relationship between ptp-index and port number shown in Ciena Wave Server
         * CLI:
         *      ptp-index = 4 * port_number (without decimal) + decimal
         *      e.g
         *          if port_number is 5 then ptp-index = 5 * 4 + 0 = 20
         *          if port_number is 5.1 then ptp-index = 5 * 4 + 1 = 21
         *
         * Relationship between channelId and in/out port:
         *      in_port = channelId * 2
         *      out_port = channelId * 2 -1
         */
        List<PortDescription> ports = Lists.newArrayList();
        RestSBController controller = checkNotNull(handler().get(RestSBController.class));
        DeviceId deviceId = handler().data().deviceId();

        HierarchicalConfiguration config = XmlConfigParser.
                loadXml(controller.get(deviceId, PORT_REQUEST, MediaType.APPLICATION_XML_TYPE));
        List<HierarchicalConfiguration> portsConfig =
                parseWaveServerCienaPorts(config);
        portsConfig.forEach(sub -> {
            String portId = sub.getString(PORT_ID);
            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
            if (CienaRestDevice.getLinesidePortId().contains(portId)) {
                annotations.set(AnnotationKeys.CHANNEL_ID, sub.getString(CHANNEL_ID));
                // TX/OUT and RX/IN ports
                annotations.set(AnnotationKeys.PORT_OUT, sub.getString(PORT_OUT));
                annotations.set(AnnotationKeys.PORT_IN, sub.getString(PORT_IN));
                ports.add(parseWaveServerCienaOchPorts(
                        Long.valueOf(portId),
                        sub,
                        annotations.build()));

            } else if (!portId.equals("5") && !portId.equals("49")) {
                DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, portId);
                //FIXME change when all optical types have two way information methods, see jira tickets
                ports.add(oduCltPortDescription(PortNumber.portNumber(sub.getLong(PORT_ID)),
                                                sub.getString(ADMIN_STATE).equals(ENABLED),
                                                CltSignalType.CLT_100GBE, annotations.build()));
            }
        });
        return ImmutableList.copyOf(ports);
    }

    public static List<HierarchicalConfiguration> parseWaveServerCienaPorts(HierarchicalConfiguration cfg) {
        return cfg.configurationsAt(PORTS);
    }

    public static PortDescription parseWaveServerCienaOchPorts(long portNumber,
                                                               HierarchicalConfiguration config,
                                                               SparseAnnotations annotations) {
        final List<String> tunableType = Lists.newArrayList("performance-optimized", "accelerated");
        final String flexGrid = "flex-grid";
        final String state = "properties.transmitter.state";
        final String tunable = "properties.modem.tx-tuning-mode";
        final String spacing = "properties.line-system.wavelength-spacing";
        final String frequency = "properties.transmitter.frequency.value";

        boolean isEnabled = config.getString(state).equals(ENABLED);
        boolean isTunable = tunableType.contains(config.getString(tunable));

        GridType gridType = config.getString(spacing).equals(flexGrid) ? GridType.FLEX : null;
        ChannelSpacing chSpacing = gridType == GridType.FLEX ? ChannelSpacing.CHL_6P25GHZ : null;

        //Working in Ghz //(Nominal central frequency - 193.1)/channelSpacing = spacingMultiplier
        final int baseFrequency = 193100;
        long spacingFrequency = chSpacing == null ? baseFrequency : chSpacing.frequency().asHz();
        int spacingMult = ((int) (toGbps(((int) config.getDouble(frequency) -
                baseFrequency)) / toGbpsFromHz(spacingFrequency))); //FIXME is there a better way ?

        return ochPortDescription(PortNumber.portNumber(portNumber), isEnabled, OduSignalType.ODU4, isTunable,
                                  new OchSignal(gridType, chSpacing, spacingMult, 1), annotations);
    }

    //FIXME remove when all optical types have two way information methods, see jira tickets
    private static long toGbps(long speed) {
        return speed * 1000;
    }

    private static long toGbpsFromHz(long speed) {
        return speed / 1000;
    }
}

