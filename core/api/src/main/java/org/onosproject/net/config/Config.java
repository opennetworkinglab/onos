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
package org.onosproject.net.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.ConnectPoint;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

    private static final String TRUE_LITERAL = "true";
    private static final String FALSE_LITERAL = "false";

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
     * @param delegate delegate context, or null for detached configs.
     */
    public final void init(S subject, String key, JsonNode node, ObjectMapper mapper,
                     ConfigApplyDelegate delegate) {
        this.subject = checkNotNull(subject, "Subject cannot be null");
        this.key = key;
        this.node = checkNotNull(node, "Node cannot be null");
        this.object = node instanceof ObjectNode ? (ObjectNode) node : null;
        this.array = node instanceof ArrayNode ? (ArrayNode) node : null;
        this.mapper = checkNotNull(mapper, "Mapper cannot be null");
        this.delegate = delegate;
    }

    /**
     * Indicates whether or not the backing JSON node contains valid data.
     * <p>
     * Default implementation returns true.
     * Subclasses are expected to override this with their own validation.
     * Implementations are free to throw a RuntimeException if data is invalid.
     * </p>
     *
     * @return true if the data is valid; false otherwise
     * @throws RuntimeException if configuration is invalid or completely foobar
     */
    public boolean isValid() {
        // Derivatives should use the provided set of predicates to test
        // validity of their fields, e.g.:
        //      isString(path)
        //      isBoolean(path)
        //      isNumber(path, [min, max])
        //      isIntegralNumber(path, [min, max])
        //      isDecimal(path, [min, max])
        //      isMacAddress(path)
        //      isIpAddress(path)
        //      isIpPrefix(path)
        //      isConnectPoint(path)
        //      isTpPort(path)
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
     * <p>
     * Not effective for detached configs.
     * </p>
     */
    public void apply() {
        checkState(delegate != null, "Cannot apply detached config");
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
     * Clears the specified property.
     *
     * @param name  property name
     * @return self
     */
    protected Config<S> clear(String name) {
        object.remove(name);
        return this;
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
        if (defaultValue != null) {
            Enum.valueOf(enumClass, object.path(name).asText(defaultValue.toString()));
        }

        JsonNode node = object.get(name);
        return node == null ? null : Enum.valueOf(enumClass, node.asText());
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
     * @return true if only allowedFields are present; false otherwise
     */
    protected boolean hasOnlyFields(String... allowedFields) {
        return hasOnlyFields(object, allowedFields);
    }

    /**
     * Indicates whether only the specified fields are present in a particular
     * JSON object.
     *
     * @param node node whose fields to check
     * @param allowedFields allowed field names
     * @return true if only allowedFields are present; false otherwise
     */
    protected boolean hasOnlyFields(ObjectNode node, String... allowedFields) {
        Set<String> fields = ImmutableSet.copyOf(allowedFields);
        node.fieldNames().forEachRemaining(f -> {
            if (!fields.contains(f)) {
                throw new InvalidFieldException(f, "Field is not allowed");
            }
        });
        return true;
    }

    /**
     * Indicates whether all specified fields are present in the backing JSON.
     *
     * @param mandatoryFields mandatory field names
     * @return true if all mandatory fields are present; false otherwise
     */
    protected boolean hasFields(String... mandatoryFields) {
        return hasFields(object, mandatoryFields);
    }

    /**
     * Indicates whether all specified fields are present in a particular
     * JSON object.
     *
     * @param node node whose fields to check
     * @param mandatoryFields mandatory field names
     * @return true if all mandatory fields are present; false otherwise
     */
    protected boolean hasFields(ObjectNode node, String... mandatoryFields) {
        Set<String> fields = ImmutableSet.copyOf(mandatoryFields);
        fields.forEach(f -> {
            if (node.path(f).isMissingNode()) {
                throw new InvalidFieldException(f, "Mandatory field is not present");
            }
        });
        return true;
    }

    /**
     * Indicates whether the specified field holds a valid MAC address.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isMacAddress(String field, FieldPresence presence) {
        return isMacAddress(object, field, presence);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * MAC address.
     *
     * @param objectNode JSON node
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isMacAddress(ObjectNode objectNode, String field, FieldPresence presence) {
        return isValid(objectNode, field, presence, n -> {
            MacAddress.valueOf(n.asText());
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid IP address.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isIpAddress(String field, FieldPresence presence) {
        return isIpAddress(object, field, presence);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * IP address.
     *
     * @param objectNode     node from whom to access the field
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isIpAddress(ObjectNode objectNode, String field, FieldPresence presence) {
        return isValid(objectNode, field, presence, n -> {
            IpAddress.valueOf(n.asText());
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid IP prefix.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isIpPrefix(String field, FieldPresence presence) {
        return isIpPrefix(object, field, presence);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * IP prefix.
     *
     * @param objectNode     node from whom to access the field
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isIpPrefix(ObjectNode objectNode, String field, FieldPresence presence) {
        return isValid(objectNode, field, presence, n -> {
            IpPrefix.valueOf(n.asText());
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid transport layer port.
     *
     * @param field JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isTpPort(String field, FieldPresence presence) {
        return isTpPort(object, field, presence);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * transport layer port.
     *
     * @param objectNode node from whom to access the field
     * @param field JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isTpPort(ObjectNode objectNode, String field, FieldPresence presence) {
        return isValid(objectNode, field, presence, n -> {
            TpPort.tpPort(n.asInt());
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid connect point string.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isConnectPoint(String field, FieldPresence presence) {
        return isConnectPoint(object, field, presence);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * connect point string.
     *
     * @param objectNode JSON node
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isConnectPoint(ObjectNode objectNode, String field, FieldPresence presence) {
        return isValid(objectNode, field, presence, n -> {
            ConnectPoint.deviceConnectPoint(n.asText());
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid string value.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param pattern  optional regex pattern
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isString(String field, FieldPresence presence, String... pattern) {
        return isString(object, field, presence, pattern);
    }

    /**
     * Indicates whether the specified field on a particular node holds a valid
     * string value.
     *
     * @param objectNode JSON node
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param pattern  optional regex pattern
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isString(ObjectNode objectNode, String field,
                               FieldPresence presence, String... pattern) {
        return isValid(objectNode, field, presence, (node) -> {
            if (!(node.isTextual() &&
                    (pattern.length > 0 && node.asText().matches(pattern[0]) || pattern.length < 1))) {
                fail("Invalid string value");
            }
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid number.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isNumber(String field, FieldPresence presence, long... minMax) {
        return isNumber(object, field, presence, minMax);
    }

    /**
     * Indicates whether the specified field of a particular node holds a
     * valid number.
     *
     * @param objectNode JSON object
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isNumber(ObjectNode objectNode, String field,
                               FieldPresence presence, long... minMax) {
        return isValid(objectNode, field, presence, n -> {
            long number = (n.isNumber()) ? n.asLong() : Long.parseLong(n.asText());
            if (minMax.length > 1) {
                verifyRange(number, minMax[0], minMax[1]);
            } else if (minMax.length > 0) {
                verifyRange(number, minMax[0]);
            }
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid integer.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isIntegralNumber(String field, FieldPresence presence, long... minMax) {
        return isIntegralNumber(object, field, presence, minMax);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * integer.
     *
     * @param objectNode JSON node
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isIntegralNumber(ObjectNode objectNode, String field,
                                       FieldPresence presence, long... minMax) {
        return isValid(objectNode, field, presence, n -> {
            long number = (n.isIntegralNumber()) ? n.asLong() : Long.parseLong(n.asText());
            if (minMax.length > 1) {
                verifyRange(number, minMax[0], minMax[1]);
            } else if (minMax.length > 0) {
                verifyRange(number, minMax[0]);
            }
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid decimal number.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isDecimal(String field, FieldPresence presence, double... minMax) {
        return isDecimal(object, field, presence, minMax);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * decimal number.
     *
     * @param objectNode JSON node
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @param minMax   optional min/max values
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isDecimal(ObjectNode objectNode, String field,
                                FieldPresence presence, double... minMax) {
        return isValid(objectNode, field, presence, n -> {
            double number = (n.isDouble()) ? n.asDouble() : Double.parseDouble(n.asText());
            if (minMax.length > 1) {
                verifyRange(number, minMax[0], minMax[1]);
            } else if (minMax.length > 0) {
                verifyRange(number, minMax[0]);
            }
            return true;
        });
    }

    /**
     * Indicates whether the specified field holds a valid boolean value.
     *
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isBoolean(String field, FieldPresence presence) {
        return isBoolean(object, field, presence);
    }

    /**
     * Indicates whether the specified field of a particular node holds a valid
     * boolean value.
     *
     * @param objectNode JSON object node
     * @param field    JSON field name
     * @param presence specifies if field is optional or mandatory
     * @return true if valid; false otherwise
     * @throws InvalidFieldException if the field is present but not valid
     */
    protected boolean isBoolean(ObjectNode objectNode, String field, FieldPresence presence) {
        return isValid(objectNode, field, presence, n -> {
            if (!(n.isBoolean() || (n.isTextual() && isBooleanString(n.asText())))) {
                fail("Field is not a boolean value");
            }
            return true;
        });
    }

    /**
     * Indicates whether a string holds a boolean literal value.
     *
     * @param str string to test
     * @return true if the string contains "true" or "false" (case insensitive),
     * otherwise false
     */
    private boolean isBooleanString(String str) {
        return str.equalsIgnoreCase(TRUE_LITERAL) || str.equalsIgnoreCase(FALSE_LITERAL);
    }

    /**
     * Indicates whether a field in the node is present and of correct value or
     * not mandatory and absent.
     *
     * @param objectNode JSON object node containing field to validate
     * @param field name of field to validate
     * @param presence specified if field is optional or mandatory
     * @param validationFunction function which can be used to verify if the
     *                           node has the correct value
     * @return true if the field is as expected
     * @throws InvalidFieldException if the field is present but not valid
     */
    private boolean isValid(ObjectNode objectNode, String field, FieldPresence presence,
                            Function<JsonNode, Boolean> validationFunction) {
        JsonNode node = objectNode.path(field);
        boolean isMandatory = presence == FieldPresence.MANDATORY;
        if (isMandatory && node.isMissingNode()) {
            throw new InvalidFieldException(field, "Mandatory field not present");
        }

        if (!isMandatory && (node.isNull() || node.isMissingNode())) {
            return true;
        }

        try {
            if (validationFunction.apply(node)) {
                return true;
            } else {
                throw new InvalidFieldException(field, "Validation error");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException(field, e);
        }
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message);
    }

    private static <N extends Comparable> void verifyRange(N num, N min) {
        if (num.compareTo(min) < 0) {
            fail("Field must be greater than " + min);
        }
    }

    private static <N extends Comparable> void verifyRange(N num, N min, N max) {
        verifyRange(num, min);

        if (num.compareTo(max) > 0) {
            fail("Field must be less than " + max);
        }
    }

}
