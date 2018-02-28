/*
 * Copyright 2017-present Open Networking Foundation
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

/**
 * Defines the initialization states of OpenStack node.
 */
public enum NodeState {

    /**
     * Indicates the node is newly added.
     */
    INIT {
        @Override
        public void process(OpenstackNodeHandler handler, OpenstackNode osNode) {
            handler.processInitState(osNode);
        }

        @Override
        public NodeState nextState() {
            return DEVICE_CREATED;
        }
    },
    /**
     * Indicates bridge devices are added according to the node state.
     */
    DEVICE_CREATED {
        @Override
        public void process(OpenstackNodeHandler handler, OpenstackNode osNode) {
            handler.processDeviceCreatedState(osNode);
        }

        @Override
        public NodeState nextState() {
            return COMPLETE;
        }
    },
    /**
     * Indicates node initialization is done.
     */
    COMPLETE {
        @Override
        public void process(OpenstackNodeHandler handler, OpenstackNode osNode) {
            handler.processCompleteState(osNode);
        }

        @Override
        public NodeState nextState() {
            return COMPLETE;
        }

    },
    /**
     * Indicates node is broken.
     */
    INCOMPLETE {
        @Override
        public void process(OpenstackNodeHandler handler, OpenstackNode osNode) {
            handler.processIncompleteState(osNode);
        }

        @Override
        public NodeState nextState() {
            return INIT;
        }
    };

    /**
     * Processes the given node which is under a certain state.
     *
     * @param handler openstack node handler
     * @param osNode openstack node
     */
    public abstract void process(OpenstackNodeHandler handler, OpenstackNode osNode);

    /**
     * Transits to the next state.
     *
     * @return the next openstack node state
     */
    public abstract NodeState nextState();
}
