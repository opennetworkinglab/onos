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

import java.util.Set;

/**
 * Encapsulation of interactions with XOS.
 */
public class XosManager {

    private static final String XOS_HOST = "10.254.1.22";
    private static final String XOS_PORT = "8000";

    private static final String URL_FMT = "http://%s:%s/xoslib/rs/subscriber/";

    private static final String BASE_URL =
            String.format(URL_FMT, XOS_HOST, XOS_PORT);


    /**
     * No instantiation (except via unit test).
     */
    XosManager() {}

    /**
     * Configure XOS to enable the functions that compose the given bundle,
     * and disable all the others, for the given subscriber.
     *
     * @param subscriberId subscriber identifier
     * @param bundle new bundle to set
     */
    public void setNewBundle(int subscriberId, Bundle bundle) {
        System.out.println("\n>> Set New Bundle : " + bundle.descriptor().id());

        String urlFmt = xosUrl(subscriberId) + "services/%s/%s";
        Set<XosFunctionDescriptor> inBundle = bundle.descriptor().functions();
        for (XosFunctionDescriptor xfd: XosFunctionDescriptor.values()) {
            xosEnableFunction(urlFmt, xfd, inBundle.contains(xfd));
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
        System.out.println("\n>> Apply : " + func + " for " + user);

        String urlPrefix = xosUrl(subscriberId) + "users/" + user.id() + "/";
        String url = urlPrefix + func.xosUrlApply(user);
        restPut(url);
    }


    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    private String xosUrl(int subscriberId) {
        return BASE_URL + String.format("%d/", subscriberId);
    }

    private void xosEnableFunction(String urlFmt, XosFunctionDescriptor xfd,
                                   boolean enable) {
        String url = String.format(urlFmt, xfd.id(), enable);
        restPut(url);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    private void restPut(String url) {
        // TODO: wire up to Jackson client...
        System.out.println("<<PUT>> " + url);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    /**
     * Singleton instance.
     */
    public static final XosManager INSTANCE = new XosManager();
}
