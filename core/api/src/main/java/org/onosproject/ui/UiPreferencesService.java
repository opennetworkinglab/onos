/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Set;

/**
 * Service for tracking user interface preferences.
 */
public interface UiPreferencesService {

    /**
     * Returns the list of user names that have user preferences available.
     *
     * @return list of user names
     */
    Set<String> getUserNames();

    /**
     * Returns an immutable copy of the preferences for the specified user.
     *
     * @param userName user name
     * @return map of user preferences
     */
    Map<String, ObjectNode> getPreferences(String userName);

    /**
     * Returns the named preference for the specified user.
     * If no such preferences exist, null will be returned.
     *
     * @param username user name
     * @param key      preference key
     * @return named preference
     */
    ObjectNode getPreference(String username, String key);

    /**
     * Sets or clears the named preference for the specified user.
     *
     * @param username user name
     * @param key      preference key
     * @param value    preference value; if null it will be cleared
     */
    void setPreference(String username, String key, ObjectNode value);

}
