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

package org.onosproject.ui.lion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Utility methods for dealing with Localization Bundles etc.
 */
public final class LionUtils {

    private static final Logger log = LoggerFactory.getLogger(LionUtils.class);

    private static final String USER_LANGUAGE = "user.language";
    private static final String USER_COUNTRY = "user.country";
    private static final String ONOS_LOCALE = "ONOS_LOCALE";
    private static final String EMPTY = "";
    private static final String DOT = ".";
    private static final String LOBAR = "_";

    // no instantiation
    private LionUtils() {
    }

    /**
     * Parses the given string into language and country codes, and returns
     * a {@link Locale} instance initialized with those parameters.
     * For example:
     * <pre>
     *     Locale locale = LionUtils.localeFromString("en_GB");
     *     locale.getLanguage();   // "en"
     *     locale.getCountry();    // "GB"
     *
     *     locale = LionUtils.localeFromString("ru");
     *     locale.getLanguage();   // "ru"
     *     locale.getCountry();    // ""
     * </pre>
     *
     * @param s the locale string
     * @return a locale instance
     */
    public static Locale localeFromString(String s) {

        if (!s.contains(LOBAR)) {
            return new Locale(s);
        }
        String[] items = s.split(LOBAR);
        return new Locale(items[0], items[1]);
    }

    /**
     * Sets the default locale, based on the Java properties shown below.
     * <pre>
     *   user.language
     *   user.country
     * </pre>
     * It is expected that the host system will have set these properties
     * appropriately. Note, however, that the default values can be
     * overridden by use of the environment variable {@code ONOS_LOCALE}.
     * <p>
     * For example, to set the Locale to French-Canadian one can invoke
     * (from the shell)...
     * <pre>
     * $ ONOS_LOCALE=fr_CA {command-to-invoke-onos} ...
     * </pre>
     *
     * @return the runtime locale
     */
    public static Locale setupRuntimeLocale() {
        Locale systemDefault = Locale.getDefault();
        log.info("System Default Locale: [{}]", systemDefault);
        // TODO: Review- do we need to store the system default anywhere?

        // Useful to log the "user.*" properties for debugging...
        Set<String> pn = System.getProperties().stringPropertyNames();
        pn.removeIf(f -> !(f.startsWith("user.")));
        for (String ukey : pn) {
            log.debug("  {}: {}", ukey, System.getProperty(ukey));
        }

        String language = System.getProperty(USER_LANGUAGE);
        String country = System.getProperty(USER_COUNTRY);
        log.info("Language: [{}], Country: [{}]", language, country);
        Locale runtime = new Locale(language != null ? language : EMPTY,
                                    country != null ? country : EMPTY);

        String override = System.getenv(ONOS_LOCALE);
        if (override != null) {
            log.warn("Override with ONOS_LOCALE: [{}]", override);
            runtime = localeFromString(override);
        }

        log.info("Setting runtime locale to: [{}]", runtime);
        Locale.setDefault(runtime);
        return runtime;
    }

    /**
     * This method takes a fully qualified name and returns a
     * {@link ResourceBundle} which is loaded from a properties file with
     * that base name.
     * <p>
     * For example, supposing the jar file contains:
     * <pre>
     * org/onosproject/util/example/SomeBundle.properties
     * </pre>
     * <p>
     * Then, to correctly load the resource bundle associated with
     * <code>SomeBundle</code>, call:
     * <pre>
     * String fqname = "org.onosproject.util.example.SomeBundle";
     * ResourceBundle res = ResourceUtils.getBundledResource(fqname);
     * </pre>
     * <p>
     * Note that no error is thrown if the properties file does not exist.
     * This condition will not become apparent until you try and access
     * a property from the bundle, at which time a
     * {@link java.util.MissingResourceException} will be thrown.
     *
     * @param basename the (fully qualified) basename of the bundle properties
     *                 file
     * @return the associated resource bundle
     */
    public static ResourceBundle getBundledResource(String basename) {
        return ResourceBundle.getBundle(basename);
    }

    /**
     * This method takes a class and returns a {@link ResourceBundle} which is
     * loaded from a properties file with the same base name as the class.
     * Note that both the class and the properties file(s) need to be in
     * the same jar file.
     * <p>
     * For example, supposing the jar file contains:
     * <pre>
     * org/onosproject/util/example/SomeObject.class
     * org/onosproject/util/example/SomeObject.properties
     * </pre>
     * <p>
     * Then, to correctly load the resource bundle associated with
     * <code>SomeObject</code>, call:
     * <pre>
     * ResourceBundle res = ResourceUtils.getBundledResource(SomeObject.class);
     * </pre>
     * <p>
     * Note that no error is thrown if the properties file does not exist.
     * This condition will not become apparent until you try and access
     * a property from the bundle, at which time a
     * {@link java.util.MissingResourceException} will be thrown.
     *
     * @param c the class
     * @return the associated resource bundle
     */
    public static ResourceBundle getBundledResource(Class<?> c) {
        return ResourceBundle.getBundle(c.getName());
    }

    /**
     * This method returns a {@link ResourceBundle} which is loaded from
     * a properties file with the specified base name from the same package
     * as the specified class.
     * Note that both the class and the properties file(s) need to be in
     * the same jar file.
     * <p>
     * For example, supposing the jar file contains:
     * <pre>
     * org/onosproject/util/example/SomeObject.class
     * org/onosproject/util/example/DisplayStrings.properties
     * </pre>
     * <p>
     * Then, to correctly load the resource bundle call:
     * <pre>
     * ResourceBundle res = ResourceUtils.getBundledResource(SomeObject.class,
     *                                                       "DisplayStrings");
     * </pre>
     * <p>
     * Note that no error is thrown if the properties file does not exist.
     * This condition will not become apparent until you try and access
     * a property from the bundle, at which time a
     * {@link java.util.MissingResourceException} will be thrown.
     *
     * @param c        the class requesting the bundle
     * @param baseName the base name of the resource bundle
     * @return the associated resource bundle
     */
    public static ResourceBundle getBundledResource(Class<?> c, String baseName) {
        String className = c.getName();
        StringBuilder sb = new StringBuilder();
        int dot = className.lastIndexOf(DOT);
        sb.append(className.substring(0, dot));
        sb.append(DOT).append(baseName);
        return ResourceBundle.getBundle(sb.toString());
    }
}
