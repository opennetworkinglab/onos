/*
 * Copyright 2018 Open Networking Foundation
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

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a glyph to be used in the user interface topology view. Instances
 * of this class are immutable.
 */
public class UiGlyph {

    private final String id;
    private final String viewbox;
    private final String path;


    /**
     * Creates a new glyph.
     *
     * The value of the viewbox parameter is a string of four numbers min-x,
     * min-y, width and height, separated by whitespace and/or a comma.
     *
     * The path parameter specifies how this element is to be drawn inside of
     * the viewbox. The ONOS GUI only uses single paths – not rectangles,
     * strokes, circles, or anything else. One path definition has to be used
     * for the entire glyph.
     *
     * @param id       glyph identifier
     * @param viewbox  glyph viewbox
     * @param path     glyph path
     */
    public UiGlyph(String id, String viewbox, String path) {
        this.id = id;
        this.viewbox = viewbox;
        this.path = path;
    }

    /**
     * Returns the identifier for this glyph.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the viewbox for this glyph.
     *
     * @return the viewbox
     */
    public String viewbox() {
        return viewbox;
    }

    /**
     * Returns the path for this glyph.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("viewbox", viewbox)
                .add("path", path)
                .toString();
    }
}
