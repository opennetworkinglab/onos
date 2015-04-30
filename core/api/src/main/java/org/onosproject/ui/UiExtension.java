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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User interface extension.
 */
public class UiExtension {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VIEW_PREFIX = "app/view/";

    private final String prefix;
    private final ClassLoader classLoader;
    private final List<UiView> views;
    private final UiMessageHandlerFactory messageHandlerFactory;

    /**
     * Creates a user interface extension for loading CSS and JS injections
     * from {@code css.html} and {@code js.html} resources, respectively.
     *
     * @param views                 list of contributed views
     * @param messageHandlerFactory optional message handler factory
     * @param classLoader           class-loader for user interface resources
     */
    public UiExtension(List<UiView> views,
                       UiMessageHandlerFactory messageHandlerFactory,
                       ClassLoader classLoader) {
        this(views, messageHandlerFactory, null, classLoader);
    }

    /**
     * Creates a user interface extension using custom resource prefix. It
     * loads CSS and JS injections from {@code path/css.html} and
     * {@code prefix/js.html} resources, respectively.
     *
     * @param views                 list of user interface views
     * @param messageHandlerFactory optional message handler factory
     * @param path                  resource path prefix
     * @param classLoader           class-loader for user interface resources
     */
    public UiExtension(List<UiView> views,
                       UiMessageHandlerFactory messageHandlerFactory,
                       String path, ClassLoader classLoader) {
        this.views = checkNotNull(ImmutableList.copyOf(views), "Views cannot be null");
        this.messageHandlerFactory = messageHandlerFactory;
        this.prefix = path != null ? (path + "/") : "";
        this.classLoader = checkNotNull(classLoader, "Class loader must be specified");
    }

    /**
     * Returns input stream containing CSS inclusion statements.
     *
     * @return CSS inclusion statements
     */
    public InputStream css() {
        return getStream(prefix + "css.html");
    }

    /**
     * Returns input stream containing JavaScript inclusion statements.
     *
     * @return JavaScript inclusion statements
     */
    public InputStream js() {
       return getStream(prefix + "js.html");
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
        return getStream(VIEW_PREFIX + viewId + "/" + path);
    }

    /**
     * Returns message handler factory.
     *
     * @return message handlers
     */
    public UiMessageHandlerFactory messageHandlerFactory() {
        return messageHandlerFactory;
    }

    // Returns the resource input stream from the specified class-loader.
    private InputStream getStream(String path) {
        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            log.warn("Unable to find resource {}", path);
        }
        return stream;
    }


}
