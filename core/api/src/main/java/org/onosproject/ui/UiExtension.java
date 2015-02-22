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
package org.onosproject.ui;

import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User interface extension.
 */
public class UiExtension {

    private final String prefix;
    private final ClassLoader classLoader;
    private final List<UiView> views;

    /**
     * Creates a user interface extension for loading CSS and JS injections
     * from {@code css.html} and {@code js.html} resources, respectively.
     *
     * @param views       list of contributed views
     * @param classLoader class-loader for user interface resources
     */
    public UiExtension(List<UiView> views, ClassLoader classLoader) {
        this(views, null, classLoader);
    }

    /**
     * Creates a user interface extension using custom resource prefix. It
     * loads CSS and JS injections from {@code path/css.html} and
     * {@code prefix/js.html} resources, respectively.
     *
     * @param views       list of user interface views
     * @param path        resource path prefix
     * @param classLoader class-loader for user interface resources
     */
    public UiExtension(List<UiView> views, String path, ClassLoader classLoader) {
        this.views = checkNotNull(ImmutableList.copyOf(views), "Views cannot be null");
        this.prefix = path != null ? (path + "/") : "";
        this.classLoader = checkNotNull(classLoader, "Class loader must be specified");
    }

    /**
     * Returns input stream containing CSS inclusion statements.
     *
     * @return CSS inclusion statements
     */
    public InputStream css() {
        return classLoader.getResourceAsStream(prefix + "css.html");
    }

    /**
     * Returns input stream containing JavaScript inclusion statements.
     *
     * @return JavaScript inclusion statements
     */
    public InputStream js() {
        return classLoader.getResourceAsStream(prefix + "js.html");
    }

    /**
     * Returns list of user interface views contributed by this extension.
     *
     * @return contributed view descriptors
     */
    public List<UiView> views() {
        return views;
    }

    /**
     * Returns input stream containing specified view-specific resource.
     *
     * @param viewId view identifier
     * @param path   resource path, relative to the view directory
     * @return resource input stream
     */
    public InputStream resource(String viewId, String path) {
        InputStream is = classLoader.getResourceAsStream(prefix + "views/" + viewId + "/" + path);
        return is;
    }

}
