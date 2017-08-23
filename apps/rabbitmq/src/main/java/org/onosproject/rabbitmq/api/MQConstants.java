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
package org.onosproject.rabbitmq.api;

/**
 * Declares the constants used in this module.
 */
public final class MQConstants {
    // No instantiation
    private MQConstants() {
    }

    /**
     * MQ correlation id.
     */
    public static final String CORRELATION_ID = "correlation_id";

    /**
     * MQ exchange name.
     */
    public static final String EXCHANGE_NAME_PROPERTY = "EXCHANGE_NAME_PROPERTY";

    /**
     * MQ routing key.
     */
    public static final String ROUTING_KEY_PROPERTY = "ROUTING_KEY_PROPERTY";

    /**
     * MQ queue name.
     */
    public static final String QUEUE_NAME_PROPERTY = "QUEUE_NAME_PROPERTY";

    /**
     * Switch id connected to onos controller published via json.
     */
    public static final String SWITCH_ID = "switch_id";

    /**
     * Switch's infrastructure device name published via json.
     */
    public static final String INFRA_DEVICE_NAME = "infra_device_name";

    /**
     * Captured event type published via json.
     */
    public static final String EVENT_TYPE = "event_type";

    /**
     * Signifies device event in json.
     */
    public static final String DEVICE_EVENT = "DEVICE_EVENT";

    /**
     * Port connect via switch.
     */
    public static final String PORT_NUMBER = "port_number";

    /**
     * Describes port status enabled or disabled.
     */
    public static final String PORT_ENABLED = "port_enabled";

    /**
     * Specifies port speed.
     */
    public static final String PORT_SPEED = "port_speed";

    /**
     * Specifies sub event types like device added, device updated etc.
     */
    public static final String SUB_EVENT_TYPE = "sub_event_type";

    /**
     * Specifies hardware version of the switch.
     */
    public static final String HW_VERSION = "hw_version";

    /**
     * Specifies switch's manufacturer.
     */
    public static final String MFR = "mfr";

    /**
     * Specifies the serial number of the connected switch.
     */
    public static final String SERIAL = "serial";

    /**
     * Specifies software version of the switch.
     */
    public static final String SW_VERSION = "sw_version";

    /**
     * Specifies chassis id of the switch.
     */
    public static final String CHASIS_ID = "chassis_id";

    /**
     * Specifies event occurrence time.
     */
    public static final String OCC_TIME = "occurrence_time";

    /**
     * Specifies switch's available time.
     */
    public static final String AVAILABLE = "available_time";

    /**
     * Specifies packet_in port details.
     */
    public static final String IN_PORT = "in_port";

    /**
     * Specifies port is logical or not.
     */
    public static final String LOGICAL = "logical";

    /**
     * Specifies packet received time.
     */
    public static final String RECEIVED = "received";

    /**
     * Specifies message type.
     */
    public static final String MSG_TYPE = "msg_type";

    /**
     * Specifies packet type.
     */
    public static final String PKT_TYPE = "PACKET_IN";

    /**
     * Specifies sub message type under msg_type.
     */
    public static final String SUB_MSG_TYPE = "sub_msg_type";

    /**
     * Specifies Ethernet type of the packet.
     */
    public static final String ETH_TYPE = "eth_type";

    /**
     * Source MAC address of the packet.
     */
    public static final String SRC_MAC_ADDR = "src_mac_address";

    /**
     * Destination MAC address of the packet.
     */
    public static final String DEST_MAC_ADDR = "dest_mac_address";

    /**
     * Specifies VLAN ID of the packet.
     */
    public static final String VLAN_ID = "vlan_id";

    /**
     * Specifies if the packet is a Broadcast or not.
     */
    public static final String B_CAST = "is_bcast";

    /**
     * Specifies if the packet is a Multicast or not.
     */
    public static final String M_CAST = "is_mcast";

    /**
     * Specifies if the packet is padded or not.
     */
    public static final String PAD = "pad";

    /**
     * Specifies priority of the packet.
     */
    public static final String PRIORITY_CODE = "priority_code";

    /**
     * Specifies length of the payload.
     */
    public static final String DATA_LEN = "data_length";

    /**
     * Packet payload(raw) in unicode format.
     */
    public static final String PAYLOAD = "payload";

    /**
     * Network topology type TopologyEvent.Type.
     */
    public static final String TOPO_TYPE = "topology_type";

    /**
     * Represents number of strongly connected components in the topology.
     */
    public static final String CLUSTER_COUNT = "cluster_count";

    /**
     * Cost for doing topology computation.
     */
    public static final String COMPUTE_COST = "compute_cost";

    /**
     * Represents topology creation time.
     */
    public static final String CREATE_TIME = "creation_time";

    /**
     * Represents number of infrastructure devices in the topology.
     */
    public static final String DEVICE_COUNT = "device_count";

    /**
     * Represents number of links in the topology.
     */
    public static final String LINK_COUNT = "link_count";

    /**
     * Represents links destination DeviceId.
     */
    public static final String DEST = "dst";

    /**
     * Represents links source DeviceId.
     */
    public static final String SRC = "src";

    /**
     * True if the link is expected, false otherwise.
     */
    public static final String EXPECTED = "expected";

    /**
     * Represents link state ACTIVE or INACTIVE.
     */
    public static final String STATE = "state";

    /**
     * Represents link type like LINK_ADDED, LINK_UPDATE, LINK_REMOVED.
     */
    public static final String LINK_TYPE = "link_type";

    /**
     * Represents the rabbit mq server properties stored in resources directory.
     */
    public static final String MQ_PROP_NAME = "rabbitmq.properties";

    /**
     * Represents rabbit mq module name for app initialization.
     */
    public static final String ONOS_APP_NAME = "org.onosproject.rabbitmq";

    /**
     * Represents rabbit mq publisher correlation identifier.
     */
    public static final String SENDER_COR_ID = "rmq.sender.correlation.id";

    /**
     * Represents rabbit mq server protocol.
     */
    public static final String SERVER_PROTO = "rmq.server.protocol";

    /**
     * Represents rabbit mq server user name.
     */
    public static final String SERVER_UNAME = "rmq.server.username";

    /**
     * Represents rabbit mq server password.
     */
    public static final String SERVER_PWD = "rmq.server.password";

    /**
     * Represents rabbit mq server address.
     */
    public static final String SERVER_ADDR = "rmq.server.ip.address";

    /**
     * Represents rabbit mq server port.
     */
    public static final String SERVER_PORT = "rmq.server.port";

    /**
     * Represents rabbit mq server vhost.
     */
    public static final String SERVER_VHOST = "rmq.server.vhost";

    /**
     * Represents rabbit mq server exchange.
     */
    public static final String SENDER_EXCHG = "rmq.sender.exchange";

    /**
     * Represents rabbit mq server routing key binds exchange and queue.
     */
    public static final String ROUTE_KEY = "rmq.sender.routing.key";

    /**
     * Represents rabbit mq server queue for message delivery.
     */
    public static final String SENDER_QUEUE = "rmq.sender.queue";

    /**
     * Represents rabbit mq server topic.
     */
    public static final String TOPIC = "topic";

    /**
     * Represents correlation ID of the sender.
     */
    public static final String COR_ID = "onos->rmqserver";
}
