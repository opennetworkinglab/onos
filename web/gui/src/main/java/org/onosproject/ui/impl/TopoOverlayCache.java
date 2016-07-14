/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui.impl;

import org.onosproject.ui.UiTopoOverlay;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * A cache of {@link org.onosproject.ui.UiTopoOverlay}'s that were registered
 * at the time the UI connection was established.
 */
public class TopoOverlayCache {

    private static final String EMPTY = "";
    private static final UiTopoOverlay NONE = new NullOverlay();

    private final Map<String, UiTopoOverlay> overlays = new HashMap<>();
    private UiTopoOverlay current = NONE;

    public TopoOverlayCache() {
        overlays.put(null, NONE);
    }

    /**
     * Adds a topology overlay to the cache.
     *
     * @param overlay   a topology overlay
     */
    public void add(UiTopoOverlay overlay) {
        overlays.put(overlay.id(), overlay);
    }

    /**
     * Invoked when the cache is no longer needed.
     */
    public void destroy() {
        overlays.clear();
    }

    /**
     * Switching currently selected overlay.
     *
     * @param deact identity of overlay to deactivate
     * @param act identity of overlay to activate
     */
    public void switchOverlay(String deact, String act) {
        UiTopoOverlay toDeactivate = getOverlay(deact);
        UiTopoOverlay toActivate = getOverlay(act);

        toDeactivate.deactivate();
        current = toActivate;
        current.activate();
    }

    private UiTopoOverlay getOverlay(String id) {
        return isNullOrEmpty(id) ? NONE : overlays.get(id);
    }

    /**
     * Returns the current overlay instance.
     * Note that this method always returns a reference; when there is no
     * overlay selected the "NULL" overlay instance is returned.
     *
     * @return the current overlay
     */
    public UiTopoOverlay currentOverlay() {
        return current;
    }

    /**
     * Returns the number of overlays in the cache. Remember that this
     * includes the "NULL" overlay, representing "no overlay selected".
     *
     * @return number of overlays
     */
    public int size() {
        return overlays.size();
    }

    /**
     * Returns true if the identifier of the currently active overlay
     * matches the given parameter.
     *
     * @param overlayId overlay identifier
     * @return true if this matches the ID of currently active overlay
     */
    public boolean isActive(String overlayId) {
        return currentOverlay().id().equals(overlayId);
    }

    // overlay instance representing "no overlay selected"
    private static class NullOverlay extends UiTopoOverlay {
        public NullOverlay() {
            super(EMPTY);
        }
    }
}
