/*
 * Copyright 2017-present Open Networking Laboratory
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
            return PORT_CREATED;
        }
    },
    /**
     * Indicates required ports are added.
     */
    PORT_CREATED {
        @Override
        public void process(OpenstackNodeHandler handler, OpenstackNode osNode) {
            handler.processPortCreatedState(osNode);
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

    public abstract void process(OpenstackNodeHandler handler, OpenstackNode osNode);
    public abstract NodeState nextState();
}
