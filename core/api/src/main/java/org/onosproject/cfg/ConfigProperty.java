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
package org.onosproject.cfg;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Component configuration property.
 */
public final class ConfigProperty {

    private final String name;
    private final Type type;
    private final String value;
    private final String defaultValue;
    private final String description;
    private final boolean isSet;

    /**
     * Representation of the type of property value.
     */
    public enum Type {
        /**
         * Indicates the value is a string.
         */
        STRING,

        /**
         * Indicates the value is a byte.
         */
        BYTE,

        /**
         * Indicates the value is an integer.
         */
        INTEGER,

        /**
         * Indicates the value is a long.
         */
        LONG,

        /**
         * Indicates the value is a float.
         */
        FLOAT,

        /**
         * Indicates the value is a double.
         */
        DOUBLE,

        /**
         * Indicates the value is a boolean.
         */
        BOOLEAN
    }

    /**
     * Creates a new configuration property with its default value.
     *
     * @param name         property name
     * @param type         value type
     * @param defaultValue default value as a string
     * @param description  property description
     * @return newly defined property
     */
    public static ConfigProperty defineProperty(String name, Type type,
                                                String defaultValue,
                                                String description) {
        return new ConfigProperty(name, type, description, defaultValue, defaultValue, false);
    }

    /**
     * Creates a new configuration property as a copy of an existing one, but
     * with a new value.
     *
     * @param property property to be changed
     * @param newValue new value as a string
     * @return newly updated property
     */
    public static ConfigProperty setProperty(ConfigProperty property, String newValue) {
        return new ConfigProperty(property.name, property.type, property.description,
                                  property.defaultValue, newValue, true);
    }

    /**
     * Creates a new configuration property as a copy of an existing one, but
     * without a specific value, thus making it take its default value.
     *
     * @param property property to be reset
     * @return newly reset property
     */
    public static ConfigProperty resetProperty(ConfigProperty property) {
        return new ConfigProperty(property.name, property.type, property.description,
                                  property.defaultValue, property.defaultValue, false);
    }

    /**
     * Creates a new configuration property with its default value.
     *
     * @param name         property name
     * @param type         value type
     * @param defaultValue default value as a string
     * @param description  property description
     * @param value        property value
     * @param isSet        indicates whether the property is set or not
     */
    private ConfigProperty(String name, Type type, String description,
                           String defaultValue, String value, boolean isSet) {
        this.name = checkNotNull(name, "Property name cannot be null");
        this.type = checkNotNull(type, "Property type cannot be null");
        this.description = checkNotNull(description, "Property description cannot be null");
        this.defaultValue = defaultValue;
        this.value = value;
        this.isSet = isSet;
    }

    /**
     * Returns the property name.
     *
     * @return property name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the property type.
     *
     * @return property type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the property description.
     *
     * @return string value
     */
    public String description() {
        return description;
    }

    /**
     * Returns the property default value as a string.
     *
     * @return string default value
     */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the property value as a string.
     *
     * @return string value
     */
    public String value() {
        return value;
    }

    /**
     * Indicates whether the property is set or whether it assumes its
     * default value.
     *
     * @return true if the property is set
     */
    public boolean isSet() {
        return isSet;
    }

    /**
     * Returns the property value as a string.
     *
     * @return string value
     */
    public String asString() {
        return value;
    }

    /**
     * Returns the property value as a byte.
     *
     * @return byte value
     */
    public byte asByte() {
        checkState(type == Type.BYTE, "Value is not a byte");
        return Byte.parseByte(value);
    }

    /**
     * Returns the property value as an integer.
     *
     * @return integer value
     */
    public int asInteger() {
        checkState(type == Type.INTEGER, "Value is not an integer");
        return Integer.parseInt(value);
    }

    /**
     * Returns the property value as a long.
     *
     * @return long value
     */
    public long asLong() {
        checkState(type == Type.INTEGER || type == Type.LONG, "Value is not a long or integer");
        return Long.parseLong(value);
    }

    /**
     * Returns the property value as a float.
     *
     * @return float value
     */
    public float asFloat() {
        checkState(type == Type.FLOAT, "Value is not a float");
        return Float.parseFloat(value);
    }

    /**
     * Returns the property value as a double.
     *
     * @return double value
     */
    public double asDouble() {
        checkState(type == Type.FLOAT || type == Type.DOUBLE, "Value is not a float or double");
        return Double.parseDouble(value);
    }

    /**
     * Returns the property value as a boolean.
     *
     * @return string value
     */
    public boolean asBoolean() {
        checkState(type == Type.BOOLEAN, "Value is not a boolean");
        return Boolean.parseBoolean(value);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * Equality is considered only on the basis of property name.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ConfigProperty) {
            final ConfigProperty other = (ConfigProperty) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("value", value)
                .add("defaultValue", defaultValue)
                .add("description", description)
                .add("isSet", isSet)
                .toString();
    }
}
