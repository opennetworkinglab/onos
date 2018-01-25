/*
 * Copyright 2016 Open Networking Foundation
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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Netconf utility for oplink netconf drivers.
 */
public final class OplinkNetconfUtility {

    // public used nodes
    public static final String CFG_MODE_MERGE = "merge";
    public static final String CFG_MODE_NONE = "none";
    public static final String CFG_OPT_DELETE = "nc:operation=\"delete\"";
    public static final String KEY_XMLNS = "xmlns=\"http://com/att/device\"";
    public static final String KEY_DATA = "data";
    public static final String KEY_OPENOPTICALDEV = "open-optical-device";
    public static final String KEY_CONNS = "connections";
    public static final String KEY_CONNID = "connection-id";
    public static final String KEY_PORTS = "ports";
    public static final String KEY_PORTID = "port-id";
    public static final String KEY_PORT = "port";
    public static final String KEY_PORTDIRECT = "port-direction";
    public static final String KEY_CHATT = "attenuation";
    public static final String KEY_DATA_CONNS = String.format("%s.%s.%s", KEY_DATA, KEY_OPENOPTICALDEV, KEY_CONNS);
    public static final String KEY_DATA_PORTS = String.format("%s.%s.%s", KEY_DATA, KEY_OPENOPTICALDEV, KEY_PORTS);
    public static final String KEY_OPENOPTICALDEV_XMLNS = String.format("%s %s", KEY_OPENOPTICALDEV, KEY_XMLNS);

    private OplinkNetconfUtility() {
    }

    /**
     * Retrieves session reply information for get operation.
     *
     * @param handler parent driver handler
     * @param filter the filter string of xml content
     * @return the reply string
     */
    public static String netconfGet(DriverHandler handler, String filter) {
        NetconfController controller = checkNotNull(handler.get(NetconfController.class));
        NetconfSession session = controller.getNetconfDevice(handler.data().deviceId()).getSession();
        String reply;
        try {
            reply = session.get(filter, null);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.", e));
        }
        return reply;
    }

    /**
     * Retrieves session reply information for get config operation.
     *
     * @param handler parent driver handler
     * @param filter the filter string of xml content
     * @return the reply string
     */
    public static String netconfGetConfig(DriverHandler handler, String filter) {
        NetconfController controller = checkNotNull(handler.get(NetconfController.class));
        NetconfSession session = controller.getNetconfDevice(handler.data().deviceId()).getSession();
        String reply;
        try {
            reply = session.getConfig(DatastoreId.RUNNING, filter);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.", e));
        }
        return reply;
    }

    /**
     * Retrieves session reply information for edit config operation.
     *
     * @param handler parent driver handler
     * @param mode selected mode to change the configuration
     * @param cfg the new configuration to be set
     * @return the reply string
     */
    public static boolean netconfEditConfig(DriverHandler handler, String mode, String cfg) {
        NetconfController controller = checkNotNull(handler.get(NetconfController.class));
        NetconfSession session = controller.getNetconfDevice(handler.data().deviceId()).getSession();
        boolean reply = false;
        try {
            reply = session.editConfig(DatastoreId.RUNNING, mode, cfg);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to edit configuration.", e));
        }
        return reply;
    }

    /**
     * Retrieves specified node hierarchical configuration from the xml information.
     *
     * @param content the xml information
     * @param key the configuration key node
     * @return the hierarchical configuration, null if exception happens
     */
    public static HierarchicalConfiguration configAt(String content, String key) {
        HierarchicalConfiguration info;
        try {
            HierarchicalConfiguration cfg = XmlConfigParser.loadXmlString(content);
            info = cfg.configurationAt(key);
        } catch (Exception e) {
            // Accept null for information polling
            return null;
        }
        return info;
    }

    /**
     * Retrieves specified node hierarchical configurations from the xml information.
     *
     * @param content the xml information
     * @param key the configuration key node
     * @return the hierarchical configurations, empty if exception happens
     */
    public static List<HierarchicalConfiguration> configsAt(String content, String key) {
        List<HierarchicalConfiguration> info;
        try {
            HierarchicalConfiguration cfg = XmlConfigParser.loadXmlString(content);
            info = cfg.configurationsAt(key);
        } catch (Exception e) {
            // Accept empty for information polling
            return ImmutableList.of();
        }
        return info;
    }

    /**
     * Makes a xml format sentence.
     *
     * @param node the node name
     * @param content the node content
     * @return the xml format sentence
     */
    public static String xml(String node, String content) {
        return String.format("<%s>%s</%s>", node, content, node);
    }

    /**
     * Makes a xml format open tag.
     *
     * @param node the node name
     * @return the xml head format string
     */
    public static String xmlOpen(String node) {
        return String.format("<%s>", node);
    }

    /**
     * Makes a xml format close tag.
     *
     * @param node the node name
     * @return the xml end format string
     */
    public static String xmlClose(String node) {
        return String.format("</%s>", node);
    }

    /**
     * Makes a xml format empty tag.
     *
     * @param node the node name
     * @return the xml format of empty tag
     */
    public static String xmlEmpty(String node) {
        return String.format("<%s/>", node);
    }
}
