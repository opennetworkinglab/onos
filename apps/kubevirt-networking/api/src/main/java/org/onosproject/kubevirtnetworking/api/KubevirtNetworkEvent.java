/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes kubevirt network service event.
 */
public class KubevirtNetworkEvent extends AbstractEvent<KubevirtNetworkEvent.Type, KubevirtNetwork> {

    public KubevirtNetworkEvent(Type type, KubevirtNetwork subject) {
        super(type, subject);
    }

    /**
     * kubevirt network events.
     */
    public enum Type {

        /**
         * Signifies that a new kubevirt network is created.
         */
        KUBEVIRT_NETWORK_CREATED,

        /**
         * Signifies that the kubevirt network is updated.
         */
        KUBEVIRT_NETWORK_UPDATED,

        /**
         * Signifies that the kubevirt network is removed.
         */
        KUBEVIRT_NETWORK_REMOVED,
    }
}
