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
 * Kubevirt Instance event class.
 */
public class KubevirtInstanceEvent extends AbstractEvent<KubevirtInstanceEvent.Type, KubevirtInstance> {

    /**
     * Creates an event of a given type for the specified kubevirt instance.
     *
     * @param type          kubevirt instance event type
     * @param subject       kubevirt instance
     */
    protected KubevirtInstanceEvent(Type type, KubevirtInstance subject) {
        super(type, subject);
    }

    /**
     * Kubevirt instance events.
     */
    public enum Type {
        /**
         * Signifies that a new kubevirt instance is created.
         */
        KUBEVIRT_INSTANCE_CREATED,

        /**
         * Signifies that the kubevirt instance is updated.
         */
        KUBEVIRT_INSTANCE_UPDATED,

        /**
         * Signifies that the kubevirt instance is removed.
         */
        KUBEVIRT_INSTANCE_REMOVED,
    }
}
