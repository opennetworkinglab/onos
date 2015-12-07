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
package org.onosproject.net.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base abstraction of a configuration facade for a specific subject. Derived
 * classes should keep all state in the specified JSON tree as that is the
 * only state that will be distributed or persisted; this class is merely
 * a facade for interacting with a particular facet of configuration on a
 * given subject.
 *
 * @param <S> type of subject
 */
@Beta
public abstract class Config<S> {

    protected S subject;
    protected String key;

    protected JsonNode node;
    protected ObjectNode object;
    protected ArrayNode array;
    protected ObjectMapper mapper;

    protected ConfigApplyDelegate delegate;

    /**
     * Indicator of whether a configuration JSON field is required.
     */
    public enum FieldPresence {
        /**
         * Signifies that config field is an optional one.
         */
        OPTIONAL,

        /**
         * Signifies that config field is mandatory.
         */
        MANDATORY
    }

    /**
     * Initializes the configuration behaviour with necessary context.
     *
     * @param subject  configuration subject
     * @param key      configuration key
     * @param node     JSON node where configuration data is stored
     * @param mapper   JSON object mapper
     * @param delegate delegate context
     */
    public void init(S subject, String key, JsonNode node, ObjectMapper mapper,
                     ConfigApplyDelegate delegate) {
        this.subject = checkNotNull(subject);
        this.key = key;
        this.node = checkNotNull(node);
        this.object = node instanceof ObjectNode ? (ObjectNode) node : null;
        this.array = node instanceof ArrayNode ? (ArrayNode) node : null;
        this.mapper = checkNotNull(mapper);
        this.delegate = checkNotNull(delegate);
    }

    /**
     * Indicates whether or not the backing JSON node contains valid data.
     * <p>
     * Default implementation returns true.
     * Subclasses are expected to override this with their own validation.
     * </p>
     *
     * @return true if the data is valid; false otherwise
     */
    public boolean isValid() {
        // TODO: figure out what assertions could be made in the base class
        // NOTE: The thought is to have none, but instead to provide a set
        // of predicates to allow configs to test validity of present fields,
        // e.g.:
        //      isString(path)
        //      isBoolean(path)
        //      isNumber(path, [min, max])
        //      isDecimal(path, [min, max])
        //      isMacAddress(path)
        //      isIpAddress(path)
        return true;
    }

    /**
     * Returns the specific subject to which this configuration pertains.
     *
     * @return configuration subject
     */
    public S subject() {
        return subject;
    }

    /**
     * Returns the configuration key. This is primarily aimed for use in
     * composite JSON trees in external representations and has no bearing on
     * the internal behaviours.
     *
     * @return configuration key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the JSON node that contains the configuration data.
     *
     * @return JSON node backing the configuration
     */
    public JsonNode node() {
        return node;
    }

    /**
     * Applies any configuration changes made via this configuration.
     */
    public void apply() {
        delegate.onApply(this);
    }


    // Miscellaneous helpers for interacting with JSON

    /**
     * Gets the specified property as a string.
     *
     * @param name         property name
     * @param defaultValue default value if property not set
     * @return property value or default value
     */
    protected String get(String name, String defaultValue) {
        return object.path(name).asText(defaultValue);
    }

    /**
     * Sets the specified property as a string or clears it if null value given.
     *
     * @param name  property name
     * @param value new value or null to clear the property
     * @return self
     */
    protected Config<S> setOrClear(String name, String value) {
        if (value != null) {
            object.put(name, value);
        } else {
            object.remove(name);
        }
        return this;
    }

    /**
     * Gets the specified property as a boolean.
     *
     * @param name         property name
     * @param defaultValue default value if property not set
     * @return property value or default value
     */
    protected boolean get(String name, boolean defaultValue) {
        return object.path(name).asBoolean(defaultValue);
    }

