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

    private static final String TEST_XOS_SERVER_ADDRESS = "10.254.1.22";
    private static final int TEST_XOS_SERVER_PORT = 8000;
    private static final String URI_RS = "/rs/";
    private static final String URI_SUBSCRIBER = "/rs/subscriber/%d/";

    private final XosManagerRestUtils xosUtilsRs =
            new XosManagerRestUtils(TEST_XOS_SERVER_ADDRESS,
                                    TEST_XOS_SERVER_PORT, URI_RS);

    private XosManagerRestUtils xosUtils;


    private final Logger log = LoggerFactory.getLogger(getClass());

    private int demoId;

    /**
     * No instantiation (except via unit test).
     */
    XosManager() {}

    /**
     * Queries XOS for the Demo Subscriber ID and caches it for future calls.
     */
    public int initDemoSubscriber() {
        log.info("intDemoSubscriber() called");
        String result = xosUtilsRs.getRest("initdemo/");
        log.info("from XOS: {}", result);

        JsonNode node;
        try {
            node = MAPPER.readTree(result);
        } catch (IOException e) {
            log.error("failed to read demo subscriber JSON", e);
            return 0;
        }

        ObjectNode obj = (ObjectNode) node;
        demoId = obj.get("id").asInt();
        log.info("Using DEMO subscriber ID {}.", demoId);

        String uri = String.format(URI_SUBSCRIBER, demoId);
        xosUtils = new XosManagerRestUtils(TEST_XOS_SERVER_ADDRESS,
                                           TEST_XOS_SERVER_PORT, uri);
        return demoId;
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
        log.info("\n>> Set New Bundle : " + bundle.descriptor().id());

        String uriFmt = "services/%s/%s";
        Set<XosFunctionDescriptor> inBundle = bundle.descriptor().functions();
        for (XosFunctionDescriptor xfd: XosFunctionDescriptor.values()) {
            // only process the functions that have a real back-end on XOS
            if (xfd.backend()) {
                String uri = String.format(uriFmt, xfd.id(), inBundle.contains(xfd));
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
        log.info("\n>> Apply : " + func + " for " + user);

        String uriPrefix = "users/" + user.id() + "/";
        String uri = uriPrefix + func.xosUrlApply(user);
        String result = xosUtils.putRest(uri);
        // TODO: convert JSON result to object and check (if we care)
    }


    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    /**
     * Singleton instance.
     */
    public static final XosManager INSTANCE = new XosManager();
}
