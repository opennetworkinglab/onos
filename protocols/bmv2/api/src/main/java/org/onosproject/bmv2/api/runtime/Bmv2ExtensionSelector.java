/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.KryoNamespace;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2FieldTypeModel;
import org.onosproject.bmv2.api.context.Bmv2HeaderModel;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.store.serializers.KryoNamespaces;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.*;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;

/**
 * Extension selector for BMv2 used as a wrapper for multiple BMv2 match parameters. Match parameters are
 * encoded using a map where the keys are expected to be field names formatted as {@code headerName.fieldName}
 * (e.g. {@code ethernet.dstAddr}).
 */
@Beta
public final class Bmv2ExtensionSelector extends AbstractExtension implements ExtensionSelector {

    private static final KryoNamespace APP_KRYO = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(Bmv2ExactMatchParam.class)
            .register(Bmv2TernaryMatchParam.class)
            .register(Bmv2LpmMatchParam.class)
            .register(Bmv2ValidMatchParam.class)
            .build();

    private Map<String, Bmv2MatchParam> parameterMap;

    /**
     * Creates a new BMv2 extension selector for the given match parameters map.
     *
     * @param paramMap a map
     */
    private Bmv2ExtensionSelector(Map<String, Bmv2MatchParam> paramMap) {
        this.parameterMap = paramMap;
    }

