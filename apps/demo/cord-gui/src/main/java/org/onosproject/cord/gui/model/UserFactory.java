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

package org.onosproject.cord.gui.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions on users.
 */
public class UserFactory extends JsonFactory {

    private static final String MAC = "mac";
    private static final String PROFILE = "profile";


    // hard-coded icons for the demo
    private static final Map<String, String> ICON_LOOKUP =
            new HashMap<String, String>();
    static {
        ICON_LOOKUP.put("Mom's PC", "mom");
        ICON_LOOKUP.put("Dad's PC", "dad");
        ICON_LOOKUP.put("Jack's Laptop", "boy2");
        ICON_LOOKUP.put("Jill's Laptop", "girl1");
    }

    private static final String DEFAULT_ICON_ID = "boy1";

    // no instantiation
    private UserFactory() {}

    /**
     * Returns an object node representation of the given user.
     *
     * @param user the user
     * @return object node
     */
    public static ObjectNode toObjectNode(SubscriberUser user) {
        String icon = ICON_LOOKUP.get(user.name());
        icon = icon == null ? DEFAULT_ICON_ID : icon;

        ObjectNode root = objectNode()
                .put(ID, user.id())
                .put(ICON_ID, icon)
                .put(NAME, user.name())
                .put(MAC, user.mac());
        root.set(PROFILE, XosFunctionFactory.profileForUser(user));
        return root;
    }

}
