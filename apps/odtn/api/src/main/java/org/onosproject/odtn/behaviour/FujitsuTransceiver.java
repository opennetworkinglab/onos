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
package org.onosproject.odtn.behaviour;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.utils.openconfig.LogicalChannel;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toResourceData;

import static org.slf4j.LoggerFactory.getLogger;


public class FujitsuTransceiver extends AbstractHandlerBehaviour
        implements ConfigurableTransceiver {

    private final Logger log = getLogger(getClass());
    private static final String TERMINAL_DEVICE_TRANSCEIVER = "transceiver";
    private static final String TERMINAL_DEVICE_LOGICAL_CHANNEL = "data.terminal-device.logical-channels.channel";
    private static final String LOGICAL_CHANNEL_TRIB_PROTOCOL = "trib-protocol";
    private static final String LOGICAL_CHANNEL_INGRESS = "ingress";
    private static final String LOGICAL_CHANNEL_CONFIG = "config";
    private static final String LOGICAL_CHANNEL_CONFIG_INDEX = "index";
    private static final String ETHERNET_PROTOCOL_100_G = "PROT_100GE";


    /**
     * Generate configuration to enable/disable transceiver.
     *
     * @param client side port of transceiver to enable/disable
     * @param line   side port of transceiver to enable/disable
     * @param enable or disable
     * @return XML documents (List to handle configuration with multiple-roots)
     *
     * channel_index = /terminal-device/logical-channels/channel/index
     * send channel.config.admin-state = enable to NE (i.e. ENABLED/DISABLED)
     * break
     */
    @Override
    public List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable) {
        DeviceId devId = this.data().deviceId();
        Port port = handler()
                .get(DeviceService.class)
                .getPort(devId, PortNumber.portNumber(client.toLong()));
        if (port == null) {
            log.error("{} does not exist on {}", client, devId);
            return Collections.emptyList();
        }

        String component = port.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(component)) {
            log.error(" annotations not found on {}@{}", client, devId);
            return Collections.emptyList();
        }

        return enable(component, enable);
    }


    @Override
    public List<CharSequence> enable(String component, boolean enable) {
        String terminalDevice = null;
        try {
            terminalDevice = getNetconfSession().doWrappedRpc(buildGetTerminalDevice());
        } catch (NetconfException e) {
            log.error("Exception occurred while retrieving rpc {}", e);
            return Collections.emptyList();
        }

        Long index = parseLogicalChannelIndex(XmlConfigParser
                .loadXml(new ByteArrayInputStream(terminalDevice.getBytes())), component);
        if (index == null) {
            log.error(" Component {} not found in the terminal device", component);
            return Collections.emptyList();
        }

        ResourceId resourceId = ResourceId.builder().build();
        List<DataNode> dataNodes = LogicalChannel.enable(index, "", enable);
        return Lists.transform(dataNodes,
                dataNode -> toCharSequence(
                        toXmlCompositeStream(toCompositeData(toResourceData(resourceId, dataNode)))));
    }


    private Long parseLogicalChannelIndex(HierarchicalConfiguration terminalDevice, String component) {
        if (terminalDevice.equals(null) || component.equals(null)) {
            log.debug("Nothing to parse in logical channels");
            return null;
        }
        Long channelIndex = null;
        List<HierarchicalConfiguration> subTrees = checkNotNull(terminalDevice
                .configurationsAt(TERMINAL_DEVICE_LOGICAL_CHANNEL));
        for (HierarchicalConfiguration channelConfig : subTrees) {
            String channel = channelConfig
                    .configurationAt(LOGICAL_CHANNEL_CONFIG)
                    .getString(LOGICAL_CHANNEL_TRIB_PROTOCOL);
            log.debug("{}", channel);
            if (channel.contains(ETHERNET_PROTOCOL_100_G)) {

                String transceiver = channelConfig.configurationAt(LOGICAL_CHANNEL_INGRESS)
                        .configurationAt(LOGICAL_CHANNEL_CONFIG)
                        .getString(TERMINAL_DEVICE_TRANSCEIVER);
                log.debug("{}", transceiver);
                if (transceiver.toLowerCase().equalsIgnoreCase(component)) {
                    channelIndex = Long.parseLong(channelConfig
                            .configurationAt(LOGICAL_CHANNEL_CONFIG)
                            .getString(LOGICAL_CHANNEL_CONFIG_INDEX));
                    break;
                }
            }
        }

        return new Long(channelIndex);
    }

    private NetconfSession getNetconfSession() {
        DeviceId did = checkNotNull(handler().data().deviceId());
        NetconfController netconfController = checkNotNull(handler().get(NetconfController.class));
        NetconfDevice netconfDevice = netconfController.getDevicesMap().get(did);
        return checkNotNull(netconfDevice.getSession());
    }

    private String buildGetTerminalDevice() {
        return "<get-config>\n" +
                "  <source>\n" +
                "    <running/>\n" +
                "  </source> \n" +
                "  <filter type=\"subtree\">\n" +
                "    <terminal-device xmlns=\"http://openconfig.net/yang/terminal-device\"> \n" +
                "    </terminal-device>\n" +
                "  </filter>\n" +
                "</get-config>";
    }

}
