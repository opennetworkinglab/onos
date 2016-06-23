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
import org.onlab.util.ImmutableByteSequence;
import org.onlab.util.KryoNamespace;
import org.onosproject.bmv2.api.context.Bmv2ActionModel;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2RuntimeDataModel;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.KryoNamespaces;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.BMV2_ACTION;

/**
 * Extension treatment for BMv2 used as a wrapper for a {@link Bmv2Action}.
 */
@Beta
public final class Bmv2ExtensionTreatment extends AbstractExtension implements ExtensionTreatment {

    private static final KryoNamespace APP_KRYO = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(Bmv2ExtensionTreatment.class)
            .register(Bmv2Action.class)
            .build();

    private List<String> parameterNames;
    private Bmv2Action action;

    /**
     * Creates a new extension treatment for the given BMv2 action.
     * The list of action parameters name is also required for visualization purposes (i.e. nicer toString()).
     *
     * @param action         an action
     * @param parameterNames a list of strings
     */
    private Bmv2ExtensionTreatment(Bmv2Action action, List<String> parameterNames) {
        this.action = action;
        this.parameterNames = parameterNames;
    }

    /**
     * Returns the action contained by this extension selector.
     *
     * @return an action
     */
    public Bmv2Action action() {
        return action;
    }

    @Override
    public ExtensionTreatmentType type() {
        return BMV2_ACTION.type();
    }

    @Override
    public byte[] serialize() {
        return APP_KRYO.serialize(this);
    }

    @Override
    public void deserialize(byte[] data) {
        Bmv2ExtensionTreatment other = APP_KRYO.deserialize(data);
        action = other.action;
        parameterNames = other.parameterNames;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(action);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ExtensionTreatment other = (Bmv2ExtensionTreatment) obj;
        return Objects.equal(this.action, other.action);
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", "(", ")");
        for (int i = 0; i < parameterNames.size(); i++) {
            stringJoiner.add(parameterNames.get(i) + "=" + action.parameters().get(i));
        }
        return MoreObjects.toStringHelper(this)
                .addValue(action.name() + stringJoiner.toString())
                .toString();
    }

    /**
     * Returns a new, empty BMv2 extension treatment.
     *
     * @return a BMv2 extension treatment
     */
    public static Bmv2ExtensionTreatment empty() {
        return new Bmv2ExtensionTreatment(null, Collections.emptyList());
    }

    /**
     * Returns a new BMv2 extension treatment builder.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder of BMv2 extension treatments.
     *
     * BMv2 action parameters are built from primitive data types ({@code short}, {@code int}, {@code long} or
     * {@code byte[]}) and automatically casted to fixed-length byte sequences according to the given BMv2
     * configuration.
     */
    public static final class Builder {
        private Bmv2Configuration configuration;
        private String actionName;
        private final Map<String, ImmutableByteSequence> parameters = Maps.newHashMap();

        private Builder() {
            // Ban constructor.
        }

        /**
         * Sets the BMv2 configuration to format the action parameters.
         *
         * @param config a BMv2 configuration
         * @return this
         */
        public Builder forConfiguration(Bmv2Configuration config) {
            this.configuration = config;
            return this;
        }

        /**
         * Sets the action name.
         *
         * @param actionName a string value
         * @return this
         */
        public Builder setActionName(String actionName) {
            this.actionName = actionName;
            return this;
        }

        /**
         * Adds an action parameter.
         *
         * @param parameterName a string value
         * @param value a short value
         * @return this
         */
        public Builder addParameter(String parameterName, short value) {
            this.parameters.put(parameterName, copyFrom(bb(value)));
            return this;
        }

        /**
         * Adds an action parameter.
         *
         * @param parameterName a string value
         * @param value an integer value
         * @return this
         */
        public Builder addParameter(String parameterName, int value) {
            this.parameters.put(parameterName, copyFrom(bb(value)));
            return this;
        }

        /**
         * Adds an action parameter.
         *
         * @param parameterName a string value
         * @param value a long value
         * @return this
         */
        public Builder addParameter(String parameterName, long value) {
            this.parameters.put(parameterName, copyFrom(bb(value)));
            return this;
        }

        /**
         * Adds an action parameter.
         *
         * @param parameterName a string value
         * @param value a byte array
         * @return this
         */
        public Builder addParameter(String parameterName, byte[] value) {
            this.parameters.put(parameterName, copyFrom(bb(value)));
            return this;
        }

        /**
         * Returns a new BMv2 extension treatment.
         *
         * @return a BMv2 extension treatment
         * @throws NullPointerException     if the given action or parameter names are not defined in the given
         *                                  configuration
         * @throws IllegalArgumentException if a given parameter cannot be casted for the given configuration, e.g.
         *                                  when trying to fit an integer value into a smaller, fixed-length parameter
         *                                  produces overflow.
         */
        public Bmv2ExtensionTreatment build() {
            checkNotNull(configuration, "configuration cannot be null");
            checkNotNull(actionName, "action name cannot be null");

            Bmv2ActionModel actionModel = configuration.action(actionName);

            checkNotNull(actionModel, "no such an action in configuration", actionName);
            checkArgument(actionModel.runtimeDatas().size() == parameters.size(),
                          "invalid number of parameters", actionName);

            List<ImmutableByteSequence> newParameters = new ArrayList<>(parameters.size());
            List<String> parameterNames = new ArrayList<>(parameters.size());

            for (String parameterName : parameters.keySet()) {
                Bmv2RuntimeDataModel runtimeData = actionModel.runtimeData(parameterName);
                checkNotNull(runtimeData, "no such an action parameter in configuration",
                             actionName + "->" + runtimeData.name());
                int bitWidth = runtimeData.bitWidth();
                try {
                    ImmutableByteSequence newSequence = fitByteSequence(parameters.get(parameterName), bitWidth);
                    int idx = actionModel.runtimeDatas().indexOf(runtimeData);
                    newParameters.add(idx, newSequence);
                    parameterNames.add(idx, parameterName);
                } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
                    throw new IllegalArgumentException(e.getMessage() +
                                                               " [" + actionName + "->" + runtimeData.name() + "]");
                }
            }

            return new Bmv2ExtensionTreatment(new Bmv2Action(actionName, newParameters), parameterNames);
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
