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

/**
 * Defines the initialization stats of KubeVirt node.
 */
public enum KubevirtNodeState {

    /**
     * Indicates the node is in pre-on-board.
     */
    PRE_ON_BOARD {
        @Override
        public void process(KubevirtNodeHandler handler, KubevirtNode node) {

        }

        @Override
        public KubevirtNodeState nodeState() {
            return null;
        }
    },
    /**
     * Indicates the node is on-boarded.
     */
    ON_BOARDED {
        @Override
        public void process(KubevirtNodeHandler handler, KubevirtNode node) {

        }

        @Override
        public KubevirtNodeState nodeState() {
            return null;
        }
    },
    /**
     * Indicates the node is newly added.
     */
    INIT {
        @Override
        public void process(KubevirtNodeHandler handler, KubevirtNode node) {

        }

        @Override
        public KubevirtNodeState nodeState() {
            return null;
        }
    },
    /**
     * Indicates bridge devices are added according to the node state.
     */
    DEVICE_CREATED {
        @Override
        public void process(KubevirtNodeHandler handler, KubevirtNode node) {

        }

        @Override
        public KubevirtNodeState nodeState() {
            return null;
        }
    },
    /**
     * Indicates node initialization is done.
     */
    COMPLETE {
        @Override
        public void process(KubevirtNodeHandler handler, KubevirtNode node) {

        }

        @Override
        public KubevirtNodeState nodeState() {
            return null;
        }
    },
    /**
     * Indicates node is broken.
     */
    INCOMPLETE {
        @Override
        public void process(KubevirtNodeHandler handler, KubevirtNode node) {

        }

        @Override
        public KubevirtNodeState nodeState() {
            return null;
        }
    };

    /**
     * Processes the given kubevirt node which is under a certain state.
     *
     * @param handler kubevirt node handler
     * @param node kubevirt node
     */
    public abstract void process(KubevirtNodeHandler handler, KubevirtNode node);

    /**
     * Transits to the next state.
     *
     * @return the next kubevirt node state
     */
    public abstract KubevirtNodeState nodeState();
}
