/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.drivers.oplink;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.List;

import static org.onosproject.drivers.oplink.OplinkOpticalUtility.CHANNEL_SPACING;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.START_CENTER_FREQ;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.STOP_CENTER_FREQ;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;

/**
 * Retrieves the ports from an Oplink optical netconf device.
 */
public class OplinkOpticalDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final String KEY_PORTNAME = "port-name";
    private static final String PORT_DIRECTION = "direction";
    // log
    private static final Logger log = getLogger(OplinkOpticalDeviceDescription.class);

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("No description to be added for device");
        //TODO to be implemented if needed.
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        log.debug("Port description to be added for device {}", data().deviceId());
        String reply = netconfGet(handler(), getPortsFilter());
        List<PortDescription> descriptions = parsePorts(reply);
        return ImmutableList.copyOf(descriptions);
    }

    private String getPortsFilter() {
        return new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlEmpty(KEY_PORTS))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
    }

    private List<PortDescription> parsePorts(String content) {
        List<HierarchicalConfiguration> subtrees = configsAt(content, KEY_DATA_PORTS);
        List<PortDescription> portDescriptions = Lists.newArrayList();
        for (HierarchicalConfiguration portConfig : subtrees) {
            portDescriptions.add(parsePort(portConfig));
        }
        return portDescriptions;
    }

    private PortDescription parsePort(HierarchicalConfiguration cfg) {
        PortNumber portNumber = PortNumber.portNumber(cfg.getLong(KEY_PORTID));
        HierarchicalConfiguration portInfo = cfg.configurationAt(KEY_PORT);
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, portInfo.getString(KEY_PORTNAME))
                .set(PORT_DIRECTION, portInfo.getString(KEY_PORTDIRECT))
                .build();
        return omsPortDescription(portNumber,
                                  true,
                                  START_CENTER_FREQ,
                                  STOP_CENTER_FREQ,
                                  CHANNEL_SPACING.frequency(),
                                  annotations);
    }
}
