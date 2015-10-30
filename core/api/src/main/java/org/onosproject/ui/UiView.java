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
 */
package org.onosproject.ui;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Represents user interface view addition.
 */
public class UiView {

    /**
     * Designates navigation menu category.
     */
    public enum Category {
        /**
         * Represents platform related views.
         */
        PLATFORM("Platform"),

        /**
         * Represents network-control related views.
         */
        NETWORK("Network"),

        /**
         * Represents miscellaneous views.
         */
        OTHER("Other"),

        /**
         * Represents views that do not show in the navigation menu.
         * This category should not be specified directly; rather, use
         * the {@link UiViewHidden} constructor instead of {@link UiView}.
         */
        HIDDEN("(hidden)");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        /**
         * Returns display label for the category.
         *
         * @return display label
         */
        public String label() {
            return label;
        }
    }

    private final Category category;
    private final String id;
    private final String label;
    private final String iconId;

    /**
     * Creates a new user interface view descriptor. The navigation item
     * will appear in the navigation menu under the specified category.
     *
     * @param category view category
     * @param id       view identifier
     * @param label    view label
     */
    public UiView(Category category, String id, String label) {
        this(category, id, label, null);
    }

    /**
     * Creates a new user interface view descriptor. The navigation item
     * will appear in the navigation menu under the specified category,
     * with the specified icon adornment.
     *
     * @param category view category
     * @param id       view identifier
     * @param label    view label
     * @param iconId   icon id
     */
    public UiView(Category category, String id, String label, String iconId) {
        this.category = category;
        this.id = id;
        this.label = label;
        this.iconId = iconId;
    }

    /**
     * Returns the navigation category.
     *
     * @return navigation category
     */
    public Category category() {
        return category;
    }

    /**
     * Returns the view identifier.
     *
     * @return view id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the view label.
     *
     * @return view label
     */
    public String label() {
        return label;
    }

    /**
     * Returns the icon ID.
     *
     * @return icon ID
     */
    public String iconId() {
        return iconId;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UiView other = (UiView) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("category", category)
                .add("id", id)
                .add("label", label)
                .add("iconId", iconId)
                .toString();
    }
}
