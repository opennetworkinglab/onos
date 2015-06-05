/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cord.gui.model.Bundle;
import org.onosproject.cord.gui.model.SubscriberUser;
import org.onosproject.cord.gui.model.XosFunction;
import org.onosproject.cord.gui.model.XosFunctionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * Encapsulation of interactions with XOS.
 */
public class XosManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String HEAD_NODE_IP = "headnodeip";
    private static final String HEAD_NODE_PORT = "headnodeport";
    private static final int PORT_MIN = 1025;
    private static final int PORT_MAX = 65535;

    private static final String TEST_XOS_SERVER_IP = "10.254.1.22";
    private static final String TEST_XOS_SERVER_PORT_STR = "8000";
    private static final int TEST_XOS_SERVER_PORT = 8000;
    private static final String URI_RS = "/rs/";
    private static final String URI_SUBSCRIBER = "/rs/subscriber/%d/";
    private static final String BUNDLE_URI_FORMAT = "services/%s/%s/";


    private String xosServerIp;
    private int xosServerPort;
    private XosManagerRestUtils xosUtilsRs;
    private XosManagerRestUtils xosUtils;


    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * No instantiation (except via unit test).
     */
    XosManager() {}

    private String getXosServerIp() {
        return System.getProperty(HEAD_NODE_IP, TEST_XOS_SERVER_IP);
    }

    private int getXosServerPort() {
        String p = System.getProperty(HEAD_NODE_PORT, TEST_XOS_SERVER_PORT_STR);
        int port;
        try {
            port = Integer.valueOf(p);
        } catch (NumberFormatException e) {
            port = TEST_XOS_SERVER_PORT;
            log.warn("Could not parse port number [{}], using {}", p, port);
        }
        if (port < PORT_MIN || port > PORT_MAX) {
            log.warn("Bad port number [{}], using {}", port, TEST_XOS_SERVER_PORT);
            port = TEST_XOS_SERVER_PORT;
        }
        return port;
    }

    /**
     * Queries XOS for the Subscriber ID lookup data, and returns it.
     */
    public ObjectNode initXosSubscriberLookups() {
        log.info("intDemoSubscriberLookups() called");
        xosServerIp = getXosServerIp();
        xosServerPort = getXosServerPort();
        log.info("Using XOS server at {}:{}", xosServerIp, xosServerPort);

        xosUtilsRs = new XosManagerRestUtils(xosServerIp, xosServerPort, URI_RS);

        // ask XOS for the subscriber ID lookup info
        String result = xosUtilsRs.getRest("subidlookup/");
        log.info("lookup data from XOS: {}", result);

        JsonNode node;
        try {
            node = MAPPER.readTree(result);
        } catch (IOException e) {
            log.error("failed to read subscriber lookup JSON data", e);
            return null;
        }
        return (ObjectNode) node;
    }

    /**
     * Sets a new XOS utils object to bind URL patterns for the
     * given XOS subscriber ID.
     *
     * @param xosSubId XOS subscriber ID
     */
    public void setXosUtilsForSubscriber(int xosSubId) {
        String uri = String.format(URI_SUBSCRIBER, xosSubId);
        xosUtils = new XosManagerRestUtils(xosServerIp, xosServerPort, uri);
    }


    public void initDemoSubscriber() {
        log.info("initDemoSubscriber() called");
        String result = xosUtilsRs.getRest("initdemo/");
        log.info("initdemo data from XOS: {}", result);
    }

    /**
     * Returns the array of users for the subscriber.
     *
     * @return list of users
     */
    public ArrayNode getUserList() {
        log.info("getUserList() called");
        String result = xosUtils.getRest("users/");

        JsonNode node;
        try {
            node = MAPPER.readTree(result);
        } catch (IOException e) {
            log.error("failed to read user list JSON", e);
            return null;
        }

        ObjectNode obj = (ObjectNode) node;
        return (ArrayNode) obj.get("users");
    }


    /**
     * Configure XOS to enable the functions that compose the given bundle,
     * and disable all the others, for the given subscriber.
     *
     * @param bundle new bundle to set
     */
    public void setNewBundle(Bundle bundle) {
        log.info(">> Set New Bundle : {}", bundle.descriptor().id());

        Set<XosFunctionDescriptor> inBundle = bundle.descriptor().functions();
        for (XosFunctionDescriptor xfd: XosFunctionDescriptor.values()) {
            // only process the functions that have a real back-end on XOS
            if (xfd.backend()) {
                String uri = String.format(BUNDLE_URI_FORMAT, xfd.id(),
                                           inBundle.contains(xfd));
                log.info("XOS-URI: {}", uri);
                String result = xosUtils.putRest(uri);
                // TODO: convert JSON result to object and check (if we care)
            }
        }
    }

    /**
     * Configure XOS with new setting for given user and function, for the
     * given subscriber account.
     *
     * @param func specific XOS function
     * @param user user (containing function state)
     */
    public void apply(XosFunction func, SubscriberUser user) {
        log.info(">> Apply : {} for {}", func, user);

        String uriPrefix = "users/" + user.id() + "/";
        String uri = uriPrefix + func.xosUrlApply(user);
        log.info("XOS-URI: {}", uri);
        String result = xosUtils.putRest(uri);
        // TODO: convert JSON result to object and check (if we care)
    }


    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    /**
     * Singleton instance.
     */
    public static final XosManager INSTANCE = new XosManager();
}
