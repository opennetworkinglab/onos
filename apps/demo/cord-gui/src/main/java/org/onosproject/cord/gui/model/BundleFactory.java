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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Utility factory for creating and/or operating on bundles.
 */
public class BundleFactory extends JsonFactory {

    private static final String BUNDLE = "bundle";
    private static final String BUNDLES = "bundles";
    private static final String FUNCTIONS = "functions";

    private static final String BASIC_ID = "basic";
    private static final String BASIC_DISPLAY_NAME = "Basic Bundle";
    private static final String BASIC_DESCRIPTION =
            "If the thing that matters most to you is high speed Internet" +
                    " connectivity delivered at a great price, then the basic" +
                    " bundle is right for you.\n" +
                    "Starting at $30 a month for 12 months.";

    private static final String FAMILY_ID = "family";
    private static final String FAMILY_DISPLAY_NAME = "Family Bundle";
    private static final String FAMILY_DESCRIPTION =
            "Enjoy great entertainment, peace of mind and big savings when " +
                    "you bundle high speed Internet and Firewall with" +
                    " Parental Control.\n" +
                    "Starting at $40 a month for 12 months.";


    // no instantiation
    private BundleFactory() {}

    /**
     * Designates the BASIC bundle.
     */
    public static final BundleDescriptor BASIC_BUNDLE =
            new DefaultBundleDescriptor(BASIC_ID, BASIC_DISPLAY_NAME,
                                        BASIC_DESCRIPTION,
                                        XosFunctionDescriptor.INTERNET,
                                        XosFunctionDescriptor.FIREWALL,
                                        XosFunctionDescriptor.CDN);

    /**
     * Designates the FAMILY bundle.
     */
    public static final BundleDescriptor FAMILY_BUNDLE =
            new DefaultBundleDescriptor(FAMILY_ID, FAMILY_DISPLAY_NAME,
                                        FAMILY_DESCRIPTION,
                                        XosFunctionDescriptor.INTERNET,
                                        XosFunctionDescriptor.FIREWALL,
                                        XosFunctionDescriptor.CDN,
                                        XosFunctionDescriptor.URL_FILTER);

    // all bundles, in the order they should be listed in the GUI
    private static final List<BundleDescriptor> ALL_BUNDLES = ImmutableList.of(
            BASIC_BUNDLE,
            FAMILY_BUNDLE
    );

    /**
     * Returns the list of available bundles.
     *
     * @return available bundles
     */
    public static List<BundleDescriptor> availableBundles() {
        return ALL_BUNDLES;
    }

    /**
     * Returns the bundle descriptor for the given identifier.
     *
     * @param bundleId bundle identifier
     * @return bundle descriptor
     * @throws IllegalArgumentException if bundle ID is unknown
     */
    public static BundleDescriptor bundleFromId(String bundleId) {
        for (BundleDescriptor bd : ALL_BUNDLES) {
            if (bd.id().equals(bundleId)) {
                return bd;
            }
        }
        throw new IllegalArgumentException("unknown bundle: " + bundleId);
    }

    /**
     * Returns an object node representation of the given bundle.
     * Note that some functions (such as CDN) are not added to the output
     * as we don't want them to appear in the GUI.
     *
     * @param bundle the bundle
     * @return object node
     */
    public static ObjectNode toObjectNode(Bundle bundle) {
        ObjectNode root = objectNode();
        BundleDescriptor descriptor = bundle.descriptor();

        ObjectNode bnode = objectNode()
                .put(ID, descriptor.id())
                .put(NAME, descriptor.displayName())
                .put(DESC, descriptor.description());

        ArrayNode funcs = arrayNode();
        for (XosFunctionDescriptor xfd: bundle.descriptor().functions()) {
            if (xfd.visible()) {
                funcs.add(XosFunctionFactory.toObjectNode(xfd));
            }
        }
        bnode.set(FUNCTIONS, funcs);
        root.set(BUNDLE, bnode);

        ArrayNode bundles = arrayNode();
        for (BundleDescriptor bd: BundleFactory.availableBundles()) {
            ObjectNode bdnode = objectNode()
                    .put(ID, bd.id())
                    .put(NAME, bd.displayName())
                    .put(DESC, bd.description());
            bundles.add(bdnode);
        }
        root.set(BUNDLES, bundles);
        return root;
    }
}
