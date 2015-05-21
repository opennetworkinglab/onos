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

/**
 * Designates XOS Functions.
 */
public enum XosFunctionDescriptor {
    /**
     * Internet function.
     */
    INTERNET("internet",
             "Internet",
             "Basic internet connectivity."),

    /**
     * Firewall function.
     */
    FIREWALL("firewall",
             "Firewall",
             "Normal firewall protection."),

    /**
     * URL Filtering function (parental controls).
     */
    URL_FILTERING("url_filtering",
                  "Parental Control",
                  "Variable levels of URL filtering.");

    private final String id;
    private final String displayName;
    private final String description;

    XosFunctionDescriptor(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns this function's internal identifier.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns this function's display name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns a short, textual description of the function.
     *
     * @return textual description
     */
    public String description() {
        return description;
    }

}
