/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.ui.lion.stitch;

import org.onosproject.ui.lion.LionBundle;
import org.onosproject.ui.lion.LionUtils;

import java.util.ResourceBundle;
import java.util.Set;

/**
 * Gathers and stitches together a localization bundle according to a
 * "lion" configuration file.
 */
public class BundleStitcher {

    private static final String CONFIG_DIR = "_config";
    private static final String SUFFIX = ".lioncfg";
    private static final String SLASH = "/";
    private static final String DOT = ".";


    private final String base;

    /**
     * Creates a bundle stitcher, configured with the specified base resource
     * path.
     *
     * @param base the base resource path
     */
    public BundleStitcher(String base) {
        this.base = base;
    }

    @Override
    public String toString() {
        return "BundleStitcher{base=\"" + base + "\"}";
    }

    /**
     * Stitches together a LionBundle, based on the bundle configuration data
     * for the given bundle ID.
     *
     * @param id the bundle ID
     * @return a corresponding lion bundle
     * @throws IllegalArgumentException if the bundle config cannot be loaded
     */
    public LionBundle stitch(String id) {
        String source = base + SLASH + CONFIG_DIR + SLASH + id + SUFFIX;
        LionConfig cfg = new LionConfig().load(source);
        LionBundle.Builder builder = new LionBundle.Builder(id);

        for (LionConfig.CmdFrom from : cfg.entries()) {
            addItemsToBuilder(builder, from);
        }

        return builder.build();
    }

    private void addItemsToBuilder(LionBundle.Builder builder,
                                   LionConfig.CmdFrom from) {
        String resBundleName = base + SLASH + from.res();
        String resFqbn = convertToFqbn(resBundleName);
        ResourceBundle bundle = LionUtils.getBundledResource(resFqbn);

        if (from.starred()) {
            addAllItems(builder, bundle);
        } else {
            addItems(builder, bundle, from.keys());
        }
    }

    // to fully-qualified-bundle-name
    private String convertToFqbn(String path) {
        if (!path.startsWith(SLASH)) {
            throw new IllegalArgumentException("path should start with '/'");
        }
        return path.substring(1).replaceAll(SLASH, DOT);
    }

    private void addAllItems(LionBundle.Builder builder, ResourceBundle bundle) {
        addItems(builder, bundle, bundle.keySet());
    }

    private void addItems(LionBundle.Builder builder, ResourceBundle bundle,
                          Set<String> keys) {
        keys.forEach(k -> builder.addItem(k, bundle.getString(k)));
    }
}
