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

package org.onosproject.drivers.fujitsu;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieves the ports from a Fujitsu T100 device via netconf.
 */
public class FujitsuT100DeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.info("No description to be added for device");
        //TODO to be implemented if needed.
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply;
        try {
            reply = session.get(requestBuilder());
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }
        List<PortDescription> descriptions =
                parseFujitsuT100Ports(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes())));
        return ImmutableList.copyOf(descriptions);
    }

    /**
     * Builds a request crafted to get the configuration required to create port
     * descriptions for the device.
     *
     * @return The request string.
     */
    private String requestBuilder() {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //Message ID is injected later.
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter type=\"subtree\">");
        rpc.append("<interfaces xmlns=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\">");
        rpc.append("</interfaces>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");
        return rpc.toString();
    }

    /**
     * Parses a configuration and returns a set of ports for the fujitsu T100.
     *
     * @param cfg a hierarchical configuration
     * @return a list of port descriptions
     */
    private static List<PortDescription> parseFujitsuT100Ports(HierarchicalConfiguration cfg) {
        AtomicInteger counter = new AtomicInteger(1);
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt("data.interfaces.interface");
        for (HierarchicalConfiguration portConfig : subtrees) {
            if (!portConfig.getString("name").contains("LCN") &&
                    !portConfig.getString("name").contains("LMP") &&
                    portConfig.getString("type").equals("ianaift:ethernetCsmacd")) {
                portDescriptions.add(parseT100OduPort(portConfig, counter.getAndIncrement()));
            } else if (portConfig.getString("type").equals("ianaift:otnOtu")) {
                portDescriptions.add(parseT100OchPort(portConfig, counter.getAndIncrement()));
            }
        }
        return portDescriptions;
    }

    private static PortDescription parseT100OchPort(HierarchicalConfiguration cfg, long count) {
        PortNumber portNumber = PortNumber.portNumber(count);
        HierarchicalConfiguration otuConfig = cfg.configurationAt("otu");
        boolean enabled = otuConfig.getString("administrative-state").equals("up");
        OduSignalType signalType = otuConfig.getString("rate").equals("OTU4") ? OduSignalType.ODU4 : null;
        //Unsure how to retreive, outside knowledge it is tunable.
        boolean isTunable = true;
        OchSignal lambda = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, 0, 4);
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, cfg.getString("name")).
                build();
        return ochPortDescription(portNumber, enabled, signalType, isTunable, lambda, annotations);
    }

    private static PortDescription parseT100OduPort(HierarchicalConfiguration cfg, long count) {
        PortNumber portNumber = PortNumber.portNumber(count);
        HierarchicalConfiguration ethernetConfig = cfg.configurationAt("ethernet");
        boolean enabled = ethernetConfig.getString("administrative-state").equals("up");
        //Rate is in kbps
        CltSignalType signalType = ethernetConfig.getString("rate").equals("100000000") ?
                CltSignalType.CLT_100GBE : null;
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, cfg.getString("name")).
                build();
        return oduCltPortDescription(portNumber, enabled, signalType, annotations);
    }

}
