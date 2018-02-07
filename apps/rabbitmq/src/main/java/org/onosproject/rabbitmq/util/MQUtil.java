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
package org.onosproject.rabbitmq.util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.onlab.packet.EthType;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.packet.InboundPacket;
import org.osgi.service.component.ComponentContext;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.rabbitmq.api.MQConstants.*;

/**
 * MQ utility class for constructing server url, packet message, device message,
 * topology message and link message.
 */
public final class MQUtil {

    private static final String COLON = ":";
    private static final String AT = "@";
    private static final String CDFS = "://";
    private static final String FS = "/";
    private static final String UTF8 = "UTF-8";
    private static final Logger log = LoggerFactory.getLogger(MQUtil.class);

    private MQUtil() {
    }

    /**
     * Returns the MQ server url.
     *
     * @param proto    mq server protocol
     * @param userName mq server username
     * @param password mq server password
     * @param ipAddr   server ip address
     * @param port     server port
     * @param vhost    server vhost
     * @return         server url
     */
    public static String getMqUrl(String proto, String userName,
            String password, String ipAddr, String port,
            String vhost) {
        StringBuilder urlBuilder = new StringBuilder();
        try {
            urlBuilder.append(proto).append(CDFS).append(userName).append(COLON)
                      .append(password).append(AT)
                      .append(ipAddr).append(COLON).append(port).append(FS)
                      .append(URLEncoder.encode(vhost, UTF8));
        } catch (UnsupportedEncodingException e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
        }
        return urlBuilder.toString().replaceAll("\\s+", "");
    }

    /**
     * Initializes and returns publisher channel configuration.
     *
     * @param  exchange   the configured mq exchange name
     * @param  routingKey the configured mq routing key
     * @param  queueName  the configured mq queue name
     * @return            the server url
     */
    public static Map<String, String> rfProducerChannelConf(String exchange,
            String routingKey, String queueName) {
        Map<String, String> channelConf = new HashMap<>();
        channelConf.put(EXCHANGE_NAME_PROPERTY, exchange);
        channelConf.put(ROUTING_KEY_PROPERTY, routingKey);
        channelConf.put(QUEUE_NAME_PROPERTY, queueName);
        return channelConf;
    }

    /**
     * Returns a JSON representation of the given device event.
     *
     * @param  event the device event
     * @return       the device event json message
     */
    public static JsonObject json(DeviceEvent event) {
        JsonObject jo = new JsonObject();
        jo.addProperty(SWITCH_ID, event.subject().id().toString());
        jo.addProperty(INFRA_DEVICE_NAME, event.subject().type().name());
        jo.addProperty(EVENT_TYPE, DEVICE_EVENT);
        if (event.port() != null) {
            jo.addProperty(PORT_NUMBER, event.port().number().toLong());
            jo.addProperty(PORT_ENABLED, event.port().isEnabled());
            jo.addProperty(PORT_SPEED, event.port().portSpeed());
            jo.addProperty(SUB_EVENT_TYPE,
                    event.type().name() != null ? event.type().name() : null);
        } else {
            jo.addProperty(SUB_EVENT_TYPE,
                    event.type().name() != null ? event.type().name() : null);
        }
        jo.addProperty(HW_VERSION, event.subject().hwVersion());
        jo.addProperty(MFR, event.subject().manufacturer());
        jo.addProperty(SERIAL, event.subject().serialNumber());
        jo.addProperty(SW_VERSION, event.subject().swVersion());
        jo.addProperty(CHASIS_ID, event.subject().chassisId().id());
        jo.addProperty(OCC_TIME, new Date(event.time()).toString());
        return jo;
    }

