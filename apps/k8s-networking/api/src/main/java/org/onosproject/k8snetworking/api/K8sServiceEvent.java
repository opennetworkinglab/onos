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

import io.fabric8.kubernetes.api.model.Service;
import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes service event.
 */
public class K8sServiceEvent extends AbstractEvent<K8sServiceEvent.Type, Service> {

    /**
     * Creates an event of a given type for the specified kubernetes service.
     *
     * @param type      kubernetes service event type
     * @param subject   kubernetes service
     */
    public K8sServiceEvent(Type type, Service subject) {
        super(type, subject);
    }

    /**
     * Kubernetes service events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes service is created.
         */
        K8S_SERVICE_CREATED,

        /**
         * Signifies that the kubernetes service is updated.
         */
        K8S_SERVICE_UPDATED,

        /**
         * Signifies that the kubernetes service is removed.
         */
        K8S_SERVICE_REMOVED,
    }
}
