/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.behaviour;

import org.onosproject.net.Annotated;
import org.onosproject.net.Description;

import java.util.Optional;

/**
 * Describes a patch interface.
 */
public interface PatchDescription extends Description, Annotated {

    /**
     * Returns the identifier of the device where this patch interface is.
     *
     * @return device identifier; empty value if not set
     */
    Optional<String> deviceId();

    /**
     * Return the name of the patch interface.
     *
     * @return patch interface name
     */
    String ifaceName();

    /**
     * Returns the name of the interface for the other side of the patch.
     *
     * @return peer patch interface name
     */
    String peer();

    /**
     * Builder of patch interface description entities.
     */
    interface Builder {

        /**
         * Returns new patch interface description.
         *
         * @return patch interface description
         */
        PatchDescription build();

        /**
         * Returns new patch interface description.
         *
         * @param deviceId device id
         * @return patch interface description builder
         */
        Builder deviceId(String deviceId);
        /**
         * Returns patch interface description builder with a given interface name.
         *
         * @param ifaceName interface name
         * @return patch interface description builder
         */
        Builder ifaceName(String ifaceName);

        /**
         * Returns patch interface description builder with a given peer.
         *
         * @param peerName peer patch interface name
         * @return patch interface description builder
         */
        Builder peer(String peerName);
    }

}
