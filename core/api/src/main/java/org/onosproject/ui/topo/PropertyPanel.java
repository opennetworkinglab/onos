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
 */

package org.onosproject.ui.topo;

import com.google.common.collect.Sets;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Models a panel displayed on the Topology View.
 */
public class PropertyPanel {

    private static final NumberFormat NF = NumberFormat.getInstance();

    private String title;
    private String typeId;
    private String id;
    private String navPath;
    private List<Prop> properties = new ArrayList<>();
    private List<ButtonId> buttons = new ArrayList<>();

    /**
     * Constructs a property panel model with the given title and
     * type identifier (icon to display).
     *
     * @param title  title text
     * @param typeId type (icon) ID
     */
    public PropertyPanel(String title, String typeId) {
        this.title = title;
        this.typeId = typeId;
    }

    /**
     * Returns a number formatter to use for formatting integer and long
     * property values.
     * <p>
     * This default implementation uses a formatter for the default
     * locale. For example:
     * <pre>
     *     Locale.ENGLISH  :  1000 -&gt; "1,000"
     *     Locale.FRENCH   :  1000 -&gt; "1 000"
     *     Locale.GERMAN   :  1000 -&gt; "1.000"
     * </pre>
     *
     * @return the number formatter
     */
    protected NumberFormat formatter() {
        return NF;
    }

    /**
     * Adds a navigation path field to the panel data, to be included in
     * the returned JSON data to the client. This is typically used to
     * configure the topology view with the appropriate navigation path for
     * a hot-link to some other view.
     *
     * @param navPath the navigation path
     * @return self for chaining
     */
    public PropertyPanel navPath(String navPath) {
        this.navPath = navPath;
        return this;
    }

    /**
     * Adds an ID field to the panel data, to be included in
     * the returned JSON data to the client.
     *
     * @param id the identifier
     * @return self, for chaining
     */
    public PropertyPanel id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Adds a property to the panel data.
     *
     * @param key   property key
     * @param value property value
     * @return self, for chaining
     */
    public PropertyPanel addProp(String key, String value) {
        properties.add(new Prop(key, value));
        return this;
    }

    /**
     * Adds a property to the panel data, using a decimal formatter.
     *
     * @param key   property key
     * @param value property value
     * @return self, for chaining
     */
    public PropertyPanel addProp(String key, int value) {
        properties.add(new Prop(key, formatter().format(value)));
        return this;
    }

    /**
     * Adds a property to the panel data, using a decimal formatter.
     *
     * @param key   property key
     * @param value property value
     * @return self, for chaining
     */
    public PropertyPanel addProp(String key, long value) {
        properties.add(new Prop(key, formatter().format(value)));
        return this;
    }

    /**
     * Adds a property to the panel data. Note that the value's
     * {@link Object#toString toString()} method is used to convert the
     * value to a string.
     *
     * @param key   property key
     * @param value property value
     * @return self, for chaining
     */
    public PropertyPanel addProp(String key, Object value) {
        properties.add(new Prop(key, value.toString()));
        return this;
    }

    /**
     * Adds a property to the panel data. Note that the value's
     * {@link Object#toString toString()} method is used to convert the
     * value to a string, from which the characters defined in the given
     * regular expression string are stripped.
     *
     * @param key     property key
     * @param value   property value
     * @param reStrip regexp characters to strip from value string
     * @return self, for chaining
     */
    public PropertyPanel addProp(String key, Object value, String reStrip) {
        String val = value.toString().replaceAll(reStrip, "");
        properties.add(new Prop(key, val));
        return this;
    }

    /**
     * Adds a separator to the panel data.
     *
     * @return self, for chaining
     */
    public PropertyPanel addSeparator() {
        properties.add(new Separator());
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
     * Returns the navigation path.
     *
     * @return the navigation path
     */
    public String navPath() {
        return navPath;
    }

    /**
     * Returns the internal ID.
     *
     * @return the ID
     */
    public String id() {
        return id;
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

    /**
     * Returns the list of button descriptors.
     *
     * @return the button list
     */
    // TODO: consider protecting this?
    public List<ButtonId> buttons() {
        return buttons;
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
        Set<String> forRemoval = Sets.newHashSet(keys);
        List<Prop> toKeep = new ArrayList<>();
        for (Prop p : properties) {
            if (!forRemoval.contains(p.key())) {
                toKeep.add(p);
            }
        }
        properties = toKeep;
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

    /**
     * Adds the given button descriptor to the panel data.
     *
     * @param button button descriptor
     * @return self, for chaining
     */
    public PropertyPanel addButton(ButtonId button) {
        buttons.add(button);
        return this;
    }

    /**
     * Removes buttons with the given descriptors from the list.
     *
     * @param descriptors descriptors to remove
     * @return self, for chaining
     */
    public PropertyPanel removeButtons(ButtonId... descriptors) {
        Set<ButtonId> forRemoval = Sets.newHashSet(descriptors);
        List<ButtonId> toKeep = new ArrayList<>();
        for (ButtonId bd : buttons) {
            if (!forRemoval.contains(bd)) {
                toKeep.add(bd);
            }
        }
        buttons = toKeep;
        return this;
    }

    /**
     * Removes all currently defined buttons.
     *
     * @return self, for chaining
     */
    public PropertyPanel removeAllButtons() {
        buttons.clear();
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
         * @param key   property key
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
