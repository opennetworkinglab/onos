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

/**
 * Defines the initialization state of Kubernetes host.
 */
public enum K8sHostState {

    /**
     * Indicates the host is newly added.
     */
    INIT {
        @Override
        public void process(K8sHostHandler handler, K8sHost host) {
            handler.processInitState(host);
        }

        @Override
        public K8sHostState nextState() {
            return DEVICE_CREATED;
        }
    },
    /**
     * Indicates bridge devices are added according to the host state.
     */
    DEVICE_CREATED {
        @Override
        public void process(K8sHostHandler handler, K8sHost host) {
            handler.processDeviceCreatedState(host);
        }

        @Override
        public K8sHostState nextState() {
            return COMPLETE;
        }
    },
    /**
     * Indicates the host initialization is done.
     */
    COMPLETE {
        @Override
        public void process(K8sHostHandler handler, K8sHost host) {
            handler.processCompleteState(host);
        }

        @Override
        public K8sHostState nextState() {
            return COMPLETE;
        }
    },
    /**
     * Indicates host is broken.
     */
    INCOMPLETE {
        @Override
        public void process(K8sHostHandler handler, K8sHost host) {
            handler.processIncompleteState(host);
        }

        @Override
        public K8sHostState nextState() {
            return INIT;
        }
    };

    /**
     * Processes the given host which is under a certain state.
     *
     * @param handler kubernetes host handler
     * @param host kubernetes host
     */
    public abstract void process(K8sHostHandler handler, K8sHost host);

    /**
     * Transits to the next state.
     *
     * @return the next kubernetes host state
     */
    public abstract K8sHostState nextState();
}
