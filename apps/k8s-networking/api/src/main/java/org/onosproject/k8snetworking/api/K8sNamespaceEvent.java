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

import io.fabric8.kubernetes.api.model.Namespace;
import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes namespace event.
 */
public class K8sNamespaceEvent extends AbstractEvent<K8sNamespaceEvent.Type, Namespace> {

    /**
     * Creates an event of a given type for the specified kubernetes namespace policy.
     *
     * @param type          kubernetes namespace event type
     * @param subject       kubernetes namespace
     */
    public K8sNamespaceEvent(Type type, Namespace subject) {
        super(type, subject);
    }

    /**
     * Kubernetes namespace events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes namespace is created.
         */
        K8S_NAMESPACE_CREATED,

        /**
         * Signifies that a new kubernetes namespace is updated.
         */
        K8S_NAMESPACE_UPDATED,

        /**
         * Signifies that a new kubernetes namespace is removed.
         */
        K8S_NAMESPACE_REMOVED,
    }
}
