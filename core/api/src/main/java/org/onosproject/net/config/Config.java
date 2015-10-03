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
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
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

}
