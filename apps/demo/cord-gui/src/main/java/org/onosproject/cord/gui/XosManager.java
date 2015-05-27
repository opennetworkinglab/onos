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

import org.onosproject.cord.gui.model.Bundle;
import org.onosproject.cord.gui.model.SubscriberUser;
import org.onosproject.cord.gui.model.XosFunction;
import org.onosproject.cord.gui.model.XosFunctionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Encapsulation of interactions with XOS.
 */
public class XosManager {

    private static final String URI_BASE = "/rs/subscriber/";

    private final XosManagerRestUtils xosUtils = new XosManagerRestUtils(URI_BASE);
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * No instantiation (except via unit test).
     */
    XosManager() {}


    private String subId(int subscriberId) {
        return String.format("%d/", subscriberId);
    }

    /**
     * Configure XOS to enable the functions that compose the given bundle,
     * and disable all the others, for the given subscriber.
     *
     * @param subscriberId subscriber identifier
     * @param bundle new bundle to set
     */
    public void setNewBundle(int subscriberId, Bundle bundle) {
        log.info("\n>> Set New Bundle : " + bundle.descriptor().id());

        String uriFmt = subId(subscriberId) + "services/%s/%s";
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
     * @param subscriberId subscriber identifier
     * @param func specific XOS function
     * @param user user (containing function state)
     */
    public void apply(int subscriberId, XosFunction func, SubscriberUser user) {
        log.info("\n>> Apply : " + func + " for " + user);

        String uriPrefix = subId(subscriberId) + "users/" + user.id() + "/";
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
