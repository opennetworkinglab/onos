/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapId;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of an immutable OpenstackVtap.
 */
public final class DefaultOpenstackVtap extends AbstractDescription implements OpenstackVtap {

    private final OpenstackVtapId id;
    private final Type type;
    private final OpenstackVtapCriterion vtapCriterion;
    private final Set<DeviceId> txDeviceIds;
    private final Set<DeviceId> rxDeviceIds;

    /**
     * Creates an DefaultOpenstackVtap using the supplied information.
     *
     * @param id            vtap identifier
     * @param type          type of vtap (all,rx,tx)
     * @param vtapCriterion criterion of vtap
     * @param txDeviceIds   device identifiers applied by vtap tx
     * @param rxDeviceIds   device identifiers applied by vtap rx
     * @param annotations   optional key/value annotations
     */
    private DefaultOpenstackVtap(OpenstackVtapId id,
                                 Type type,
                                 OpenstackVtapCriterion vtapCriterion,
                                 Set<DeviceId> txDeviceIds,
                                 Set<DeviceId> rxDeviceIds,
                                 SparseAnnotations... annotations) {
        super(annotations);
        this.id = checkNotNull(id);
        this.type = checkNotNull(type);
        this.vtapCriterion = checkNotNull(vtapCriterion);
        this.txDeviceIds = Objects.nonNull(txDeviceIds) ?
                ImmutableSet.copyOf(txDeviceIds) : ImmutableSet.of();
        this.rxDeviceIds = Objects.nonNull(rxDeviceIds) ?
                ImmutableSet.copyOf(rxDeviceIds) : ImmutableSet.of();
    }

    @Override
    public OpenstackVtapId id() {
        return id;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public OpenstackVtapCriterion vtapCriterion() {
        return vtapCriterion;
    }

    @Override
    public Set<DeviceId> txDeviceIds() {
        return txDeviceIds;
    }

    @Override
    public Set<DeviceId> rxDeviceIds() {
        return rxDeviceIds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, vtapCriterion, txDeviceIds, rxDeviceIds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultOpenstackVtap) {
            final DefaultOpenstackVtap other = (DefaultOpenstackVtap) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.vtapCriterion, other.vtapCriterion) &&
                    Objects.equals(this.txDeviceIds(), other.txDeviceIds()) &&
                    Objects.equals(this.rxDeviceIds(), other.rxDeviceIds()) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("type", type())
                .add("vtapCriterion", vtapCriterion())
                .add("txDeviceIds", txDeviceIds())
                .add("rxDeviceIds", rxDeviceIds())
                .add("annotations", annotations())
                .toString();
    }

    /**
     * Creates OpenstackVtap builder with default parameters.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates OpenstackVtap builder inheriting with default parameters,
     * from specified OpenstackVtap.
     *
     * @param vtap to inherit default from
     * @return builder
     */
    public static Builder builder(OpenstackVtap vtap) {
        return new Builder(vtap);
    }

    /**
     * Builder for DefaultOpenstackVtap object.
     */
    public static class Builder implements OpenstackVtap.Builder {
        private OpenstackVtapId id;
        private Type type = Type.VTAP_ALL;
        private OpenstackVtapCriterion vtapCriterion;
        private Set<DeviceId> txDeviceIds;
        private Set<DeviceId> rxDeviceIds;
        private SparseAnnotations annotations = DefaultAnnotations.EMPTY;

        // Private constructor not intended to use from external
        Builder() {
        }

        Builder(OpenstackVtap description) {
            this.id = description.id();
            this.type = description.type();
            this.vtapCriterion = description.vtapCriterion();
            this.txDeviceIds = description.txDeviceIds();
            this.rxDeviceIds = description.rxDeviceIds();
            this.annotations  = (SparseAnnotations) description.annotations();
        }

        /**
         * Sets mandatory field id.
         *
         * @param id to set
         * @return self
         */
        @Override
        public Builder id(OpenstackVtapId id) {
            this.id = id;
            return this;
        }

        /**
         * Sets mandatory field type.
         *
         * @param type of the vtap
         * @return self
         */
        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Sets mandatory field criterion.
         *
         * @param vtapCriterion for the vtap
         * @return self
         */
        @Override
        public Builder vtapCriterion(OpenstackVtapCriterion vtapCriterion) {
            this.vtapCriterion = vtapCriterion;
            return this;
        }

        /**
         * Sets a tx deviceId set.
         *
         * @param txDeviceIds deviceId set for tx
         * @return builder
         */
        @Override
        public Builder txDeviceIds(Set<DeviceId> txDeviceIds) {
            this.txDeviceIds = txDeviceIds;
            return this;
        }

        /**
         * Sets a rx deviceId set.
         *
         * @param rxDeviceIds deviceId set for rx
         * @return builder
         */
        @Override
        public Builder rxDeviceIds(Set<DeviceId> rxDeviceIds) {
            this.rxDeviceIds = rxDeviceIds;
            return this;
        }

        /**
         * Sets annotations.
         *
         * @param annotations of the vtap
         * @return self
         */
        @Override
        public Builder annotations(SparseAnnotations annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Builds a DefaultOpenstackVtap instance.
         *
         * @return DefaultOpenstackVtap
         */
        @Override
        public DefaultOpenstackVtap build() {
            return new DefaultOpenstackVtap(checkNotNull(id),
                    checkNotNull(type),
                    checkNotNull(vtapCriterion),
                    txDeviceIds,
                    rxDeviceIds,
                    checkNotNull(annotations));
        }
    }

}