    /**
     * Returns the match parameters map of this selector.
     *
     * @return a match parameter map
     */
    public Map<String, Bmv2MatchParam> parameterMap() {
        return parameterMap;
    }


    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.BMV2_MATCH_PARAMS.type();
    }

    @Override
    public byte[] serialize() {
        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        this.parameterMap = APP_KRYO.deserialize(data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameterMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ExtensionSelector other = (Bmv2ExtensionSelector) obj;
        return Objects.equal(this.parameterMap, other.parameterMap);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        parameterMap.forEach((name, param) -> {
            switch (param.type()) {
                case EXACT:
                    Bmv2ExactMatchParam e = (Bmv2ExactMatchParam) param;
                    helper.add(name, e.value());
                    break;
                case TERNARY:
                    Bmv2TernaryMatchParam t = (Bmv2TernaryMatchParam) param;
                    helper.add(name, t.value() + "&&&" + t.mask());
                    break;
                case LPM:
                    Bmv2LpmMatchParam l = (Bmv2LpmMatchParam) param;
                    helper.add(name, l.value() + "/" + String.valueOf(l.prefixLength()));
                    break;
                case VALID:
                    Bmv2ValidMatchParam v = (Bmv2ValidMatchParam) param;
                    helper.add(name, v.flag() ? "VALID" : "NOT_VALID");
                    break;
                default:
                    helper.add(name, param);
                    break;
            }
        });
        return helper.toString();
    }

    /**
     * Returns a new, empty BMv2 extension selector.
     *
     * @return a BMv2 extension treatment
     */
    public static Bmv2ExtensionSelector empty() {
        return new Bmv2ExtensionSelector(Collections.emptyMap());
    }

    /**
     * Returns a new builder of BMv2 extension selectors.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of BMv2 extension selectors.
     * <p>
     * Match parameters are built from primitive data types ({@code short}, {@code int}, {@code long} or
     * {@code byte[]}) and automatically casted to fixed-length byte sequences according to the given BMv2
     * configuration.
     */
    public static final class Builder {

        private final Map<Pair<String, String>, Bmv2MatchParam> parameterMap = Maps.newHashMap();
        private Bmv2Configuration configuration;

        private Builder() {
            // ban constructor.
        }

        /**
         * Sets the BMv2 configuration to format the match parameters of the selector.
         *
         * @param config a BMv2 configuration
         * @return this
         */
        public Builder forConfiguration(Bmv2Configuration config) {
            this.configuration = config;
            return this;
        }

        /**
         * Adds an exact match parameter for the given header field and value.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      a short value
         * @return this
         */
        public Builder matchExact(String headerName, String fieldName, short value) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             exact(value));
            return this;
        }

        /**
         * Adds an exact match parameter for the given header field and value.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      an integer value
         * @return this
         */
        public Builder matchExact(String headerName, String fieldName, int value) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             exact(value));
            return this;
        }

        /**
         * Adds an exact match parameter for the given header field and value.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      a long value
         * @return this
         */
        public Builder matchExact(String headerName, String fieldName, long value) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             exact(value));
            return this;
        }

        /**
         * Adds an exact match parameter for the given header field and value.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      a byte array
         * @return this
         */
        public Builder matchExact(String headerName, String fieldName, byte[] value) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             exact(value));
            return this;
        }

        /**
         * Adds a ternary match parameter for the given header field, value and mask.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      a short value
         * @param mask       a short value
         * @return this
         */
        public Builder matchTernary(String headerName, String fieldName, short value, short mask) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             ternary(value, mask));
            return this;
        }
        /**
         * Adds a ternary match parameter for the given header field, value and mask.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      an integer value
         * @param mask       an integer value
         * @return this
         */
        public Builder matchTernary(String headerName, String fieldName, int value, int mask) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             ternary(value, mask));
            return this;
        }
        /**
         * Adds a ternary match parameter for the given header field, value and mask.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      a long value
         * @param mask       a long value
         * @return this
         */
        public Builder matchTernary(String headerName, String fieldName, long value, long mask) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             ternary(value, mask));
            return this;
        }
        /**
         * Adds a ternary match parameter for the given header field, value and mask.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param value      a byte array
         * @param mask       a byte array
         * @return this
         */
        public Builder matchTernary(String headerName, String fieldName, byte[] value, byte[] mask) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             ternary(value, mask));
            return this;
        }

        /**
         * Adds a longest-prefix match (LPM) parameter for the given header field, value and prefix length.
         *
         * @param headerName   a string value
         * @param fieldName    a string value
         * @param value        a short value
         * @param prefixLength an integer value
         * @return this
         */
        public Builder matchLpm(String headerName, String fieldName, short value, int prefixLength) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             lpm(value, prefixLength));
            return this;
        }
        /**
         * Adds a longest-prefix match (LPM) parameter for the given header field, value and prefix length.
         *
         * @param headerName   a string value
         * @param fieldName    a string value
         * @param value        an integer value
         * @param prefixLength an integer value
         * @return this
         */
        public Builder matchLpm(String headerName, String fieldName, int value, int prefixLength) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             lpm(value, prefixLength));
            return this;
        }
        /**
         * Adds a longest-prefix match (LPM) parameter for the given header field, value and prefix length.
         *
         * @param headerName   a string value
         * @param fieldName    a string value
         * @param value        a long value
         * @param prefixLength an integer value
         * @return this
         */
        public Builder matchLpm(String headerName, String fieldName, long value, int prefixLength) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             lpm(value, prefixLength));
            return this;
        }
        /**
         * Adds a longest-prefix match (LPM) parameter for the given header field, value and prefix length.
         *
         * @param headerName   a string value
         * @param fieldName    a string value
         * @param value        a byte array
         * @param prefixLength an integer value
         * @return this
         */
        public Builder matchLpm(String headerName, String fieldName, byte[] value, int prefixLength) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             lpm(value, prefixLength));
            return this;
        }

        /**
         * Adds a valid match parameter for the given header field.
         *
         * @param headerName a string value
         * @param fieldName  a string value
         * @param flag       a boolean value
         * @return this
         */
        public Builder matchValid(String headerName, String fieldName, boolean flag) {
            parameterMap.put(Pair.of(checkNotNull(headerName, "header name cannot be null"),
                                     checkNotNull(fieldName, "field name cannot be null")),
                             new Bmv2ValidMatchParam(flag));
            return this;
        }

        /**
         * Returns a new BMv2 extension selector.
         *
         * @return a BMv2 extension selector
         * @throws NullPointerException     if a given header or field name is not defined in the given configuration
         * @throws IllegalArgumentException if a given parameter cannot be casted for the given configuration, e.g.
         *                                  when trying to fit an integer value into a smaller, fixed-length parameter
         *                                  produces overflow.
         */
        public Bmv2ExtensionSelector build() {
            checkNotNull(configuration, "configuration cannot be null");
            checkState(parameterMap.size() > 0, "parameter map cannot be empty");

            final Map<String, Bmv2MatchParam> newParameterMap = Maps.newHashMap();

            for (Pair<String, String> key : parameterMap.keySet()) {

                String headerName = key.getLeft();
                String fieldName = key.getRight();

                Bmv2HeaderModel headerModel = configuration.header(headerName);
                checkNotNull(headerModel, "no such a header in configuration", headerName);

                Bmv2FieldTypeModel fieldModel = headerModel.type().field(fieldName);
                checkNotNull(fieldModel, "no such a field in configuration", key);

                int bitWidth = fieldModel.bitWidth();

                Bmv2MatchParam oldParam = parameterMap.get(key);
                Bmv2MatchParam newParam = null;

                try {
                    switch (oldParam.type()) {
                        case EXACT:
                            Bmv2ExactMatchParam e = (Bmv2ExactMatchParam) oldParam;
                            newParam = new Bmv2ExactMatchParam(fitByteSequence(e.value(), bitWidth));
                            break;
                        case TERNARY:
                            Bmv2TernaryMatchParam t = (Bmv2TernaryMatchParam) oldParam;
                            newParam = new Bmv2TernaryMatchParam(fitByteSequence(t.value(), bitWidth),
                                                                 fitByteSequence(t.mask(), bitWidth));
                            break;
                        case LPM:
                            Bmv2LpmMatchParam l = (Bmv2LpmMatchParam) oldParam;
                            checkArgument(l.prefixLength() <= bitWidth, "LPM parameter has prefix length too long",
                                          key);
                            newParam = new Bmv2LpmMatchParam(fitByteSequence(l.value(), bitWidth),
                                                             l.prefixLength());
                            break;
                        case VALID:
                            newParam = oldParam;
                            break;
                        default:
                            throw new RuntimeException("Match parameter type not supported: " + oldParam.type());
                    }
                } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
                    throw new IllegalArgumentException(e.getMessage() + " [" + key + "]");
                }
                // FIXME: should put the pair object instead of building a new string for the key.
                newParameterMap.put(headerName + "." + fieldName, newParam);
            }

            return new Bmv2ExtensionSelector(newParameterMap);
        }

        private static Bmv2MatchParam exact(Object value) {
            return new Bmv2ExactMatchParam(copyFrom(bb(value)));
        }

        private static Bmv2MatchParam ternary(Object value, Object mask) {
            return new Bmv2TernaryMatchParam(copyFrom(bb(value)), copyFrom(bb(mask)));
        }

        private static Bmv2MatchParam lpm(Object value, int prefixLength) {
            return new Bmv2LpmMatchParam(copyFrom(bb(value)), prefixLength);
        }

        private static ByteBuffer bb(Object value) {
            if (value instanceof Short) {
                return ByteBuffer.allocate(Short.BYTES).putShort((short) value);
            } else if (value instanceof Integer) {
                return ByteBuffer.allocate(Integer.BYTES).putInt((int) value);
            } else if (value instanceof Long) {
                return ByteBuffer.allocate(Long.BYTES).putLong((long) value);
            } else if (value instanceof byte[]) {
                byte[] bytes = (byte[]) value;
                return ByteBuffer.allocate(bytes.length).put(bytes);
            } else {
                // Never here.
                return null;
            }
        }
    }
}
