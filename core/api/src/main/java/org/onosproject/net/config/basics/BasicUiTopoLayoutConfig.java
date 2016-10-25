/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.config.basics;

import com.google.common.base.MoreObjects;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.InvalidFieldException;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import static org.onosproject.net.region.RegionId.regionId;

/**
 * Basic configuration for UI topology layouts.
 * <p>
 * Note that a layout configuration will include information about
 * which background map (or sprites definition) to use, and at what
 * relative scale and offset.
 * <p>
 * Note also that the {@code geomap} and {@code sprites} fields are
 * mutually exclusive.
 */
public class BasicUiTopoLayoutConfig extends Config<UiTopoLayoutId> {

    static final String REGION = "region";
    static final String PARENT = "parent";
    static final String GEOMAP = "geomap";
    static final String SPRITES = "sprites";
    static final String SCALE = "scale";
    static final String OFFSET_X = "offsetX";
    static final String OFFSET_Y = "offsetY";

    static final double DEFAULT_SCALE = 1.0;
    static final double DEFAULT_OFFSET = 0.0;

    private static final String E_GEOMAP_SPRITE =
            "Layout cannot have both geomap and sprites defined";
    private static final String E_SPRITES_ALREADY_SET =
            "Can't set geomap when sprites is already set";
    private static final String E_GEOMAP_ALREADY_SET =
            "Can't set sprites when geomap is already set";

    @Override
    public boolean isValid() {
        if (object.has(GEOMAP) && object.has(SPRITES)) {
            throw new InvalidFieldException(GEOMAP, E_GEOMAP_SPRITE);
        }

        return hasOnlyFields(REGION, PARENT, GEOMAP, SPRITES, SCALE,
                OFFSET_X, OFFSET_Y);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("region", region())
                .add("parent", parent())
                .add("geomap", geomap())
                .add("sprites", sprites())
                .add("scale", scale())
                .add("offX", offsetX())
                .add("offY", offsetY())
                .toString();
    }

    /**
     * Returns the identifier of the backing region. This will be
     * null if there is no backing region.
     *
     * @return backing region identifier
     */
    public RegionId region() {
        String r = get(REGION, null);
        return r == null ? null : regionId(r);
    }

    /**
     * Sets the identifier of the backing region.
     *
     * @param id backing region identifier, or null to unset
     * @return config for UI topology layout
     */
    public BasicUiTopoLayoutConfig region(RegionId id) {
        setOrClear(REGION, id == null ? null : id.id());
        return this;
    }

    /**
     * Returns the identifier of the parent layout.
     *
     * @return layout identifier of parent
     */
    public UiTopoLayoutId parent() {
        String p = get(PARENT, null);
        return p == null ? UiTopoLayoutId.DEFAULT_ID : UiTopoLayoutId.layoutId(p);
    }

    /**
     * Sets the identifier of the parent layout.
     *
     * @param id parent ui-topo-layout identifier, or null to unset
     * @return config for UI topology layout
     */
    public BasicUiTopoLayoutConfig parent(UiTopoLayoutId id) {
        setOrClear(PARENT, id == null ? null : id.id());
        return this;
    }

    /**
     * Returns the identifier for the background geo-map.
     *
     * @return geo-map identifier
     */
    public String geomap() {
        return get(GEOMAP, null);
    }

    /**
     * Sets the name of the geomap (topojson file) to use for this layout.
     *
     * @param geomap geomap name; null to clear
     * @return config for UI topology layout
     * @throws InvalidFieldException if the sprites field is already set
     */
    public BasicUiTopoLayoutConfig geomap(String geomap) {
        if (geomap != null && hasField(SPRITES)) {
            throw new InvalidFieldException(GEOMAP, E_SPRITES_ALREADY_SET);
        }
        setOrClear(GEOMAP, geomap);
        return this;
    }

    /**
     * Returns the identifier for the background sprites.
     *
     * @return sprites identifier
     */
    public String sprites() {
        return get(SPRITES, null);
    }

    /**
     * Sets the name of the sprites definition to use for this layout.
     *
     * @param sprites sprites definition name; null to clear
     * @return config for UI topology layout
     * @throws InvalidFieldException if the geomap field is already set
     */
    public BasicUiTopoLayoutConfig sprites(String sprites) {
        if (sprites != null && hasField(GEOMAP)) {
            throw new InvalidFieldException(GEOMAP, E_GEOMAP_ALREADY_SET);
        }
        setOrClear(SPRITES, sprites);
        return this;
    }

    /**
     * Returns the scale for the geomap / sprites background.
     *
     * @return scale of background map / diagram
     */
    public double scale() {
        return get(SCALE, DEFAULT_SCALE);
    }

    /**
     * Sets the scale for the geomap / sprites background.
     *
     * @param scale the scale to set
     * @return config for UI topology layout
     */
    public BasicUiTopoLayoutConfig scale(Double scale) {
        setOrClear(SCALE, scale);
        return this;
    }

    /**
     * Returns the x-offset for the geomap / sprites background.
     *
     * @return x-offset of background map / diagram
     */
    public double offsetX() {
        return get(OFFSET_X, DEFAULT_OFFSET);
    }

    /**
     * Sets the x-offset for the geomap / sprites background.
     *
     * @param offsetX the x-offset to set
     * @return config for UI topology layout
     */
    public BasicUiTopoLayoutConfig offsetX(Double offsetX) {
        setOrClear(OFFSET_X, offsetX);
        return this;
    }

    /**
     * Returns the y-offset for the geomap / sprites background.
     *
     * @return y-offset of background map / diagram
     */
    public double offsetY() {
        return get(OFFSET_Y, DEFAULT_OFFSET);
    }

    /**
     * Sets the scale for the geomap / sprites background.
     *
     * @param offsetY the y-offset to set
     * @return config for UI topology layout
     */
    public BasicUiTopoLayoutConfig offsetY(Double offsetY) {
        setOrClear(OFFSET_Y, offsetY);
        return this;
    }

}
