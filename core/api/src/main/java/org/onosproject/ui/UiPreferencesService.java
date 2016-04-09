/*
 * Copyright 2016-present Open Networking Laboratory
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
     * Sets the named preference for the specified user.
     *
     * @param userName   user name
     * @param preference name of the user preference
     * @param value      preference value
     */
    void setPreference(String userName, String preference, ObjectNode value);

}
