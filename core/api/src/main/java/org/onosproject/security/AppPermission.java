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
 */

package org.onosproject.security;

import java.security.BasicPermission;

/**
 * Implementation of API access permission.
 */
public class AppPermission extends BasicPermission {

    /**
     * Creates new application permission using the supplied data.
     * @param name permission name
     */
    public AppPermission(String name) {
        super(name.toUpperCase(), "");
    }

    /**
     * Creates new application permission using the supplied data.
     * @param name permission name
     * @param actions permission action
     */
    public AppPermission(String name, String actions) {
        super(name.toUpperCase(), actions);
    }

}
