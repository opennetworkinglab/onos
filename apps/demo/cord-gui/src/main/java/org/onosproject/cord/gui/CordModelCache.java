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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.cord.gui.model.XosFunctionDescriptor.URL_FILTER;

/**
 * In memory cache of the model of the subscriber's account.
 */
public class CordModelCache extends JsonFactory {

    private static final String BUNDLE = "bundle";
    private static final String USERS = "users";
    private static final String SUB_ID = "subId";
    private static final String LEVEL = "level";

    private int subscriberId;
    private Bundle currentBundle;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // NOTE: use a tree map to maintain sorted order by user ID
    private final Map<Integer, SubscriberUser> userMap =
            new TreeMap<Integer, SubscriberUser>();

    /**
     * Constructs a model cache, (retrieving demo subscriber ID),
     * initializing it with basic bundle, and fetching the list of users.
     */
    CordModelCache() {
        log.info("Initialize model cache");
        subscriberId = XosManager.INSTANCE.initXosSubscriber();
        currentBundle = new Bundle(BundleFactory.BASIC_BUNDLE);
        initUsers();
    }

    private void initUsers() {
        ArrayNode users = XosManager.INSTANCE.getUserList();
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
            log.info("..caching user {} (id:{})", name, id);
        }
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
    }

    /**
     * Returns the dashboard page data as JSON.
     *
     * @return dashboard page JSON data
     */
    public String jsonDashboard() {
        ObjectNode root = objectNode();
        root.put(BUNDLE, currentBundle.descriptor().displayName());
        root.set(USERS, userJsonArray());
        addSubId(root);
        return root.toString();
    }

    /**
     * Returns the bundle page data as JSON.
     *
     * @return bundle page JSON data
     */
    public String jsonBundle() {
        ObjectNode root = BundleFactory.toObjectNode(currentBundle);
        addSubId(root);
        return root.toString();
    }

    /**
     * Returns the users page data as JSON.
     *
     * @return users page JSON data
     */
    public String jsonUsers() {
        ObjectNode root = objectNode();
        root.set(USERS, userJsonArray());
        addSubId(root);
        return root.toString();
    }

    /**
     * Singleton instance.
     */
    public static final CordModelCache INSTANCE = new CordModelCache();
}
