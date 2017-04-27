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

package org.onosproject.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a user interface topology-2 view overlay.
 * <p>
 * This base class does little more than provide a logger, an identifier,
 * name, and glyph ID.
 * Subclasses will probably want to override some or all of the base methods
 * to do useful things during the life-cycle of the (topo-2) overlay.
 */
public class UiTopo2Overlay {

    private static final String DEFAULT_GLYPH_ID = "m_topo";

    /**
     * Logger for this overlay.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String id;
    private final String name;

    private boolean isActive = false;

    /**
     * Creates a new user interface topology view overlay descriptor, with
     * the given identifier and (human readable) name.
     *
     * @param id overlay identifier
     * @param name overlay name
     */
    public UiTopo2Overlay(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the identifier for this overlay.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the name for this overlay.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the glyph identifier to use in the toolbar.
     * This implementation returns a default value. Subclasses may override
     * this to provide the identity of a custom glyph.
     *
     * @return glyph ID
     */
    public String glyphId() {
        return DEFAULT_GLYPH_ID;
    }

    /**
     * Callback invoked to initialize this overlay, soon after creation.
     * This default implementation does nothing.
     */
    public void init() {
    }

    /**
     * Callback invoked when this overlay is activated.
     */
    public void activate() {
        isActive = true;
    }

    /**
     * Callback invoked when this overlay is deactivated.
     */
    public void deactivate() {
        isActive = false;
    }

    /**
     * Returns true if this overlay is currently active.
     *
     * @return true if overlay active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Callback invoked to destroy this instance by cleaning up any
     * internal state ready for garbage collection.
     * This default implementation holds no state and does nothing.
     */
    public void destroy() {
    }

    /**
     * Callback invoked when the topology highlighting should be updated.
     * It is the implementation's responsibility to update the Model
     * Highlighter state. This implementation does nothing.
     */
    public void highlightingCallback(/* ref to highlight model ? */) {

    }
}
