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
 *
 */

package org.onosproject.ui.topo;

/**
 * Designates a descriptor for a button on the topology view panels.
 */
public class ButtonDescriptor {

    private final String id;
    private final String glyphId;
    private final String tooltip;

    /**
     * Creates a button descriptor with the given identifier, glyph ID, and
     * tooltip text. To reference a custom glyph defined in the overlay itself,
     * prefix its ID with an asterisk, (e.g. {@code "*myGlyph"}). Alternatively,
     * use one of the {@link TopoConstants.Glyphs predefined constant}.
     *
     * @param id identifier for the button
     * @param glyphId identifier for the glyph
     * @param tooltip tooltip text
     */
    public ButtonDescriptor(String id, String glyphId, String tooltip) {
        this.id = id;
        this.glyphId = glyphId;
        this.tooltip = tooltip;
    }

    /**
     * Returns the identifier for this button.
     *
     * @return identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the glyph identifier for this button.
     *
     * @return glyph identifier
     */
    public String glyphId() {
        return glyphId;
    }

    /**
     * Returns the tooltip text for this button.
     *
     * @return tooltip text
     */
    public String tooltip() {
        return tooltip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ButtonDescriptor that = (ButtonDescriptor) o;
        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
