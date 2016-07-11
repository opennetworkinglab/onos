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

package org.onosproject.drivers.huawei;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Retrieves the ports from a Huawei vrp device via netconf.
 */
public class HuaweiDeviceDescription extends AbstractHandlerBehaviour
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
            reply = session.get(interfaceRequestBuilder());
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }
        List<PortDescription> descriptions =
                parseHuaweiPorts(XmlConfigParser.
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
        rpc.append("<lldp xmlns=\"http://www.huawei.com/netconf/vrp\"");
        rpc.append(" content-version=\"1.0\" format-version=\"1.0\">");
        rpc.append("<lldpInterfaces>");
        rpc.append("<lldpInterface/>");
        rpc.append("</lldpInterfaces>");
        rpc.append("</lldp>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");
        return rpc.toString();
    }

    private String interfaceRequestBuilder() {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //Message ID is injected later.
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get-config>");
        rpc.append("<source>");
        rpc.append("<running />");
        rpc.append("</source>");
        rpc.append("<filter type=\"subtree\">");
        rpc.append("<ifm xmlns=\"http://www.huawei.com/netconf/vrp\"");
        rpc.append(" content-version=\"1.0\" format-version=\"1.0\">");
        rpc.append("<interfaces>");
        rpc.append("<interface>");
        rpc.append("<ifName/>");
        rpc.append("<ifNumber/>");
        rpc.append("<ifPhyType/>");
        rpc.append("<ifAdminStatus/>");
        rpc.append("</interface>");
        rpc.append("</interfaces>");
        rpc.append("</ifm>");
        rpc.append("</filter>");
        rpc.append("</get-config>");
        rpc.append("</rpc>");
        return rpc.toString();
    }

    /**
     * Parses a configuration and returns a set of ports for the huawei device.
     *
     * @param cfg a hierarchical configuration
     * @return a list of port descriptions
     */
    private static List<PortDescription> parseHuaweiPorts(HierarchicalConfiguration cfg) {
        AtomicInteger counter = new AtomicInteger(1);
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
//                cfg.configurationsAt("data.lldp.lldpInterfaces.lldpInterface");
                cfg.configurationsAt("data.ifm.interfaces.interface");
        for (HierarchicalConfiguration portConfig : subtrees) {
                portDescriptions.add(parseToPort(portConfig, counter.getAndIncrement()));
        }
        return portDescriptions;
    }

    private static PortDescription parseToPort(HierarchicalConfiguration cfg, long count) {
        PortNumber portNumber = PortNumber.portNumber(count);
        boolean enabled = cfg.getString("ifAdminStatus").equals("up");
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, cfg.getString("ifName")).
                set("portid", UUID.randomUUID().toString()).
                build();
        return new DefaultPortDescription(portNumber, enabled, annotations);
    }

}