    /**
     * Sets the specified property as a boolean or clears it if null value given.
     *
     * @param name  property name
     * @param value new value or null to clear the property
     * @return self
     */
    protected Config<S> setOrClear(String name, Boolean value) {
        if (value != null) {
            object.put(name, value.booleanValue());
        } else {
            object.remove(name);
        }
        return this;
    }

    /**
     * Gets the specified property as an integer.
     *
     * @param name         property name
     * @param defaultValue default value if property not set
     * @return property value or default value
     */
    protected int get(String name, int defaultValue) {
        return object.path(name).asInt(defaultValue);
    }

    /**
     * Sets the specified property as an integer or clears it if null value given.
     *
     * @param name  property name
     * @param value new value or null to clear the property
     * @return self
     */
    protected Config<S> setOrClear(String name, Integer value) {
        if (value != null) {
            object.put(name, value.intValue());
        } else {
            object.remove(name);
        }
        return this;
    }

    /**
     * Gets the specified property as a long.
     *
     * @param name         property name
     * @param defaultValue default value if property not set
     * @return property value or default value
     */
    protected long get(String name, long defaultValue) {
        return object.path(name).asLong(defaultValue);
    }

    /**
     * Sets the specified property as a long or clears it if null value given.
     *
     * @param name  property name
     * @param value new value or null to clear the property
     * @return self
     */
    protected Config<S> setOrClear(String name, Long value) {
        if (value != null) {
            object.put(name, value.longValue());
        } else {
            object.remove(name);
        }
        return this;
    }

    /**
     * Gets the specified property as a double.
     *
     * @param name         property name
     * @param defaultValue default value if property not set
     * @return property value or default value
     */
    protected double get(String name, double defaultValue) {
        return object.path(name).asDouble(defaultValue);
    }

    /**
     * Sets the specified property as a double or clears it if null value given.
     *
     * @param name  property name
     * @param value new value or null to clear the property
     * @return self
     */
    protected Config<S> setOrClear(String name, Double value) {
        if (value != null) {
            object.put(name, value.doubleValue());
        } else {
            object.remove(name);
        }
        return this;
    }

    /**
     * Gets the specified property as an enum.
     *
     * @param name         property name
     * @param defaultValue default value if property not set
     * @param enumClass    the enum class
     * @param <E>          type of enum
     * @return property value or default value
     */
    protected <E extends Enum<E>> E get(String name, E defaultValue, Class<E> enumClass) {
        return Enum.valueOf(enumClass, object.path(name).asText(defaultValue.toString()));
    }

    /**
     * Sets the specified property as a double or clears it if null value given.
     *
     * @param name  property name
     * @param value new value or null to clear the property
     * @param <E>   type of enum
     * @return self
     */
    protected <E extends Enum> Config<S> setOrClear(String name, E value) {
        if (value != null) {
            object.put(name, value.toString());
        } else {
            object.remove(name);
        }
        return this;
    }

    /**
     * Gets the specified array property as a list of items.
     *
     * @param name     property name
     * @param function mapper from string to item
     * @param <T>      type of item
     * @return list of items
     */
    protected <T> List<T> getList(String name, Function<String, T> function) {
        List<T> list = Lists.newArrayList();
        ArrayNode arrayNode = (ArrayNode) object.path(name);
        arrayNode.forEach(i -> list.add(function.apply(i.asText())));
        return list;
    }

    /**
     * Gets the specified array property as a list of items.
     *
     * @param name     property name
     * @param function mapper from string to item
     * @param defaultValue default value if property not set
     * @param <T>      type of item
     * @return list of items
     */
    protected <T> List<T> getList(String name, Function<String, T> function, List<T> defaultValue) {
        List<T> list = Lists.newArrayList();
        JsonNode jsonNode = object.path(name);
        if (jsonNode.isMissingNode()) {
            return defaultValue;
        }
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        arrayNode.forEach(i -> list.add(function.apply(i.asText())));
        return list;
    }

