/*
 *
 *  * Copyright 2016-present Open Networking Laboratory
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.onosproject.drivers.ciena;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the ports from a Ciena WaveServer Rest device.
 */
public class CienaWaveserverDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    private static final String SPEED = "speed";
    private static final String GBPS = "Gbps";
    private static final String PORT_ID = "port-id";
    private static final String XML = "xml";
    private static final String ENABLED = "enabled";
    private static final String EMPTY_STRING = "";
    private static final String NAME = "name";
    private static final String ADMIN_STATE = "admin-state";

    private static final ArrayList<String> LINESIDE_PORT_ID = Lists.newArrayList(
            "4", "48");

    private static final String GENERAL_PORT_REQUEST =
            "ws-ports?config=true&format=xml&depth=unbounded";
    private static final String SPECIFIC_PORT_PATH = "ws-ptps/ptp/";
    private static final String SPECIFIC_PORT_CONFIG =
            "/ptp-config?config=true&format=xml&depth=unbounded";
    //HTTP strings
//    private static final String GENERAL_PORT_REQUEST =
//            "/yang-api/datastore/ws-ports?config=true&format=xml&depth=unbounded";
//    private static final String SPECIFIC_PORT_PATH = "/yang-api/datastore/ws-ptps/ptp/";
//    private static final String SPECIFIC_PORT_CONFIG =
//            "/ptp-config?config=true&format=xml&depth=unbounded";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.info("No description to be added for device");
        //TODO to be implemented if needed.
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        return getPorts();
    }

    private List<PortDescription> getPorts() {
        List<PortDescription> ports = Lists.newArrayList();
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();


        HierarchicalConfiguration config = XmlConfigParser.
                loadXml(controller.get(deviceId, GENERAL_PORT_REQUEST, XML));
        List<HierarchicalConfiguration> portsConfig =
                parseWaveServerCienaPorts(config);
        portsConfig.forEach(sub -> {
            String portId = sub.getString(PORT_ID);
            String name = sub.getString(NAME);
            if (LINESIDE_PORT_ID.contains(portId)) {
                String txName = name + " Tx";
                DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, txName);
                String wsportInfoRequest = SPECIFIC_PORT_PATH + portId +
                        SPECIFIC_PORT_CONFIG;
                ports.add(parseWaveServerCienaOchPorts(
                        sub.getLong(PORT_ID),
                        toGbps(Long.parseLong(sub.getString(SPEED).replace(GBPS, EMPTY_STRING)
                                                      .replace(" ", EMPTY_STRING))),
                        XmlConfigParser.loadXml(controller.get(deviceId, wsportInfoRequest, XML)),
                        annotations.build()));
                //adding corresponding opposite side port
                String rxName = name.replace(".1", ".2") + " Rx";
                ports.add(parseWaveServerCienaOchPorts(
                        sub.getLong(PORT_ID) + 1,
                        toGbps(Long.parseLong(sub.getString(SPEED).replace(GBPS, EMPTY_STRING)
                                                      .replace(" ", EMPTY_STRING))),
                        XmlConfigParser.loadXml(controller.get(deviceId, wsportInfoRequest, XML)),
                        annotations.set(AnnotationKeys.PORT_NAME, rxName)
                                .build()));
            } else if (!portId.equals("5") && !portId.equals("49")) {
                DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, name);
                //FIXME change when all optical types have two way information methods, see jira tickets
                final int speed100GbpsinMbps = 100000;
                CltSignalType cltType = toGbps(Long.parseLong(
                        sub.getString(SPEED).replace(GBPS, EMPTY_STRING)
                                .replace(" ", EMPTY_STRING))) == speed100GbpsinMbps ?
                        CltSignalType.CLT_100GBE : null;
                ports.add(oduCltPortDescription(PortNumber.portNumber(sub.getLong(PORT_ID)),
                                                sub.getString(ADMIN_STATE).equals(ENABLED),
                                                cltType, annotations.build()));
            }
        });
        return ImmutableList.copyOf(ports);
    }

    public static List<HierarchicalConfiguration> parseWaveServerCienaPorts(HierarchicalConfiguration cfg) {
        return cfg.configurationsAt("ws-ports.port-interface");
    }

    public static PortDescription parseWaveServerCienaOchPorts(long portNumber, long oduPortSpeed,
                                                               HierarchicalConfiguration config,
                                                               SparseAnnotations annotations) {
        final List<String> tunableType = Lists.newArrayList("Performance-Optimized", "Accelerated");
        final String transmitterPath = "ptp-config.transmitter-state";
        final String tunablePath = "ptp-config.adv-config.tx-tuning-mode";
        final String gridTypePath = "ptp-config.adv-config.wl-spacing";
        final String frequencyPath = "ptp-config.adv-config.frequency";

        boolean isEnabled = config.getString(transmitterPath).equals("enabled");
        boolean isTunable = tunableType.contains(config.getString(tunablePath));

        //FIXME change when all optical types have two way information methods, see jira tickets
        final int speed100GbpsinMbps = 100000;
        OduSignalType oduSignalType = oduPortSpeed == speed100GbpsinMbps ? OduSignalType.ODU4 : null;
        GridType gridType = config.getString(gridTypePath).equals("FlexGrid") ? GridType.FLEX : null;
        ChannelSpacing chSpacing = gridType == GridType.FLEX ? ChannelSpacing.CHL_6P25GHZ : null;

        //Working in Ghz //(Nominal central frequency - 193.1)/channelSpacing = spacingMultiplier
        final int baseFrequency = 193100;
        int spacingMult = (int) (toGbps((Integer.parseInt(config.getString(frequencyPath)) -
                baseFrequency)) / toGbpsFromHz(chSpacing.frequency().asHz())); //FIXME is there a better way ?

        return ochPortDescription(PortNumber.portNumber(portNumber), isEnabled, oduSignalType, isTunable,
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

