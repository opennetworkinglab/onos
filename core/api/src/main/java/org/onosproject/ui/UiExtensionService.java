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

import org.onosproject.ui.lion.LionBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for registering user interface extensions.
 */
public interface UiExtensionService {

    /**
     * Registers the specified user interface extension.
     *
     * @param extension UI extension to register
     */
    void register(UiExtension extension);

    /**
     * Unregisters the specified user interface extension.
     *
     * @param extension UI extension to unregister
     */
    void unregister(UiExtension extension);

    /**
     * Registers the specified user interface glyph factory.
     *
     * @param factory UI glyph factory to register
     */
    default void register(UiGlyphFactory factory) {
    }

    /**
     * Unregisters the specified user interface glyph factory.
     *
     * @param factory UI glyph factory to unregister
     */
    default void unregister(UiGlyphFactory factory) {
    }

    /**
     * Registers the specified topo hilighter factory.
     *
     * @param factory UI topo higlighter factory to register
     */
    default void register(UiTopoHighlighterFactory factory) {
    }

    /**
     * Unregisters the specified user interface extension.
     *
     * @param factory UI topo higlighter factory to unregister
     */
    default void unregister(UiTopoHighlighterFactory factory) {
    }

    /**
     * Returns the list of registered user interface extensions.
     *
     * @return list of extensions
     */
    List<UiExtension> getExtensions();

    /**
     * Returns the user interface extension that contributed the specified view.
     *
     * @param viewId view identifier
     * @return contributing user interface extension
     */
    UiExtension getViewExtension(String viewId);

    /**
     * Returns the list of registered user interface glyphs.
     *
     * @return list of glyphs
     */
    default List<UiGlyph> getGlyphs() {
        return new ArrayList<UiGlyph>();
    }

    /**
     * Returns the list of registered topo highlighter factories.
     *
     * @return list of highlighter factories
     */
    default List<UiTopoHighlighterFactory> getTopoHighlighterFactories() {
        return new ArrayList<UiTopoHighlighterFactory>();
    }

    /**
     * Returns the navigation pane localization bundle.
     *
     * @return the navigation localization bundle
     */
    LionBundle getNavLionBundle();

    /**
     * Refreshes the backing model.
     */
    void refreshModel();
}
