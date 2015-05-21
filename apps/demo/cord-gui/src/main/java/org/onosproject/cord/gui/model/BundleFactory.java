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

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Utility factory for creating bundles and functions etc.
 */
public class BundleFactory {

    private static final String BASIC_ID = "basic";
    private static final String BASIC_DISPLAY_NAME = "Basic Bundle";

    private static final String FAMILY_ID = "family";
    private static final String FAMILY_DISPLAY_NAME = "Family Bundle";

    // no instantiation
    private BundleFactory() {}

    private static final BundleDescriptor BASIC =
            new DefaultBundleDescriptor(BASIC_ID, BASIC_DISPLAY_NAME,
                                        XosFunctionDescriptor.INTERNET,
                                        XosFunctionDescriptor.FIREWALL);

    private static final BundleDescriptor FAMILY =
            new DefaultBundleDescriptor(FAMILY_ID, FAMILY_DISPLAY_NAME,
                                        XosFunctionDescriptor.INTERNET,
                                        XosFunctionDescriptor.FIREWALL,
                                        XosFunctionDescriptor.URL_FILTERING);

    /**
     * Returns the list of available bundles.
     *
     * @return available bundles
     */
    public static List<BundleDescriptor> availableBundles() {
        return ImmutableList.of(BASIC, FAMILY);
    }
}
