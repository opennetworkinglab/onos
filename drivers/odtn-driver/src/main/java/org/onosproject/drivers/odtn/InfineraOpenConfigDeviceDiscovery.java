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
package org.onosproject.drivers.odtn;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortDescription.Builder;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * OpenConfig based device and port discovery.
 */
public class InfineraOpenConfigDeviceDiscovery
        extends AbstractHandlerBehaviour
        implements OdtnDeviceDescriptionDiscovery {

    private static final Logger log = getLogger(InfineraOpenConfigDeviceDiscovery.class);

    @Override
    public DeviceDescription discoverDeviceDetails() {
        return new DefaultDeviceDescription(handler().data().deviceId().uri(),
                                            Device.Type.TERMINAL_DEVICE, "Infinera",
                                            "XT-3300", "unknown",
                                            "unknown", new ChassisId());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        try {
            return discoverPorts();
        } catch (Exception e) {
            log.error("Error discovering port details on {}", data().deviceId(), e);
            return ImmutableList.of();
        }
    }

    private List<PortDescription> discoverPorts() throws ConfigurationException, IOException {
        DeviceId did = data().deviceId();
        NetconfSession ns = Optional.ofNullable(handler().get(NetconfController.class))
                .map(c -> c.getNetconfDevice(did))
                .map(NetconfDevice::getSession)
                .orElseThrow(() -> new IllegalStateException("No NetconfSession found for " + did));

        // TODO convert this method into non-blocking form?

        String reply = ns.asyncGet()
                .join().toString();

        // workaround until asyncGet().join() start failing exceptionally
        String data = null;
        if (reply.startsWith("<data")) {
            data = reply;
        }

        if (data == null) {
            log.error("No valid response found from {}:\n{}", did, reply);
            return ImmutableList.of();
        }

        XMLConfiguration cfg = new XMLConfiguration();
        cfg.load(CharSource.wrap(data).openStream());

        return discoverPorts(cfg);
    }

    /**
     * Parses port information from OpenConfig XML configuration.
     *
     * @param cfg tree where the root node is {@literal <data>}
     * @return List of ports
     */
    @VisibleForTesting
    protected List<PortDescription> discoverPorts(XMLConfiguration cfg) {
        // If we want to use XPath
        cfg.setExpressionEngine(new XPathExpressionEngine());

        // converting components into PortDescription.
        List<HierarchicalConfiguration> components = cfg.configurationsAt("interfaces/interface");
        return components.stream()
                .map(this::toPortDescription)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // wrapper to make parsing exception safe
    private PortDescription toPortDescription(HierarchicalConfiguration component) {
        try {
            return toPortDescriptionInternal(component);
        } catch (Exception e) {
            log.error("Unexpected exception parsing component {} on {}",
                      component.getString("name"),
                      data().deviceId(), e);
            return null;
        }
    }

    /**
     * Converts Component subtree to PortDescription.
     *
     * @param component subtree to parse
     * @return PortDescription or null if component is not an ONOS Port
     */
    private PortDescription toPortDescriptionInternal(HierarchicalConfiguration component) {

        // to access other part of <data> tree:
        //log.warn("parent data Node: {}",
        //       ((SubnodeConfiguration) component).getParent().getRootNode().getName());

        String name = component.getString("name");
        checkNotNull(name);
        if (!name.contains("GIGECLIENTCTP")) {
            return null;
        }

        Builder builder = DefaultPortDescription.builder();

        Map<String, String> props = new HashMap<>();
        props.put(OdtnDeviceDescriptionDiscovery.OC_NAME, name);
        props.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, name);

        Pattern clientPattern = Pattern.compile("GIGECLIENTCTP.1-A-2-T(\\d+)");
        Pattern linePattern = Pattern.compile("GIGECLIENTCTP.1-L(\\d+)-1-1");
        Matcher clientMatch = clientPattern.matcher(name);
        Matcher lineMatch = linePattern.matcher(name);

        if (clientMatch.find()) {
            String num = clientMatch.group(1);
            Integer connection = (Integer.parseInt(num) + 1) / 2;
            props.putIfAbsent(PORT_TYPE, OdtnPortType.CLIENT.value());
            props.putIfAbsent(CONNECTION_ID, "connection:" + connection.toString());
            builder.withPortNumber(PortNumber.portNumber(Long.parseLong(num), name));
            builder.type(Type.PACKET);
        } else if (lineMatch.find()) {
            String num = lineMatch.group(1);
            props.putIfAbsent(PORT_TYPE, OdtnPortType.LINE.value());
            props.putIfAbsent(CONNECTION_ID, "connection:" + num);
            builder.withPortNumber(PortNumber.portNumber(100 + Long.parseLong(num), name));
            builder.type(Type.OCH);
        } else {
            return null;
        }

        builder.annotations(DefaultAnnotations.builder().putAll(props).build());
        return builder.build();

    }

}
