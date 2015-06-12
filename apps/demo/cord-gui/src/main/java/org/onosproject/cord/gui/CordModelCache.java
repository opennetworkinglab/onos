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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.onosproject.cord.gui.model.Bundle;
import org.onosproject.cord.gui.model.BundleDescriptor;
import org.onosproject.cord.gui.model.BundleFactory;
import org.onosproject.cord.gui.model.JsonFactory;
import org.onosproject.cord.gui.model.SubscriberUser;
import org.onosproject.cord.gui.model.UserFactory;
import org.onosproject.cord.gui.model.XosFunction;
import org.onosproject.cord.gui.model.XosFunctionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.cord.gui.model.XosFunctionDescriptor.URL_FILTER;

/**
 * In memory cache of the model of the subscriber's account.
 */
public class CordModelCache extends JsonFactory {

    private static final String KEY_SSID_MAP = "ssidmap";
    private static final String KEY_SSID = "service_specific_id";
    private static final String KEY_SUB_ID = "subscriber_id";

    private static final int DEMO_SSID = 1234;

    private static final String EMAIL_0 = "john@smith.org";
    private static final String EMAIL_1 = "john@doe.org";

    private static final String EMAIL = "email";
    private static final String SSID = "ssid";
    private static final String SUB_ID = "subId";

    private static final String BUNDLE = "bundle";
    private static final String USERS = "users";
    private static final String LEVEL = "level";
    private static final String LOGOUT = "logout";

    private static final String BUNDLE_NAME = BUNDLE + "_name";
    private static final String BUNDLE_DESC = BUNDLE + "_desc";

    private static final Map<Integer, Integer> LOOKUP = new HashMap<>();

    private String email = null;
    private int subscriberId;
    private int ssid;
    private Bundle currentBundle;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // NOTE: use a tree map to maintain sorted order by user ID
    private final Map<Integer, SubscriberUser> userMap =
            new TreeMap<Integer, SubscriberUser>();

    /**
     * Constructs a model cache, retrieving a mapping of SSID to XOS Subscriber
     * IDs from the XOS server.
     */
    CordModelCache() {
        log.info("Initialize model cache");
        ObjectNode map = XosManager.INSTANCE.initXosSubscriberLookups();
        initLookupMap(map);
        log.info("{} entries in SSID->SubID lookup map", LOOKUP.size());
    }

    private void initLookupMap(ObjectNode map) {
        ArrayNode array = (ArrayNode) map.get(KEY_SSID_MAP);
        Iterator<JsonNode> iter = array.elements();
        StringBuilder msg = new StringBuilder();
        while (iter.hasNext()) {
            ObjectNode node = (ObjectNode) iter.next();
            String ssidStr = node.get(KEY_SSID).asText();
            int ssid = Integer.valueOf(ssidStr);
            int subId = node.get(KEY_SUB_ID).asInt();
            LOOKUP.put(ssid, subId);
            msg.append(String.format("\n..binding SSID %s to sub-id %s", ssid, subId));
        }
        log.info(msg.toString());
    }

    private int lookupSubId(int ssid) {
        Integer subId = LOOKUP.get(ssid);
        if (subId == null) {
            log.error("Unmapped SSID: {}", ssid);
            return 0;
        }
        return subId;
    }

    /**
     * Initializes the model for the subscriber account associated with
     * the given email address.
     *
     * @param email the email address
     */
    void init(String email) {
        // defaults to the demo account
        int ssid = DEMO_SSID;

        this.email = email;

        // obviously not scalable, but good enough for demo code...
        if (EMAIL_0.equals(email)) {
            ssid = 0;
        } else if (EMAIL_1.equals(email)) {
            ssid = 1;
        }

        this.ssid = ssid;
        subscriberId = lookupSubId(ssid);
        XosManager.INSTANCE.setXosUtilsForSubscriber(subscriberId);

        // call the initdemo API to ensure users are populated in XOS
        XosManager.INSTANCE.initDemoSubscriber();

        // NOTE: I think the following should work for non-DEMO account...
        currentBundle = new Bundle(BundleFactory.BASIC_BUNDLE);
        initUsers();
    }

