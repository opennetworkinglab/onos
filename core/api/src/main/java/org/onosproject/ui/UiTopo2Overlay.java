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
 * This base class does little more than provide a logger and an identifier.
 * <p>
 * Subclasses will want to override some or all of the base methods
 * to do useful things during the life-cycle of the (topo-2) overlay.
 */
public class UiTopo2Overlay {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String id;

    private boolean isActive = false;

    /**
     * Creates a new user interface topology view overlay descriptor with
     * the given identifier.
     *
     * @param id overlay identifier
     */
    public UiTopo2Overlay(String id) {
        this.id = id;
    }

    /**
     * Returns the identifier for this overlay.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return "UiTopo2Overlay{id=\"" + id +
                "\", class=\"" + getClass().getSimpleName() + "\"}";
    }

    /**
     * Callback invoked to initialize this overlay, soon after creation.
     * This default implementation does nothing.
     * Subclasses may choose to override this to set some initial state.
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
