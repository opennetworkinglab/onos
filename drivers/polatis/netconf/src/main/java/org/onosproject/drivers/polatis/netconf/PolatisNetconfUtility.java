/*
 * Copyright 2017 Open Networking Foundation
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

package org.onosproject.drivers.polatis.netconf;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Netconf utility for polatis netconf drivers.
 */
public final class PolatisNetconfUtility {

    public static final String KEY_XMLNS = "xmlns=\"http://www.polatis.com/yang/optical-switch\"";
    public static final String KEY_POLATIS_XMLNS = "xmlns=\"http://www.polatis.com/yang/polatis-switch\"";
    public static final String KEY_DATA = "data";
    public static final String KEY_PORT = "port";
    public static final String KEY_PAIR = "pair";
    public static final String KEY_PAIRS = "pairs";
    public static final String KEY_PORTID = "port-id";
    public static final String KEY_PORTPEER = "portPeer";
    public static final String KEY_PORTDIR = "portDir";
    public static final String KEY_PORTCONFIG = "port-config";
    public static final String KEY_SYSTEMALARMS = "system-alarms";
    public static final String KEY_ALARM = "alarm";
    public static final String KEY_CONNS = "cross-connects";
    public static final String KEY_PRODINF = "product-information";
    public static final String KEY_PORTSETSTATE = "port-set-state";
    public static final String KEY_RPCREPLY = "rpc-reply";
    public static final String KEY_OK = "<ok/>";
    public static final String KEY_PORTCONFIG_XMLNS = String.format("%s %s", KEY_PORTCONFIG, KEY_XMLNS);
    public static final String KEY_SYSTEMALARMS_XMLNS = String.format("%s %s", KEY_SYSTEMALARMS, KEY_POLATIS_XMLNS);
    public static final String KEY_CONNS_XMLNS = String.format("%s %s", KEY_CONNS, KEY_XMLNS);
    public static final String KEY_PRODINF_XMLNS = String.format("%s %s", KEY_PRODINF, KEY_XMLNS);
    public static final String KEY_PORTSETSTATE_XMLNS = String.format("%s %s", KEY_PORTSETSTATE, KEY_XMLNS);
    public static final String KEY_DATA_CONNS = String.format("%s.%s", KEY_DATA, KEY_CONNS);
    public static final String KEY_DATA_PRODINF = String.format("%s.%s", KEY_DATA, KEY_PRODINF);
    public static final String KEY_DATA_PORTCONFIG = String.format("%s.%s.%s", KEY_DATA, KEY_PORTCONFIG, KEY_PORT);
    public static final String KEY_DATA_SYSTEMALARMS = String.format("%s.%s.%s", KEY_DATA, KEY_SYSTEMALARMS, KEY_ALARM);
    public static final String KEY_OPM = "opm-power";
    public static final String KEY_OPM_XMLNS = String.format("%s %s", KEY_OPM, KEY_XMLNS);
    public static final String KEY_DATA_OPM = String.format("%s.%s.%s", KEY_DATA, KEY_OPM, KEY_PORT);
    public static final String KEY_DATA_OPM_PORT = String.format("%s.%s.%s", KEY_DATA, KEY_OPM, KEY_PORT);
    public static final String KEY_VOA = "voa";
    public static final String KEY_VOA_XMLNS = String.format("%s %s", KEY_VOA, KEY_XMLNS);
    public static final String KEY_DATA_VOA_PORT = String.format("%s.%s.%s", KEY_DATA, KEY_VOA, KEY_PORT);
    public static final String OPTICAL_CAPABILITY_PREFIX
                        = "http://www.polatis.com/yang/optical-switch?module=optical-switch&amp;revision=";

    public static final String CFG_MODE_MERGE = "merge";

    private static final Logger log = getLogger(PolatisDeviceDescription.class);

    private PolatisNetconfUtility() {
    }

    /**
     * Retrieves session reply information for get operation.
     *
     * @param handler parent driver handler
     * @param filter the filter string of xml content
     * @return the reply string
     */
    public static String netconfGet(DriverHandler handler, String filter) {
        NetconfSession session = getNetconfSession(handler);
        String reply;
        try {
            reply = session.get(filter, null);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve configuration.", e));
        }
        return reply;
    }

    /**
     * Retrieves session reply information for get operation.
     *
     * @param session explicit NETCONF session
     * @param filter the filter string of xml content
     * @return the reply string
     */
    public static String netconfGet(NetconfSession session, String filter) {
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
        NetconfSession session = getNetconfSession(handler);
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
        NetconfSession session = getNetconfSession(handler);
        boolean reply = false;
        try {
            reply = session.editConfig(DatastoreId.RUNNING, mode, cfg);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to edit configuration.", e));
        }
        return reply;
    }

    /**
     * Makes a NETCONF RPC.
     *
     * @param handler parent driver handler
     * @param body body of RPC
     * @return the reply string
     */
    public static String netconfRpc(DriverHandler handler, String body) {
        NetconfSession session = getNetconfSession(handler);
        String reply;
        try {
            reply = session.doWrappedRpc(body);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to make RPC..", e));
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
            HierarchicalConfiguration cfg = XmlConfigParser.loadXmlString(content, false);
            info = cfg.configurationAt(key);
        } catch (IllegalArgumentException e) {
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
            HierarchicalConfiguration cfg = XmlConfigParser.loadXmlString(content, false);
            info = cfg.configurationsAt(key);

        } catch (IllegalArgumentException e) {
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

    public static String opticalRevision(DriverHandler handler) {
        NetconfSession session = getNetconfSession(handler);
        Set<String> capabilities = session.getDeviceCapabilitiesSet();
        for (String c : capabilities) {
            if (c.startsWith(OPTICAL_CAPABILITY_PREFIX)) {
                return c.substring(OPTICAL_CAPABILITY_PREFIX.length());
            }
        }
        return null;
    }

    /**
     * Subscribes for notifications.
     *
     * @param handler parent driver handler
     * @return true on success, false otherwise
     */
    public static boolean subscribe(DriverHandler handler) {
        NetconfSession session = getNetconfSession(handler);
        try {
            session.startSubscription();
        } catch (NetconfException e) {
            log.error("Failed to subscribe for notifications.");
            return false;
        }
        return true;
    }

    /**
     * Returns the NETCONF session of the device.
     *
     * @return session
     */
    private static NetconfSession getNetconfSession(DriverHandler handler) {
        NetconfController controller = checkNotNull(handler.get(NetconfController.class));
        NetconfSession session = controller.getNetconfDevice(handler.data().deviceId()).getSession();
        if (session == null) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve the netconf device."));
        }
        return session;
    }
}
