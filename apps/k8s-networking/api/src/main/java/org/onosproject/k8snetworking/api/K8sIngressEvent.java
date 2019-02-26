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

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes ingress event.
 */
public class K8sIngressEvent extends AbstractEvent<K8sIngressEvent.Type, Ingress> {

    /**
     * Creates an event of a given type for the specified kubernetes ingress.
     *
     * @param type      kubernetes ingress event type
     * @param subject   kubernetes ingress
     */
    public K8sIngressEvent(Type type, Ingress subject) {
        super(type, subject);
    }

    /**
     * Kubernetes ingress events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes ingress is created.
         */
        K8S_INGRESS_CREATED,

        /**
         *  Signifies that the kubernetes ingress is updated.
         */
        K8S_INGRESS_UPDATED,

        /**
         * Signifies that the kubernetes ingress is removed.
         */
        K8S_INGRESS_REMOVED,
    }
}
