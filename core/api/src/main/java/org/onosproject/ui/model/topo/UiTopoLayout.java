/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

import com.google.common.base.MoreObjects;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a specific "subset" of the UI model of the network topology
 * that a user might wish to view. Backed by a {@link Region}.
 * <p>
 * These instances include information about which geo-map or grid-layout
 * should be displayed, along with zoom and offset parameters.
 */
public class UiTopoLayout {

    // package private for unit test access
    static final double SCALE_MIN = 0.01;
    static final double SCALE_MAX = 100.0;
    static final double SCALE_DEFAULT = 1.0;
    static final double OFFSET_DEFAULT = 0.0;

    static final String E_ROOT_PARENT = "Cannot change parent ID of root layout";
    static final String E_ROOT_REGION = "Cannot set region on root layout";
    static final String E_SPRITES_SET = "Cannot set geomap if sprites is set";
    static final String E_GEOMAP_SET = "Cannot set sprites if geomap is set";
    static final String E_SCALE_OOB =
            "Scale out of bounds; expected [" + SCALE_MIN + ".." + SCALE_MAX + "]";

    private final UiTopoLayoutId id;

    private Region region;
    private UiTopoLayoutId parent;
    private String geomap;
    private String sprites;
    private double scale = SCALE_DEFAULT;
    private double offsetX = OFFSET_DEFAULT;
    private double offsetY = OFFSET_DEFAULT;

    /**
     * Created a new UI topology layout.
     *
     * @param id layout identifier
     */
    public UiTopoLayout(UiTopoLayoutId id) {
        checkNotNull(id, "layout ID cannot be null");
        this.id = id;

        // NOTE: root layout is its own parent...
        if (isRoot()) {
            parent = id;
        }
    }

    /**
     * Returns true if this layout instance is at the top of the
     * hierarchy tree.
     *
     * @return true if this is the root layout
     */
    public boolean isRoot() {
        return UiTopoLayoutId.DEFAULT_ID.equals(id);
    }

    /**
     * Returns the UI layout identifier.
     *
     * @return identifier of the layout
     */
    public UiTopoLayoutId id() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("region", region)
                .add("parent", parent)
                .add("geomap", geomap)
                .add("sprites", sprites)
                .add("scale", scale)
                .add("offsetX", offsetX)
                .add("offsetY", offsetY)
                .toString();
    }

    /**
     * Sets the backing region for this layout. Note that an exception will
     * be thrown if this is the root layout.
     *
     * @param region the backing region
     * @return self, for chaining
     * @throws IllegalArgumentException if this is the root layout
     */
    public UiTopoLayout region(Region region) {
        if (isRoot()) {
            throw new IllegalArgumentException(E_ROOT_REGION);
        }

        this.region = region;
        return this;
    }

    /**
     * Returns the backing region with which this layout is associated. Note
     * that this may be null (for the root layout).
     *
     * @return backing region
     */
    public Region region() {
        return region;
    }

    /**
     * Returns the identifier of the backing region. If this is the default
     * layout, the null-region ID will be returned, otherwise the ID of the
     * backing region for this layout will be returned; null in the case that
     * there is no backing region.
     *
     * @return backing region identifier
     */
    public RegionId regionId() {
        return isRoot() ? UiRegion.NULL_ID :
                (region == null ? null : region.id());
    }

    /**
     * Sets the identity of this layout's parent. May be null to unset.
     * Note that an exception will be thrown if this is the root layout,
     * since the parent of the root is always itself, and cannot be changed.
     *
     * @param parentId parent layout identifier
     * @return self, for chaining
     * @throws IllegalArgumentException if this instance is the root layout
     */
    public UiTopoLayout parent(UiTopoLayoutId parentId) {
        if (isRoot()) {
            throw new IllegalArgumentException(E_ROOT_PARENT);
        }
        // TODO: consider checking ancestry chain to prevent loops

        parent = parentId;
        return this;
    }

    /**
     * Returns the parent layout identifier.
     *
     * @return parent layout identifier
     */
    public UiTopoLayoutId parent() {
        return parent;
    }

    /**
     * Sets the name of the geomap for this layout. This is the symbolic
     * name for a "topojson" file containing a geographic map projection,
     * to be displayed in the topology view, for this layout.
     * <p>
     * Since the geomap and sprites fields are mutually exclusive, this
     * method will throw an exception if the sprites field is already set.
     *
     * @param geomap the geomap name
     * @return self, for chaining
     * @throws IllegalArgumentException if the sprites field is not null
     */
    public UiTopoLayout geomap(String geomap) {
        if (sprites != null) {
            throw new IllegalArgumentException(E_SPRITES_SET);
        }
        this.geomap = geomap;
        return this;
    }

    /**
     * Returns the symbolic name for the geomap for this layout.
     *
     * @return name of geomap
     */
    public String geomap() {
        return geomap;
    }

    /**
     * Sets the name of the sprites definition for this layout. This is the
     * symbolic name for a definition of sprites,
     * which render as a symbolic background (e.g. a campus, or floor plan),
     * to be displayed in the topology view, for this layout.
     * <p>
     * Since the geomap and sprites fields are mutually exclusive, this
     * method will throw an exception if the geomap field is already set.
     *
     * @param sprites the sprites definition name
     * @return self, for chaining
     * @throws IllegalArgumentException if the geomap field is not null
     */
    public UiTopoLayout sprites(String sprites) {
        if (geomap != null) {
            throw new IllegalArgumentException(E_GEOMAP_SET);
        }
        this.sprites = sprites;
        return this;
    }

    /**
     * Returns the symbolic name for the sprites definition for this layout.
     *
     * @return name of sprites definition
     */
    public String sprites() {
        return sprites;
    }

    private boolean scaleWithinBounds(double scale) {
        return scale >= SCALE_MIN && scale <= SCALE_MAX;
    }

    /**
     * Sets the scale for the geomap / sprite image. Note that the
     * acceptable bounds are from {@value #SCALE_MIN} to {@value #SCALE_MAX}.
     *
     * @param scale the scale
     * @return self for chaining
     * @throws IllegalArgumentException if the value is out of bounds
     */
    public UiTopoLayout scale(double scale) {
        checkArgument(scaleWithinBounds(scale), E_SCALE_OOB);
        this.scale = scale;
        return this;
    }

    /**
     * Returns the scale for the geomap / sprite image.
     *
     * @return the scale
     */
    public double scale() {
        return scale;
    }

    /**
     * Sets the x-offset value.
     *
     * @param offsetX x-offset
     * @return self, for chaining
     */
    public UiTopoLayout offsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    /**
     * Returns the x-offset value.
     *
     * @return the x-offset
     */
    public double offsetX() {
        return offsetX;
    }

    /**
     * Sets the y-offset value.
     *
     * @param offsetY y-offset
     * @return self, for chaining
     */
    public UiTopoLayout offsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    /**
     * Returns the y-offset value.
     *
     * @return the y-offset
     */
    public double offsetY() {
        return offsetY;
    }

}
