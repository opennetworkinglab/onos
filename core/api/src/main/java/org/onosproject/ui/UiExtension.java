/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.ui.lion.LionBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a user interface extension.
 * <p>
 * Note that the {@link Builder} class is used to create a user
 * interface extension instance, and that these instances are immutable.
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
    private final List<UiView> viewList;
    private final List<LionBundle> lionBundles;
    private final UiMessageHandlerFactory messageHandlerFactory;
    private final UiTopoOverlayFactory topoOverlayFactory;
    private final UiTopo2OverlayFactory topo2OverlayFactory;
    private final UiTopoMapFactory topoMapFactory;

    private boolean isValid = true;
    private boolean ui2Valid = true;

    // private constructor - only the builder calls this
    private UiExtension(ClassLoader cl, String path, List<UiView> views,
                        List<LionBundle> bundles,
                        UiMessageHandlerFactory mhFactory,
                        UiTopoOverlayFactory toFactory,
                        UiTopo2OverlayFactory to2Factory,
                        UiTopoMapFactory tmFactory,
                        boolean ui2Vld) {
        classLoader = cl;
        resourcePath = path;
        viewList = views;
        lionBundles = bundles;
        messageHandlerFactory = mhFactory;
        topoOverlayFactory = toFactory;
        topo2OverlayFactory = to2Factory;
        topoMapFactory = tmFactory;
        ui2Valid = ui2Vld;
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
        return (isValid || ui2Valid) ? viewList : ImmutableList.of();
    }

    /**
     * Returns the list of localization bundles that this extension is
     * contributing.
     *
     * @return contributed localization bundles
     */
    public List<LionBundle> lionBundles() {
        return ImmutableList.copyOf(lionBundles);
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

    /**
     * Returns the topology-2 overlay factory, if one was defined.
     *
     * @return topology-2 overlay factory
     */
    public UiTopo2OverlayFactory topo2OverlayFactory() {
        return topo2OverlayFactory;
    }

    /**
     * Returns the topology map factory, if one was defined.
     *
     * @return topology map factory
     */
    public UiTopoMapFactory topoMapFactory() {
        return topoMapFactory;
    }


    // Returns the resource input stream from the specified class-loader.
    private InputStream getStream(String path) {
        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            isValid = false;
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
        private List<UiView> viewList = new ArrayList<>();
        private List<LionBundle> lionBundles = new ArrayList<>();
        private UiMessageHandlerFactory messageHandlerFactory = null;
        private UiTopoOverlayFactory topoOverlayFactory = null;
        private UiTopo2OverlayFactory topo2OverlayFactory = null;
        private UiTopoMapFactory topoMapFactory = null;
        private boolean ui2valid = false;

        /**
         * Create a builder with the given class loader.
         * Resource path defaults to "".
         * Views defaults to an empty list.
         * MessageHandler, TopoOverlay, and TopoMap factories default to null.
         *
         * @param cl    the class loader
         * @param views list of views contributed by this extension
         */
        public Builder(ClassLoader cl, List<UiView> views) {
            checkNotNull(cl, "Must provide a class loader");
            checkArgument(!views.isEmpty(), "Must provide at least one view");
            classLoader = cl;
            viewList = views;
        }

        /**
         * Sets the localization bundles (aka {@code LionBundle}s) that this
         * UI extension is contributing.
         *
         * @param bundles the bundles to register
         * @return self, for chaining
         */
        public Builder lionBundles(List<LionBundle> bundles) {
            checkNotNull(bundles, "Must provide a list");
            lionBundles = bundles;
            return this;
        }

        /**
         * Set the resource path. That is, the path to where the CSS and JS
         * files are located.
         *
         * @param path resource path
         * @return self, for chaining
         */
        public Builder resourcePath(String path) {
            resourcePath = path == null ? EMPTY : path + SLASH;
            return this;
        }

        /**
         * Sets the message handler factory for this extension.
         *
         * @param mhFactory message handler factory
         * @return self, for chaining
         */
        public Builder messageHandlerFactory(UiMessageHandlerFactory mhFactory) {
            messageHandlerFactory = mhFactory;
            return this;
        }

        /**
         * Sets the topology overlay factory for this extension.
         *
         * @param toFactory topology overlay factory
         * @return self, for chaining
         */
        public Builder topoOverlayFactory(UiTopoOverlayFactory toFactory) {
            topoOverlayFactory = toFactory;
            return this;
        }

        /**
         * Sets the topology-2 overlay factory for this extension.
         *
         * @param to2Factory topology-2 overlay factory
         * @return self, for chaining
         */
        public Builder topo2OverlayFactory(UiTopo2OverlayFactory to2Factory) {
            topo2OverlayFactory = to2Factory;
            return this;
        }

        /**
         * Sets the topology map factory for this extension.
         *
         * @param tmFactory topology map factory
         * @return self, for chaining
         */
        public Builder topoMapFactory(UiTopoMapFactory tmFactory) {
            topoMapFactory = tmFactory;
            return this;
        }

        /**
         * Marks this as ui2valid for Ui2.
         * This is because Ui2 does not include the same layout of embedded html, js and css
         *
         * @return self, for chaining
         */
        public Builder ui2() {
            ui2valid = true;
            return this;
        }

        /**
         * Builds the user interface extension.
         *
         * @return UI extension instance
         */
        public UiExtension build() {
            return new UiExtension(classLoader, resourcePath, viewList,
                                   lionBundles,
                                   messageHandlerFactory, topoOverlayFactory,
                                   topo2OverlayFactory, topoMapFactory, ui2valid);
        }
    }
}