    private void initUsers() {
        // start with a clean slate
        userMap.clear();

        ArrayNode users = XosManager.INSTANCE.getUserList();
        if (users == null) {
            log.warn("no user list for SSID {} (subid {})", ssid, subscriberId);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (JsonNode u: users) {
            ObjectNode user = (ObjectNode) u;

            int id = user.get("id").asInt();
            String name = user.get("name").asText();
            String mac = user.get("mac").asText();
            String level = user.get("level").asText();

            // NOTE: We are just storing the current "url-filter" level.
            //       Since we are starting with the BASIC bundle, (that does
            //       not include URL_FILTER), we don't yet have the URL_FILTER
            //       memento in which to store the level.
            SubscriberUser su = createUser(id, name, mac, level);
            userMap.put(id, su);
            sb.append(String.format("\n..cache user %s [%d], %s, %s",
                                    name, id, mac, level));
        }
        log.info(sb.toString());
    }

    private SubscriberUser createUser(int uid, String name, String mac,
                                      String level) {
        SubscriberUser user = new SubscriberUser(uid, name, mac, level);
        for (XosFunction f: currentBundle.functions()) {
            user.setMemento(f.descriptor(), f.createMemento());
        }
        return user;
    }

    /**
     * Returns the currently selected bundle.
     *
     * @return current bundle
     */
    public Bundle getCurrentBundle() {
        return currentBundle;
    }

    /**
     * Sets a new bundle.
     *
     * @param bundleId bundle identifier
     * @throws IllegalArgumentException if bundle ID is unknown
     */
    public void setCurrentBundle(String bundleId) {
        log.info("set new bundle : {}", bundleId);
        BundleDescriptor bd = BundleFactory.bundleFromId(bundleId);
        currentBundle = new Bundle(bd);
        // update the user mementos
        for (SubscriberUser user: userMap.values()) {
            user.clearMementos();
            for (XosFunction f: currentBundle.functions()) {
                user.setMemento(f.descriptor(), f.createMemento());
                if (f.descriptor().equals(URL_FILTER)) {
                    applyUrlFilterLevel(user, user.urlFilterLevel());
                }
            }
        }

        XosManager.INSTANCE.setNewBundle(currentBundle);
    }


    /**
     * Returns the list of current users for this subscriber account.
     *
     * @return the list of users
     */
    public List<SubscriberUser> getUsers() {
        return ImmutableList.copyOf(userMap.values());
    }

    /**
     * Applies a function parameter change for a user, pushing that
     * change through to XOS.
     *
     * @param userId user identifier
     * @param funcId function identifier
     * @param param function parameter to change
     * @param value new value for function parameter
     */
    public void applyPerUserParam(String userId, String funcId,
                                  String param, String value) {

        int uid = Integer.parseInt(userId);
        SubscriberUser user = userMap.get(uid);
        checkNotNull(user, "unknown user id: " + uid);

        XosFunctionDescriptor xfd =
                XosFunctionDescriptor.valueOf(funcId.toUpperCase());

        XosFunction func = currentBundle.findFunction(xfd);
        checkNotNull(func, "function not part of bundle: " + funcId);
        applyParam(func, user, param, value, true);
    }

    // =============

    private void applyUrlFilterLevel(SubscriberUser user, String level) {
        XosFunction urlFilter = currentBundle.findFunction(URL_FILTER);
        if (urlFilter != null) {
            applyParam(urlFilter, user, LEVEL, level, false);
        }
    }

    private void applyParam(XosFunction func, SubscriberUser user,
                            String param, String value, boolean punchThrough) {
        func.applyParam(user, param, value);
        if (punchThrough) {
            XosManager.INSTANCE.apply(func, user);
        }
    }

    private ArrayNode userJsonArray() {
        ArrayNode userList = arrayNode();
        for (SubscriberUser user: userMap.values()) {
            userList.add(UserFactory.toObjectNode(user));
        }
        return userList;
    }

    // ============= generate JSON for GUI rest calls..

    private void addSubId(ObjectNode root) {
        root.put(SUB_ID, subscriberId);
        root.put(SSID, ssid);
        root.put(EMAIL, email);
    }


    /**
     * Returns response JSON for login request.
     * <p>
     * Depending on which email is used, will bind the GUI to the
     * appropriate XOS Subscriber ID.
     *
     * @param email the supplied email
     * @return JSON acknowledgement
     */
    public synchronized String jsonLogin(String email) {
        log.info("jsonLogin(\"{}\")", email);
        init(email);
        ObjectNode root = objectNode();
        addSubId(root);
        return root.toString();
    }

    /**
     * Returns the dashboard page data as JSON.
     *
     * @return dashboard page JSON data
     */
    public synchronized String jsonDashboard() {
        log.info("jsonDashboard()");

        if (email == null) {
            return jsonLogout();
        }

        BundleDescriptor bundleDescriptor = currentBundle.descriptor();
        ObjectNode root = objectNode();
        root.put(BUNDLE_NAME, bundleDescriptor.displayName());
        root.put(BUNDLE_DESC, bundleDescriptor.description());
        root.set(USERS, userJsonArray());
        addSubId(root);
        return root.toString();
    }

    /**
     * Returns the bundle page data as JSON.
     *
     * @return bundle page JSON data
     */
    public synchronized String jsonBundle() {
        log.info("jsonBundle()");

        if (email == null) {
            return jsonLogout();
        }

        ObjectNode root = BundleFactory.toObjectNode(currentBundle);
        addSubId(root);
        return root.toString();
    }

    /**
     * Returns the users page data as JSON.
     *
     * @return users page JSON data
     */
    public synchronized String jsonUsers() {
        log.info("jsonUsers()");

        if (email == null) {
            return jsonLogout();
        }

        ObjectNode root = objectNode();
        root.set(USERS, userJsonArray());
        addSubId(root);
        return root.toString();
    }

    /**
     * Returns logout acknowledgement as JSON.
     *
     * @return logout acknowledgement
     */
    public synchronized String jsonLogout() {
        log.info("jsonLogout()");
        ObjectNode root = objectNode().put(LOGOUT, true);
        addSubId(root);

        email = null;   // signifies no one logged in

        return root.toString();
    }

    /**
     * Singleton instance.
     */
    public static final CordModelCache INSTANCE = new CordModelCache();
}
