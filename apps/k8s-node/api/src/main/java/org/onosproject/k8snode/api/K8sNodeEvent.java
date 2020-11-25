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
package org.onosproject.k8snode.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes Kubernetes node init state event.
 */
public class K8sNodeEvent extends AbstractEvent<K8sNodeEvent.Type, K8sNode> {

    /**
     * Lists of kubernetes node event types.
     */
    public enum Type {

        /**
         * Signifies that new node is created.
         */
        K8S_NODE_CREATED,

        /**
         * Signifies that the node is updated.
         */
        K8S_NODE_UPDATED,

        /**
         * Signifies that the node state is completed.
         */
        K8S_NODE_COMPLETE,

        /**
         * Signifies that the node is removed.
         */
        K8S_NODE_REMOVED,

        /**
         * Signifies that the node state is incomplete.
         */
        K8S_NODE_INCOMPLETE,

        /**
         * Signifies that the node state is off-boarded.
         */
        K8S_NODE_OFF_BOARDED
    }

    /**
     * Creates an event with the given type and node.
     *
     * @param type event type
     * @param subject kubernetes node
     */
    public K8sNodeEvent(Type type, K8sNode subject) {
        super(type, subject);
    }
}
