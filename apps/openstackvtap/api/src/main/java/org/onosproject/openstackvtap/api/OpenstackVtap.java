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
 * Abstraction of an openstack vtap.
 */
public interface OpenstackVtap extends Annotated {

    /**
     * List of openstack vtap types.
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
         * Indicates selection of mirroring any traffic.
         */
        VTAP_ANY(0x0);

        private final int type;

        Type(int type) {
            this.type = type;
        }

        public boolean isValid(Type comp) {
            return (this.type & comp.type) == comp.type;
        }
    }

    /**
     * Returns the openstack vtap identifier.
     *
     * @return vtap ID
     */
    OpenstackVtapId id();

    /**
     * Returns the openstack vtap type.
     *
     * @return type of vtap
     */
    Type type();

    /**
     * Returns the openstack vtap criterion.
     *
     * @return criterion of vtap
     */
    OpenstackVtapCriterion vtapCriterion();

    /**
     * Returns a collection of device identifiers for tx.
     *
     * @return device identifiers for tx
     */
    Set<DeviceId> txDeviceIds();

    /**
     * Returns a collection of device identifiers for rx.
     *
     * @return device identifiers for rx
     */
    Set<DeviceId> rxDeviceIds();

    /**
     * Builder of new openstack vtap instance.
     */
    interface Builder {
        /**
         * Returns openstack vtap builder with supplied id.
         *
         * @param id openstack vtap id
         * @return openstack vtap builder
         */
        Builder id(OpenstackVtapId id);

        /**
         * Returns openstack vtap builder with supplied type.
         *
         * @param type of the vtap
         * @return openstack vtap builder
         */
        Builder type(OpenstackVtap.Type type);

        /**
         * Returns openstack vtap builder with supplied criterion.
         *
         * @param vtapCriterion for the vtap
         * @return openstack vtap builder
         */
        Builder vtapCriterion(OpenstackVtapCriterion vtapCriterion);

        /**
         * Returns openstack vtap builder with supplied tx deviceId set.
         *
         * @param txDeviceIds deviceId set for tx
         * @return openstack vtap builder
         */
        Builder txDeviceIds(Set<DeviceId> txDeviceIds);

        /**
         * Returns openstack vtap builder with supplied rx deviceId set.
         *
         * @param rxDeviceIds deviceId set for rx
         * @return openstack vtap builder
         */
        Builder rxDeviceIds(Set<DeviceId> rxDeviceIds);

        /**
         * Returns openstack vtap builder with supplied annotations.
         *
         * @param annotations a set of annotations
         * @return openstack vtap builder
         */
        Builder annotations(SparseAnnotations annotations);

        /**
         * Builds an immutable OpenstackVtap instance.
         *
         * @return openstack vtap instance
         */
        OpenstackVtap build();
    }
}
