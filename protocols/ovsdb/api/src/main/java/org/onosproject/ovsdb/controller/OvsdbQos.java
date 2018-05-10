/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ovsdb.controller;

import com.google.common.collect.Maps;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.QosDescription;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.ovsdb.controller.OvsdbConstant.CBS;
import static org.onosproject.ovsdb.controller.OvsdbConstant.CIR;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MAX_RATE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_EGRESS_POLICER;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_EXTERNAL_ID_KEY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_TYPE_PREFIX;

/**
 * The class representing an OVSDB Qos.
 * This class is immutable.
 */
public final class OvsdbQos {

    private final String type;
    private Optional<Map<Long, String>> queues;
    private Map<String, String> otherConfigs;
    private Map<String, String> externalIds;

    /**
     * Creates an OvsdbQos using the given inputs.
     *
     * @param type the type of the qos
     * @param queues rate queues
     * @param otherConfigs Key-value pairs for configuring rarely used features
     * @param externalIds Key-value pairs for use by external frameworks, rather than by OVS itself
     */
    private OvsdbQos(String type, Optional<Map<Long, String>> queues,
                     Map<String, String> otherConfigs,
                     Map<String, String> externalIds) {

        this.type = checkNotNull(type);
        this.queues = queues;
        this.otherConfigs = otherConfigs;
        this.externalIds = externalIds;
    }

    /**
     * Returns the type of qos.
     *
     * @return the type of qos
     */
    public String qosType() {
        return type;
    }

    /**
     * Returns the map of queues.
     *
     * @return the queues.
     */
    public Optional<Map<Long, String>> qosQueues() {
        return queues;
    }

    /**
     * Returns other configurations of the qos.
     *
     * @return map of configurations
     */
    public Map<String, String> otherConfigs() {
        return otherConfigs;
    }

    /**
     * Returns the optional external ids.
     *
     * @return the external ids.
     */
    public Map<String, String> externalIds() {
        return externalIds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, queues, externalIds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbQos) {
            final OvsdbQos other = (OvsdbQos) obj;
            return Objects.equals(this.type, other.type) &&
                    Objects.equals(this.otherConfigs, other.otherConfigs) &&
                    Objects.equals(this.queues, other.queues) &&
                    Objects.equals(this.externalIds, other.externalIds);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("qosType", qosType())
                .add("qosQueues", qosQueues())
                .add("otherConfigs", otherConfigs())
                .add("externalIds", externalIds())
                .toString();
    }

    /**
     * Returns new OVSDB qos builder.
     *
     * @return ovsdb qos builder
     */
    public static OvsdbQos.Builder builder() {
        return new OvsdbQos.Builder();
    }

    /**
     * Returns new OVSDB qos builder with Qos description.
     *
     * @param qosDesc qos description
     * @return ovsdb qos builder
     */
    public static Builder builder(QosDescription qosDesc) {
        return new Builder(qosDesc);
    }

    /**
     * Builder of OVSDB qos entities.
     */
    public static final class Builder {
        private String type;
        private Optional<Map<Long, String>> queues = Optional.empty();
        private Map<String, String> otherConfigs = Maps.newHashMap();
        private Map<String, String> externalIds = Maps.newHashMap();

        /**
         * Constructs an empty builder.
         */
        private Builder() {

        }

        /**
         * Constructs a builder with a given Qos description.
         *
         * @param qosDesc Qos description
         */
        private Builder(QosDescription qosDesc) {
            if (qosDesc.maxRate().isPresent()) {
                otherConfigs.put(MAX_RATE, String.valueOf((long) qosDesc.maxRate().get().bps()));
            }
            if (qosDesc.cir().isPresent()) {
                otherConfigs.put(CIR, qosDesc.cir().get().toString());
            }
            if (qosDesc.cbs().isPresent()) {
                otherConfigs.put(CBS, qosDesc.cbs().get().toString());
            }

            if (qosDesc.queues().isPresent()) {
                Map<Long, String> map = new HashMap<>();
                qosDesc.queues().get().forEach((k, v) -> map.put(k, v.queueId().name()));
                queues = Optional.ofNullable(map);
            }
            type = qosDesc.type() == QosDescription.Type.EGRESS_POLICER ?
                    QOS_EGRESS_POLICER :
                    QOS_TYPE_PREFIX.concat(qosDesc.type().name().toLowerCase());
            externalIds.putAll(((DefaultAnnotations) qosDesc.annotations()).asMap());
            externalIds.put(QOS_EXTERNAL_ID_KEY, qosDesc.qosId().name());
        }

        /**
         * Returns new OVSDB qos.
         *
         * @return ovsdb qos
         */
        public OvsdbQos build() {
            return new OvsdbQos(type, queues, otherConfigs, externalIds);
        }

        /**
         * Returns OVSDB qos builder with a given type.
         *
         * @param type name of the qos
         * @return ovsdb qos builder
         */
        public Builder qosType(String type) {
            this.type = type;
            return this;
        }

        /**
         * Returns OVSDB qos builder with a given queues.
         *
         * @param queues the map of queue
         * @return ovsdb qos builder
         */
        public Builder queues(Map<Long, String> queues) {
            this.queues = Optional.ofNullable(queues);
            return this;
        }

        /**
         * Returns OVSDB qos builder with given configs.
         *
         * @param otherConfigs other configs
         * @return ovsdb qos builder
         */
        public Builder otherConfigs(Map<String, String> otherConfigs) {
            this.otherConfigs = Maps.newHashMap(otherConfigs);
            return this;
        }

        /**
         * Returns OVSDB qos builder with given external ids.
         *
         * @param ids the external ids
         * @return ovsdb qos builder
         */
        public Builder externalIds(Map<String, String> ids) {
            this.externalIds = ids;
            return this;
        }

    }

}
