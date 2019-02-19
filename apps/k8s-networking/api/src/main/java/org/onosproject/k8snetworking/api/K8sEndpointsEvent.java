/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import io.fabric8.kubernetes.api.model.Endpoints;
import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes endpoints event.
 */
public class K8sEndpointsEvent extends AbstractEvent<K8sEndpointsEvent.Type, Endpoints> {

    /**
     * Creates an event of a given type for the specified kubernetes endpoints.
     *
     * @param type      kubernetes endpoints event type
     * @param subject   kubernetes endpoints
     */
    public K8sEndpointsEvent(Type type, Endpoints subject) {
        super(type, subject);
    }

    /**
     * Kubernetes endpoints events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes endpoints is created.
         */
        K8S_ENDPOINTS_CREATED,

        /**
         * Signifies that the kubernetes endpoints is updated.
         */
        K8S_ENDPOINTS_UPDATED,

        /**
         * Signifies that the kubernetes endpoints is removed.
         */
        K8S_ENDPOINTS_REMOVED,
    }
}
