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

import java.util.ResourceBundle;

/**
 * Utility methods for dealing with Localization Bundles etc.
 */
public final class LionUtils {

    private static final String DOT = ".";

    // no instantiation
    private LionUtils() {
    }

    /**
     * This method takes a fully qualified name and returns a
     * {@link ResourceBundle} which is loaded from a properties file with
     * that base name.
     * <p>
     * For example, supposing the jar file contains:
     * <p>
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
     * <p>
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
     * <p>
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
