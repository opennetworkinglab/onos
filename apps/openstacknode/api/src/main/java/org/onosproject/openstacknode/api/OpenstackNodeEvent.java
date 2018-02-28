/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknode.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes OpenStack node init state event.
 */
public class OpenstackNodeEvent extends AbstractEvent<OpenstackNodeEvent.Type, OpenstackNode> {

    /**
     * List of openstack node event types.
     */
    public enum Type {

        /**
         * Signifies that new node is created.
         */
        OPENSTACK_NODE_CREATED,

        /**
         * Signifies that the node state is updated.
         */
        OPENSTACK_NODE_UPDATED,

        /**
         * Signifies that the node state is complete.
         */
        OPENSTACK_NODE_COMPLETE,

        /**
         * Signifies that the node state is removed.
         */
        OPENSTACK_NODE_REMOVED,

        /**
         * Signifies that the node state is changed to incomplete.
         */
        OPENSTACK_NODE_INCOMPLETE
    }

    /**
     * Creates an event with the given type and node.
     *
     * @param type event type
     * @param node openstack node
     */
    public OpenstackNodeEvent(Type type, OpenstackNode node) {
        super(type, node);
    }
}
