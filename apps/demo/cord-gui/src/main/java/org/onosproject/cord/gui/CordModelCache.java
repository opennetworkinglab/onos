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

import com.google.common.collect.ImmutableList;
import org.onosproject.cord.gui.model.Bundle;
import org.onosproject.cord.gui.model.BundleDescriptor;
import org.onosproject.cord.gui.model.BundleFactory;
import org.onosproject.cord.gui.model.SubscriberUser;

import java.util.ArrayList;
import java.util.List;

/**
 * In memory cache of the model of the subscriber's account.
 */
public class CordModelCache {

    // faked for the demo
    private static final int SUBSCRIBER_ID = 92;
    private static final String MAC_1 = "010203040506";
    private static final String MAC_2 = "010203040507";
    private static final String MAC_3 = "010203040508";
    private static final String MAC_4 = "010203040509";

    private Bundle currentBundle;
    private final List<SubscriberUser> users;

    /**
     * Constructs a model cache, initializing it with basic bundle.
     */
    public CordModelCache() {
        currentBundle = new Bundle(BundleFactory.BASIC_BUNDLE);
        users = new ArrayList<SubscriberUser>();
        initUsers();
    }

    /**
     * Used to initialize users for the demo. These are currently fake.
     */
    public void initUsers() {
        users.add(new SubscriberUser(1, "Mom's MacBook", MAC_1));
        users.add(new SubscriberUser(2, "Dad's iPad", MAC_2));
        users.add(new SubscriberUser(3, "Dick's laptop", MAC_3));
        users.add(new SubscriberUser(4, "Jane's laptop", MAC_4));
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
        BundleDescriptor bdesc = BundleFactory.bundleFromId(bundleId);
        currentBundle = new Bundle(bdesc);
    }

    /**
     * Returns the list of current users for this subscriber account.
     *
     * @return the list of users
     */
    public List<SubscriberUser> getUsers() {
        return ImmutableList.copyOf(users);
    }
}
