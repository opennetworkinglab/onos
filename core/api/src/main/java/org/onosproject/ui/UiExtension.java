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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User interface extension.
 */
public final class UiExtension {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VIEW_PREFIX = "app/view/";
    private static final String EMPTY = "";
    private static final String SLASH = "/";
    private static final String CSS_HTML = "css.html";
    private static final String JS_HTML = "js.html";

    private final ClassLoader classLoader;
    private final String resourcePath;
    private final List<UiView> views;
    private final UiMessageHandlerFactory messageHandlerFactory;
    private final UiTopoOverlayFactory topoOverlayFactory;


    // private constructor - only the builder calls this
    private UiExtension(ClassLoader cl, String path, List<UiView> views,
                        UiMessageHandlerFactory mhFactory,
                        UiTopoOverlayFactory toFactory) {
        this.classLoader = cl;
        this.resourcePath = path;
        this.views = views;
        this.messageHandlerFactory = mhFactory;
        this.topoOverlayFactory = toFactory;
    }


    /**
     * Returns input stream containing CSS inclusion statements.
     *
     * @return CSS inclusion statements
     */
    public InputStream css() {
        return getStream(resourcePath + CSS_HTML);
    }

    /**
     * Returns input stream containing JavaScript inclusion statements.
     *
     * @return JavaScript inclusion statements
     */
    public InputStream js() {
        return getStream(resourcePath + JS_HTML);
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
        return getStream(VIEW_PREFIX + viewId + SLASH + path);
    }

    /**
     * Returns message handler factory, if one was defined.
     *
     * @return message handler factory
     */
    public UiMessageHandlerFactory messageHandlerFactory() {
        return messageHandlerFactory;
    }

    /**
     * Returns the topology overlay factory, if one was defined.
     *
     * @return topology overlay factory
     */
    public UiTopoOverlayFactory topoOverlayFactory() {
        return topoOverlayFactory;
    }


    // Returns the resource input stream from the specified class-loader.
    private InputStream getStream(String path) {
        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            log.warn("Unable to find resource {}", path);
        }
        return stream;
    }


    /**
     * UI Extension Builder.
     */
    public static class Builder {
        private ClassLoader classLoader;

        private String resourcePath = EMPTY;
        private List<UiView> views = new ArrayList<>();
        private UiMessageHandlerFactory messageHandlerFactory = null;
        private UiTopoOverlayFactory topoOverlayFactory = null;

        /**
         * Create a builder with the given class loader.
         * Resource path defaults to "".
         * Views defaults to an empty list.
         * Both Message and TopoOverlay factories default to null.
         *
         * @param cl    the class loader
         * @param views list of views contributed by this extension
         */
        public Builder(ClassLoader cl, List<UiView> views) {
            checkNotNull(cl, "Must provide a class loader");
            checkArgument(views.size() > 0, "Must provide at least one view");
            this.classLoader = cl;
            this.views = views;
        }

        /**
         * Set the resource path. That is, path to where the CSS and JS
         * files are located. This value should
         *
         * @param path resource path
         * @return self, for chaining
         */
        public Builder resourcePath(String path) {
            this.resourcePath = path == null ? EMPTY : path + SLASH;
            return this;
        }

        /**
         * Sets the message handler factory for this extension.
         *
         * @param mhFactory message handler factory
         * @return self, for chaining
         */
        public Builder messageHandlerFactory(UiMessageHandlerFactory mhFactory) {
            this.messageHandlerFactory = mhFactory;
            return this;
        }

        /**
         * Sets the topology overlay factory for this extension.
         *
         * @param toFactory topology overlay factory
         * @return self, for chaining
         */
        public Builder topoOverlayFactory(UiTopoOverlayFactory toFactory) {
            this.topoOverlayFactory = toFactory;
            return this;
        }

        /**
         * Builds the UI extension.
         *
         * @return UI extension instance
         */
        public UiExtension build() {
            return new UiExtension(classLoader, resourcePath, views,
                                   messageHandlerFactory, topoOverlayFactory);
        }

    }

}