    /**
     * Sets the specified property as an array of items in a given collection or
     * clears it if null is given.
     *
     * @param name       propertyName
     * @param collection collection of items
     * @param <T>        type of items
     * @return self
     */
    protected <T> Config<S> setOrClear(String name, Collection<T> collection) {
        if (collection == null) {
            object.remove(name);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();
            collection.forEach(i -> arrayNode.add(i.toString()));
            object.set(name, arrayNode);
        }
        return this;
    }

    /**
     * Indicates whether only the specified fields are present in the backing JSON.
     *
     * @param allowedFields allowed field names
     * @return true if all allowedFields are present; false otherwise
     */
    protected boolean hasOnlyFields(String... allowedFields) {
        Set<String> fields = ImmutableSet.copyOf(allowedFields);
        return !Iterators.any(object.fieldNames(), f -> !fields.contains(f));
    }

    /**
     * Indicates whether the specified field holds a valid MAC address.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws IllegalArgumentException if field is present, but not valid MAC
     */
    protected boolean isMacAddress(String field, FieldPresence presence) {
        JsonNode node = object.path(field);
        return isValid(node, presence, node.isTextual() &&
                MacAddress.valueOf(node.asText()) != null);
    }

    /**
     * Indicates whether the specified field holds a valid IP address.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws IllegalArgumentException if field is present, but not valid IP
     */
    protected boolean isIpAddress(String field, FieldPresence presence) {
        JsonNode node = object.path(field);
        return isValid(node, presence, node.isTextual() &&
                IpAddress.valueOf(node.asText()) != null);
    }

    /**
     * Indicates whether the specified field holds a valid string value.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param pattern  optional regex pattern
     * @return true if valid; false otherwise
     * @throws IllegalArgumentException if field is present, but not valid MAC
     */
    protected boolean isString(String field, FieldPresence presence, String... pattern) {
        JsonNode node = object.path(field);
        return isValid(node, presence, node.isTextual() &&
                (pattern.length > 0 && node.asText().matches(pattern[0]) || pattern.length < 1));
    }

    /**
     * Indicates whether the specified field holds a valid number.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws IllegalArgumentException if field is present, but not valid
     */
    protected boolean isNumber(String field, FieldPresence presence, long... minMax) {
        JsonNode node = object.path(field);
        return isValid(node, presence, (node.isLong() || node.isInt()) &&
                (minMax.length > 0 && minMax[0] <= node.asLong() || minMax.length < 1) &&
                (minMax.length > 1 && minMax[1] > node.asLong() || minMax.length < 2));
    }

    /**
     * Indicates whether the specified field holds a valid decimal number.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws IllegalArgumentException if field is present, but not valid
     */
    protected boolean isDecimal(String field, FieldPresence presence, double... minMax) {
        JsonNode node = object.path(field);
        return isValid(node, presence, (node.isDouble() || node.isFloat()) &&
                (minMax.length > 0 && minMax[0] <= node.asDouble() || minMax.length < 1) &&
                (minMax.length > 1 && minMax[1] > node.asDouble() || minMax.length < 2));
    }

    /**
     * Indicates whether the specified field holds a valid boolean value.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws IllegalArgumentException if field is present, but not valid
     */
    protected boolean isBoolean(String field, FieldPresence presence) {
        JsonNode node = object.path(field);
        return isValid(node, presence, node.isBoolean());
    }

    /**
     * Indicates whether the node is present and of correct value or not
     * mandatory and absent.
     *
     * @param node         JSON node
     * @param presence     specifies if field is optional or mandatory
     * @param correctValue true if the value is correct
     * @return true if the field is as expected
     */
    private boolean isValid(JsonNode node, FieldPresence presence, boolean correctValue) {
        boolean isMandatory = presence == FieldPresence.MANDATORY;
        return isMandatory && correctValue || !isMandatory && !node.isNull() || correctValue;
    }
}
