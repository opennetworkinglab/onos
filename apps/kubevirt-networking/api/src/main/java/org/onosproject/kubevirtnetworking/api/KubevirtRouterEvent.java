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
 * Kubevirt router event class.
 */
public class KubevirtRouterEvent extends AbstractEvent<KubevirtRouterEvent.Type, KubevirtRouter> {

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type      kubevirt router event type
     * @param subject   kubevirt router
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject) {
        super(type, subject);
    }

    public enum Type {
        /**
         * Signifies that a new kubevirt router is created.
         */
        KUBEVIRT_ROUTER_CREATED,

        /**
         * Signifies that the kubevirt router is updated.
         */
        KUBEVIRT_ROUTER_UPDATED,

        /**
         * Signifies that the kubevirt router is removed.
         */
        KUBEVIRT_ROUTER_REMOVED,
    }
}
