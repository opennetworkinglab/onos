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
import org.onosproject.net.behaviour.QueueDescription;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.ovsdb.controller.OvsdbConstant.BURST;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MAX_RATE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MIN_RATE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.PRIORITY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QUEUE_EXTERNAL_ID_KEY;

/**
 * The class representing an OVSDB Queue.
 * This class is immutable.
 */
public final class OvsdbQueue {
    private final Optional<Long> dscp;
    private final Map<String, String> otherConfigs;
    private final Map<String, String> externalIds;

    /**
     * Creates an OvsdbQueue using the given inputs.
     *
     * @param dscp the dscp of queue
     * @param otherConfigs Key-value pairs for configuring rarely used features
     * @param externalIds Key-value pairs for use by external frameworks, rather than by OVS itself
     */
    private OvsdbQueue(Optional<Long> dscp, Map<String, String> otherConfigs,
                       Map<String, String> externalIds) {
        this.dscp = dscp;
        this.otherConfigs = otherConfigs;
        this.externalIds = externalIds;
    }

    /**
     * Returns the dscp of queue.
     *
     * @return the dscp
     */
    public Optional<Long> dscp() {
        return dscp;
    }

    /**
     * Returns other configurations of the queue.
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
        return Objects.hash(dscp, otherConfigs, externalIds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbQueue) {
            final OvsdbQueue other = (OvsdbQueue) obj;
            return Objects.equals(this.dscp, other.dscp) &&
                    Objects.equals(this.otherConfigs, other.otherConfigs) &&
                    Objects.equals(this.externalIds, other.externalIds);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("dscp", dscp())
                .add("maxRate", otherConfigs())
                .add("externalIds", externalIds())
                .toString();
    }

    /**
     * Returns new OVSDB queue builder.
     *
     * @return ovsdb queue builder
     */
    public static OvsdbQueue.Builder builder() {
        return new OvsdbQueue.Builder();
    }

    /**
     * Returns new OVSDB queue builder with queue description.
     *
     * @param queueDescription queue description
     * @return ovsdb queue builder
     */
    public static Builder builder(QueueDescription queueDescription) {
        return new Builder(queueDescription);
    }

    /**
     * Builder of OVSDB queue entities.
     */
    public static final class Builder {
        private Optional<Long> dscp = Optional.empty();
        private Map<String, String> otherConfigs = Maps.newHashMap();
        private Map<String, String> externalIds  = Maps.newHashMap();

        /**
         * Constructs an empty builder.
         */
        private Builder() {

        }

        /**
         * Constructs a builder with a given queue description.
         *
         * @param queueDescription queue description
         */
        private Builder(QueueDescription queueDescription) {
            if (queueDescription.maxRate().isPresent()) {
                otherConfigs.put(MAX_RATE, String.valueOf((long) queueDescription.maxRate().get().bps()));
            }
            if (queueDescription.minRate().isPresent()) {
                otherConfigs.put(MIN_RATE, String.valueOf((long) queueDescription.minRate().get().bps()));
            }
            if (queueDescription.burst().isPresent()) {
                otherConfigs.put(BURST, queueDescription.burst().get().toString());
            }
            if (queueDescription.priority().isPresent()) {
                otherConfigs.put(PRIORITY, queueDescription.priority().get().toString());
            }
            if (queueDescription.dscp().isPresent()) {
                dscp = Optional.of(queueDescription.dscp().get().longValue());
            }

            externalIds.putAll(((DefaultAnnotations) queueDescription.annotations()).asMap());
            externalIds.put(QUEUE_EXTERNAL_ID_KEY, queueDescription.queueId().name());
        }

        /**
         * Returns new OVSDB queue.
         *
         * @return ovsdb queue
         */
        public OvsdbQueue build() {
            return new OvsdbQueue(dscp, otherConfigs, externalIds);
        }

        /**
         * Returns OVSDB queue builder with dscp.
         *
         * @param dscp dscp
         * @return ovsdb queue builder
         */
        public Builder dscp(Long dscp) {
            this.dscp = Optional.ofNullable(dscp);
            return this;
        }

        /**
         * Returns OVSDB queue builder with given configs.
         *
         * @param otherConfigs other configs
         * @return ovsdb queue builder
         */
        public Builder otherConfigs(Map<String, String> otherConfigs) {
            this.otherConfigs = Maps.newHashMap(otherConfigs);
            return this;
        }

        /**
         * Returns OVSDB queue builder with given external ids.
         *
         * @param ids the external ids
         * @return ovsdb queue builder
         */
        public Builder externalIds(Map<String, String> ids) {
            this.externalIds = ids;
            return this;
        }

    }

}