    /**
     * Returns a JSON representation of the given packet context.
     *
     * @param  context the packet context
     * @return         the inbound packetjson message
     */
    public static JsonObject json(PacketContext context) {
        JsonObject jo = new JsonObject();
        InboundPacket pkt = context.inPacket();
        // parse connection host
        jo.addProperty(SWITCH_ID, pkt.receivedFrom().deviceId().toString());
        jo.addProperty(IN_PORT, pkt.receivedFrom().port().name());
        jo.addProperty(LOGICAL, pkt.receivedFrom().port().isLogical());
        jo.addProperty(RECEIVED, new Date(context.time()).toString());
        jo.addProperty(MSG_TYPE, PKT_TYPE);
        // parse ethernet
        jo.addProperty(SUB_MSG_TYPE,
                EthType.EtherType.lookup(pkt.parsed().getEtherType()).name());
        jo.addProperty(ETH_TYPE, pkt.parsed().getEtherType());
        jo.addProperty(SRC_MAC_ADDR, pkt.parsed().getSourceMAC().toString());
        jo.addProperty(DEST_MAC_ADDR, pkt.parsed().getDestinationMAC().toString());
        jo.addProperty(VLAN_ID, pkt.parsed().getVlanID());
        jo.addProperty(B_CAST, pkt.parsed().isBroadcast());
        jo.addProperty(M_CAST, pkt.parsed().isMulticast());
        jo.addProperty(PAD, pkt.parsed().isPad());
        jo.addProperty(PRIORITY_CODE, pkt.parsed().getPriorityCode());
        // parse bytebuffer
        jo.addProperty(DATA_LEN, pkt.unparsed().array().length);
        jo.addProperty(PAYLOAD, pkt.unparsed().asCharBuffer().toString());
        return jo;
    }

    /**
     * Returns a JSON representation of the given topology event.
     *
     * @param  event the topology event
     * @return       the topology event json message
     */
    public static JsonObject json(TopologyEvent event) {
        Topology topology = event.subject();
        JsonObject jo = new JsonObject();
        jo.addProperty(TOPO_TYPE, TopologyEvent.Type.TOPOLOGY_CHANGED.name());
        jo.addProperty(CLUSTER_COUNT, topology.clusterCount());
        jo.addProperty(COMPUTE_COST, topology.computeCost());
        jo.addProperty(CREATE_TIME, new Date(topology.creationTime()).toString());
        jo.addProperty(DEVICE_COUNT, topology.deviceCount());
        jo.addProperty(LINK_COUNT, topology.linkCount());
        jo.addProperty(AVAILABLE, new Date(topology.time()).toString());
        return jo;
    }

    /**
     * Returns a JSON representation of the given link event.
     *
     * @param  event the link event
     * @return       the link event json message
     */
    public static JsonObject json(LinkEvent event) {
        Link link = event.subject();
        JsonObject jo = new JsonObject();
        jo.addProperty(EVENT_TYPE, event.type().name());
        jo.addProperty(DEST, link.dst().deviceId().toString());
        jo.addProperty(SRC, link.src().deviceId().toString());
        jo.addProperty(EXPECTED, link.isExpected());
        jo.addProperty(STATE, link.state().name());
        jo.addProperty(LINK_TYPE, link.type().name());
        return jo;
    }

    /**
     * Handles load mq property file from resources and returns Properties.
     *
     * @param  context          the component context
     * @return                  the mq server properties
     * @throws RuntimeException if property file not found.
     */
    public static Properties getProp(ComponentContext context) {
        InputStream is;
        URL configUrl;
        try {
            configUrl = context.getBundleContext().getBundle()
                                                  .getResource(MQ_PROP_NAME);
            is = configUrl.openStream();
        } catch (Exception ex) {
            // This will be used only during junit test case since bundle
            // context will be available during runtime only.
            // FIXME - this should be configured with component config when running as a test
            is = MQUtil.class.getClassLoader().getResourceAsStream(MQ_PROP_NAME);
        }

        Properties properties;
        try {
            properties = new Properties();
            properties.load(is);
        } catch (Exception e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalStateException(e);
        }
        return properties;
    }
}
