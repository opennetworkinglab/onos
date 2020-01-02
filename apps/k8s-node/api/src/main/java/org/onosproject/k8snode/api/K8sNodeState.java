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

/**
 * Defines the initialization stats of Kubernetes node.
 */
public enum K8sNodeState {

    /**
     * Indicates the node is in pre-on-board.
     */
    PRE_ON_BOARD {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processPreOnBoardState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return ON_BOARDED;
        }
    },
    /**
     * Indicates the node is on-boarded.
     */
    ON_BOARDED {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processOnBoardedState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return ON_BOARDED;
        }
    },
    /**
     * Indicates the node is post-on-board.
     */
    POST_ON_BOARD {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processPostOnBoardState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return POST_ON_BOARD;
        }
    },
    /**
     * Indicates the node is newly added.
     */
    INIT {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processInitState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return DEVICE_CREATED;
        }
    },
    /**
     * Indicates bridge devices are added according to the node state.
     */
    DEVICE_CREATED {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processDeviceCreatedState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return COMPLETE;
        }
    },
    /**
     * Indicates node initialization is done.
     */
    COMPLETE {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processCompleteState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return COMPLETE;
        }
    },
    /**
     * Indicates node is broken.
     */
    INCOMPLETE {
        @Override
        public void process(K8sNodeHandler handler, K8sNode node) {
            handler.processIncompleteState(node);
        }

        @Override
        public K8sNodeState nextState() {
            return INIT;
        }
    };

    /**
     * Processes the given node which is under a certain state.
     *
     * @param handler kubernetes node handler
     * @param node kubernetes node
     */
    public abstract void process(K8sNodeHandler handler, K8sNode node);

    /**
     * Transits to the next state.
     *
     * @return the next kubernetes node state
     */
    public abstract K8sNodeState nextState();
}
