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

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Models a panel displayed on the Topology View.
 */
public class PropertyPanel {

    private String title;
    private String typeId;
    private List<Prop> properties = new ArrayList<>();

    /**
     * Constructs a property panel model with the given title and
     * type identifier (icon to display).
     *
     * @param title title text
     * @param typeId type (icon) ID
     */
    public PropertyPanel(String title, String typeId) {
        this.title = title;
        this.typeId = typeId;
    }

    /**
     * Adds a property to the panel.
     *
     * @param p the property
     * @return self, for chaining
     */
    public PropertyPanel add(Prop p) {
        properties.add(p);
        return this;
    }

    /**
     * Returns the title text.
     *
     * @return title text
     */
    public String title() {
        return title;
    }

    /**
     * Returns the type identifier.
     *
     * @return type identifier
     */
    public String typeId() {
        return typeId;
    }

    /**
     * Returns the list of properties to be displayed.
     *
     * @return the property list
     */
    // TODO: consider protecting this?
    public List<Prop> properties() {
        return properties;
    }

    // == MUTATORS

    /**
     * Sets the title text.
     *
     * @param title title text
     * @return self, for chaining
     */
    public PropertyPanel title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the type identifier (icon ID).
     *
     * @param typeId type identifier
     * @return self, for chaining
     */
    public PropertyPanel typeId(String typeId) {
        this.typeId = typeId;
        return this;
    }

    /**
     * Removes properties with the given keys from the list.
     *
     * @param keys keys of properties to remove
     * @return self, for chaining
     */
    public PropertyPanel removeProps(String... keys) {
        Set<String> keysForRemoval = Sets.newHashSet(keys);
        List<Prop> propsToKeep = new ArrayList<>();
        for (Prop p: properties) {
            if (!keysForRemoval.contains(p.key())) {
                propsToKeep.add(p);
            }
        }
        properties = propsToKeep;
        return this;
    }

    /**
     * Removes all currently defined properties.
     *
     * @return self, for chaining
     */
    public PropertyPanel removeAllProps() {
        properties.clear();
        return this;
    }

    // ====================

    /**
     * Simple data carrier for a property, composed of a key/value pair.
     */
    public static class Prop {
        private final String key;
        private final String value;

        /**
         * Constructs a property data value.
         *
         * @param key property key
         * @param value property value
         */
        public Prop(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the property's key.
         *
         * @return the key
         */
        public String key() {
            return key;
        }

        /**
         * Returns the property's value.
         *
         * @return the value
         */
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Prop prop = (Prop) o;
            return key.equals(prop.key) && value.equals(prop.value);
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "{" + key + " -> " + value + "}";
        }
    }

    /**
     * Auxiliary class representing a separator property.
     */
    public static class Separator extends Prop {
        public Separator() {
            super("-", "");
        }
    }

}
