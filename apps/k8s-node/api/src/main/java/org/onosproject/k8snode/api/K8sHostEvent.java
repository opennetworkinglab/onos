/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes Kubernetes host init state event.
 */
public class K8sHostEvent extends AbstractEvent<K8sHostEvent.Type, K8sHost> {

    /**
     * Lists of kubernetes host event types.
     */
    public enum Type {

        /**
         * Signifies that new host is created.
         */
        K8S_HOST_CREATED,

        /**
         * Signifies that the host is updated.
         */
        K8S_HOST_UPDATED,

        /**
         * Signifies that the host state is completed.
         */
        K8S_HOST_COMPLETE,

        /**
         * Signifies that the host is removed.
         */
        K8S_HOST_REMOVED,

        /**
         * Signifies that the host state is incomplete.
         */
        K8S_HOST_INCOMPLETE,

        /**
         * Signifies that a set of nodes have been added into the host.
         */
        K8S_NODES_ADDED,

        /**
         * Signifies that a set of nodes have been removed from the host.
         */
        K8S_NODES_REMOVED,
    }

    /**
     * Creates an event with the given type and host.
     *
     * @param type event type
     * @param subject kubernetes host
     */
    public K8sHostEvent(Type type, K8sHost subject) {
        super(type, subject);
    }
}
