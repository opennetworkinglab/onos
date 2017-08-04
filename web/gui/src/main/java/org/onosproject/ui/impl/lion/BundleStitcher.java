/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ui.impl.lion;

import com.google.common.collect.ImmutableList;
import org.onosproject.ui.lion.LionBundle;
import org.onosproject.ui.lion.LionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Gathers and stitches together a localization bundle according to a
 * "lion" configuration file.
 */
public class BundleStitcher {

    private static final Logger log =
            LoggerFactory.getLogger(BundleStitcher.class);

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
     * @throws IllegalArgumentException if the config cannot be loaded
     *                                  or the bundle cannot be stitched
     */
    public LionBundle stitch(String id) {
        String source = base + SLASH + CONFIG_DIR + SLASH + id + SUFFIX;

        // the following may throw IllegalArgumentException...
        LionConfig cfg = new LionConfig().load(source);

        LionBundle.Builder builder = new LionBundle.Builder(id);
        int total = 0;
        int added = 0;

        for (LionConfig.CmdFrom from : cfg.entries()) {
            total += 1;
            log.debug("  processing: {}", from.orig());
            if (addItemsToBuilder(builder, from)) {
                added += 1;
            }
        }
        log.debug("  added items for {}/{} FROM entries", added, total);
        return builder.build();
    }

    private boolean addItemsToBuilder(LionBundle.Builder builder,
                                      LionConfig.CmdFrom from) {
        String resBundleName = base + SLASH + from.res();
        String fqbn = convertToFqbn(resBundleName);
        ResourceBundle bundle = null;
        boolean ok = true;

        try {
            // NOTE: have to be explicit about the locale and class-loader
            //       for this to work under Karaf, apparently...
            Locale locale = Locale.getDefault();
            ClassLoader classLoader = getClass().getClassLoader();
            bundle = LionUtils.getBundledResource(fqbn, locale, classLoader);

        } catch (MissingResourceException e) {
            log.warn("Cannot find resource bundle: {}", fqbn);
            log.debug("BOOM!", e);
            ok = false;
        }

        if (ok) {
            Set<String> keys = from.starred() ? bundle.keySet() : from.keys();
            addItems(builder, bundle, keys);
            log.debug("  added {} item(s) from {}", keys.size(), from.res());
        }

        return ok;
    }

    // to fully-qualified-bundle-name
    private String convertToFqbn(String path) {
        if (!path.startsWith(SLASH)) {
            throw new IllegalArgumentException("path should start with '/'");
        }
        return path.substring(1).replaceAll(SLASH, DOT);
    }

    private void addItems(LionBundle.Builder builder, ResourceBundle bundle,
                          Set<String> keys) {
        keys.forEach(k -> builder.addItem(k, bundle.getString(k)));
    }

    /**
     * Generates an immutable list of localization bundles, using the specified
     * resource tree (base) and localization configuration file names (tags).
     * <p>
     * As an example, you might invoke:
     * <pre>
     * private static final String LION_BASE = "/org/onosproject/ui/lion";
     *
     * private static final String[] LION_TAGS = {
     *     "core.view.App",
     *     "core.view.Settings",
     *     "core.view.Cluster",
     *     "core.view.Processor",
     *     "core.view.Partition",
     * };
     *
     * List&lt;LionBundle&gt; bundles =
     *      LionUtils.generateBundles(LION_BASE, LION_TAGS);
     * </pre>
     * It is expected that in the "LION_BASE" directory there is a subdirectory
     * named "_config" which contains the configuration files listed in the
     * "LION_TAGS" array, each with a ".lioncfg" suffix...
     * <pre>
     * /org/onosproject/ui/lion/
     *   |
     *   +-- _config
     *         |
     *         +-- core.view.App.lioncfg
     *         +-- core.view.Settings.lioncfg
     *         :
     * </pre>
     * These files collate a localization bundle for their particular view
     * by referencing resource bundles and their keys.
     *
     * @param base the base resource directory path
     * @param tags the list of bundles to generate
     * @return a list of generated localization bundles
     */
    public static List<LionBundle> generateBundles(String base, String... tags) {
        List<LionBundle> bundles = new ArrayList<>(tags.length);
        BundleStitcher stitcher = new BundleStitcher(base);
        for (String tag : tags) {
            try {
                LionBundle lion = stitcher.stitch(tag);
                bundles.add(lion);
                log.info("Generated LION bundle: {}", lion);
                log.debug(" Dumped: {}", lion.dump());

            } catch (IllegalArgumentException e) {
                log.warn("Unable to generate bundle: {} / {}", base, tag);
                log.debug("BOOM!", e);
            }
        }
        return ImmutableList.copyOf(bundles);
    }
}
