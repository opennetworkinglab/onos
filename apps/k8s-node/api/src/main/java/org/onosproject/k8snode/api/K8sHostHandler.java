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
 * Service handling kubernetes host stats.
 */
public interface K8sHostHandler {

    /**
     * Processes the given host for init state.
     * It creates required bridges on OVS by referring to host type.zw
     *
     * @param k8sHost kubernetes host
     */
    void processInitState(K8sHost k8sHost);

    /**
     * Processes the given node for device created state.
     * It creates required ports on the bridges based on the node type.
     *
     * @param k8sHost kubernetes host
     */
    void processDeviceCreatedState(K8sHost k8sHost);

    /**
     * Processes the given host for complete state.
     * It performs post-init jobs for the complete host.
     *
     * @param k8sHost kubernetes host
     */
    void processCompleteState(K8sHost k8sHost);

    /**
     * Processes the given host for incomplete state.
     *
     * @param k8sHost kubernetes host
     */
    void processIncompleteState(K8sHost k8sHost);
}
