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
package org.onosproject.kubevirtnode.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes Kubevirt node init state event.
 */
public class KubevirtNodeEvent extends AbstractEvent<KubevirtNodeEvent.Type, KubevirtNode> {

    /**
     * List of kubevirt node event types.
     */
    public enum Type {

        /**
         * Signifies that new node is created.
         */
        KUBEVIRT_NODE_CREATED,

        /**
         * Signifies that the node state is updated.
         */
        KUBEVIRT_NODE_UPDATED,

        /**
         * Signifies that the node state is complete.
         */
        KUBEVIRT_NODE_COMPLETE,

        /**
         * Signifies that the node state is removed.
         */
        KUBEVIRT_NODE_REMOVED,

        /**
         * Signifies that the node state is changed to incomplete.
         */
        KUBEVIRT_NODE_INCOMPLETE
    }

    public KubevirtNodeEvent(Type type, KubevirtNode subject) {
        super(type, subject);
    }
}
