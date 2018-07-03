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
package org.onosproject.openstackvtap.api;

import org.onosproject.net.Annotated;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;

import java.util.Set;

/**
 * Abstraction of an OpenstackVtap.
 */
public interface OpenstackVtap extends Annotated {

    /**
     * Openstack vTap type.
     */
    enum Type {

        /**
         * Indicates mirroring both TX and RX traffic.
         */
        VTAP_ALL(0x3),

        /**
         * Indicates only mirroring RX traffic.
         */
        VTAP_RX(0x2),

        /**
         * Indicates only mirroring TX traffic.
         */
        VTAP_TX(0x1),

        /**
         * Indicates do not mirroring any traffic.
         */
        VTAP_NONE(0);

        private final int type;

        Type(int type) {
            this.type = type;
        }

        public boolean isValid(Type comp) {
            return (this.type & comp.type) == comp.type;
        }
    }

    /**
     * OpenstackVtap identifier.
     *
     * @return OpenstackVtap id
     */
    OpenstackVtapId id();

    /**
     * Returns OpenstackVtap type.
     *
     * @return type
     */
    Type type();

    /**
     * Returns the vTap criterion.
     *
     * @return vTap criterion
     */
    OpenstackVtapCriterion vTapCriterion();

    /**
     * Returns a collection of TX device identifiers.
     *
     * @return device identifiers
     */
    Set<DeviceId> txDeviceIds();

    /**
     * Returns a collection of RX device identifiers.
     *
     * @return device identifiers
     */
    Set<DeviceId> rxDeviceIds();

    /**
     * Builder of new openstack vTap instance.
     */
    interface Builder {

        /**
         * Returns openstack vTap builder with supplied vTap identifier.
         *
         * @param id vTap identifier
         * @return openstack vTap builder
         */
        Builder id(OpenstackVtapId id);

        /**
         * Returns openstack vTap builder with supplied vTap type.
         *
         * @param type vTap type
         * @return openstack vTap builder
         */
        Builder type(OpenstackVtap.Type type);

        /**
         * Returns openstack vTap builder with supplied vTap criterion.
         *
         * @param vTapCriterion vTap criterion
         * @return openstack vTap builder
         */
        Builder vTapCriterion(OpenstackVtapCriterion vTapCriterion);

        /**
         * Returns openstack vTap builder with supplied TX device identifiers.
         *
         * @param txDeviceIds TX device identifiers
         * @return openstack vTap builder
         */
        Builder txDeviceIds(Set<DeviceId> txDeviceIds);

        /**
         * Returns openstack vTap builder with supplied RX device identifiers.
         *
         * @param rxDeviceIds RX device identifiers
         * @return openstack vTap builder
         */
        Builder rxDeviceIds(Set<DeviceId> rxDeviceIds);

        /**
         * Returns openstack vTap builder with supplied annotations.
         *
         * @param annotations a set of annotations
         * @return openstack vTap builder
         */
        Builder annotations(SparseAnnotations... annotations);

        /**
         * Builds an immutable openstack vTap instance.
         *
         * @return openstack vTap instance
         */
        OpenstackVtap build();
    }
}
