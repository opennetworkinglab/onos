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

package org.onosproject.ui.impl.topo;

import org.onosproject.ui.UiTopo2Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * A cache of {@link org.onosproject.ui.UiTopo2Overlay}'s that were
 * registered at the time the UI connection was established.
 * <p>
 * Note, for now, this is a simplified version which will only cache
 * a single overlay. At some future point, this should be expanded to mirror
 * the behavior of {@link org.onosproject.ui.impl.TopoOverlayCache}.
 */
public class Topo2OverlayCache {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String EMPTY = "";
    private static final String NO_OVERLAY = "No Overlay";
    private static final String UNKNOWN = "unknown";

    private static final UiTopo2Overlay NONE = new NullOverlay();

    private final Map<String, UiTopo2Overlay> overlays = new HashMap<>();
    private UiTopo2Overlay current = null;

    /**
     * Constructs the overlay cache.
     */
    public Topo2OverlayCache() {
        overlays.put(null, NONE);
    }

    /**
     * Adds a topology-2 overlay to the cache.
     *
     * @param overlay a topology-2 overlay
     */
    public void add(UiTopo2Overlay overlay) {
        overlays.put(overlay.id(), overlay);
        log.warn("added overlay: " + overlay);
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
     * @param act   identity of overlay to activate
     */
    public void switchOverlay(String deact, String act) {
        UiTopo2Overlay toDeactivate = getOverlay(deact);
        UiTopo2Overlay toActivate = getOverlay(act);

        toDeactivate.deactivate();
        current = toActivate;
        current.activate();
    }

    private UiTopo2Overlay getOverlay(String id) {
        return isNullOrEmpty(id) ? NONE : overlays.get(id);
    }

    /**
     * Returns the current overlay instance.
     * Note that this method always returns a reference; when there is no
     * overlay selected the "NULL" overlay instance is returned.
     *
     * @return the current overlay
     */
    public UiTopo2Overlay currentOverlay() {
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

    /**
     * Returns the collection of registered overlays.
     *
     * @return registered overlays
     */
    public Collection<UiTopo2Overlay> list() {
        return overlays.values();
    }

    // overlay instance representing "no overlay selected"
    private static class NullOverlay extends UiTopo2Overlay {
        NullOverlay() {
            super(EMPTY);
        }
    }
}
