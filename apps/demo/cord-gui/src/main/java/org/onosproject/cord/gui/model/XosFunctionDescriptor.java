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
             "Discover the joys of high-speed, reliable Internet" +
                     " connectivity delivered seamlessly to your home.",
             false,
             true),

    /**
     * Firewall function.
     */
    FIREWALL("firewall",
             "Firewall",
             "Simple access control and filtering with minimal set-up.",
             true,
             true),

    /**
     * URL Filtering function (parental controls).
     */
    URL_FILTER("url_filter",
               "Parental Control",
               "Parental Control is peace of mind that your kids are safe" +
                       " - whether you are around or away. Indicate with a " +
                       "few clicks what online content is appropriate for " +
                       "your children, and voila - you have control over" +
                       " what your kids can and cannot view.",
               true,
               true),

    /**
     * Content Distribution function.
     */
    CDN("cdn",
        "CDN",
        "Content Distribution Network service.",
        true,
        false);


    private final String id;
    private final String displayName;
    private final String description;
    private final boolean backend;
    private final boolean visible;

    XosFunctionDescriptor(String id, String displayName, String description,
                          boolean backend, boolean visible) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.backend = backend;
        this.visible = visible;
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

    /**
     * Returns true if this function is supported by the XOS backend.
     *
     * @return true if backend function exists
     */
    public boolean backend() {
        return backend;
    }

    /**
     * Returns true if this function should be shown in the GUI, in the
     * bundle listing.
     *
     * @return true if to be displayed
     */
    public boolean visible() {
        return visible;
    }
}
