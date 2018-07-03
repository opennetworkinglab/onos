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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapId;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of an immutable openstack vTap.
 */
public final class DefaultOpenstackVtap extends AbstractDescription implements OpenstackVtap {

    private final OpenstackVtapId id;
    private final Type type;
    private final OpenstackVtapCriterion vTapCriterion;
    private final Set<DeviceId> txDeviceIds;
    private final Set<DeviceId> rxDeviceIds;

    // private constructor not intended to use from external
    private DefaultOpenstackVtap(OpenstackVtapId id, Type type,
                                 OpenstackVtapCriterion vTapCriterion,
                                 Set<DeviceId> txDeviceIds, Set<DeviceId> rxDeviceIds,
                                 SparseAnnotations... annotations) {
        super(annotations);
        this.id = id;
        this.type = type;
        this.vTapCriterion = vTapCriterion;
        this.txDeviceIds = txDeviceIds;
        this.rxDeviceIds = rxDeviceIds;
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
    public OpenstackVtapCriterion vTapCriterion() {
        return vTapCriterion;
    }

    @Override
    public Set<DeviceId> txDeviceIds() {
        return ImmutableSet.copyOf(txDeviceIds);
    }

    @Override
    public Set<DeviceId> rxDeviceIds() {
        return ImmutableSet.copyOf(rxDeviceIds);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("vTapCriterion", vTapCriterion)
                .add("txDeviceIds", txDeviceIds)
                .add("rxDeviceIds", rxDeviceIds)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, type, vTapCriterion, txDeviceIds, rxDeviceIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultOpenstackVtap that = (DefaultOpenstackVtap) o;
        return Objects.equal(this.id, that.id)
                    && Objects.equal(this.type, that.type)
                    && Objects.equal(this.vTapCriterion, that.vTapCriterion)
                    && Objects.equal(this.txDeviceIds, that.txDeviceIds)
                    && Objects.equal(this.rxDeviceIds, that.rxDeviceIds);
    }

    /**
     * Creates a new default openstack vTap builder.
     *
     * @return default openstack vTap builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DefaultOpenstackVtap object.
     */
    public static class Builder implements OpenstackVtap.Builder {
        private static final SparseAnnotations EMPTY = DefaultAnnotations.builder().build();

        private OpenstackVtapId id;
        private Type type;
        private OpenstackVtapCriterion vTapCriterion;
        private Set<DeviceId> txDeviceIds;
        private Set<DeviceId> rxDeviceIds;
        private SparseAnnotations annotations = EMPTY;

        // private constructor not intended to use from external
        Builder() {
        }

        @Override
        public Builder id(OpenstackVtapId id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder vTapCriterion(OpenstackVtapCriterion vTapCriterion) {
            this.vTapCriterion = vTapCriterion;
            return this;
        }

        @Override
        public Builder txDeviceIds(Set<DeviceId> txDeviceIds) {
            if (txDeviceIds != null) {
                this.txDeviceIds = ImmutableSet.copyOf(txDeviceIds);
            } else {
                this.txDeviceIds = Sets.newHashSet();
            }
            return this;
        }

        @Override
        public Builder rxDeviceIds(Set<DeviceId> rxDeviceIds) {
            if (rxDeviceIds != null) {
                this.rxDeviceIds = ImmutableSet.copyOf(rxDeviceIds);
            } else {
                this.rxDeviceIds = Sets.newHashSet();
            }
            return this;
        }

        @Override
        public Builder annotations(SparseAnnotations... annotations) {
            checkArgument(annotations.length <= 1,
                    "Only one set of annotations is expected");
            this.annotations = annotations.length == 1 ? annotations[0] : EMPTY;
            return this;
        }

        @Override
        public DefaultOpenstackVtap build() {
            return new DefaultOpenstackVtap(id, type, vTapCriterion,
                                            txDeviceIds, rxDeviceIds, annotations);
        }
    }

}
