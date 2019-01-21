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
package org.onosproject.k8snetworking.api;

import org.onosproject.event.ListenerService;
import org.onosproject.k8snetworking.api.K8sNetwork.Type;
import org.onosproject.k8snetworking.api.K8sPort.State;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubernetes network.
 */
public interface K8sNetworkService
        extends ListenerService<K8sNetworkEvent, K8sNetworkListener> {

    /**
     * Returns the kubernetes network with the supplied network identifier.
     *
     * @param networkId network identifier
     * @return kubernetes network
     */
    K8sNetwork network(String networkId);

    /**
     * Returns all kubernetes networks registered in the service.
     *
     * @return set of kubernetes networks
     */
    Set<K8sNetwork> networks();

    /**
     * Returns the kubernetes networks with the given network type.
     *
     * @param type virtual network type
     * @return set of kubernetes networks
     */
    Set<K8sNetwork> networks(Type type);

    /**
     * Returns the kubernetes port with the supplied network identifier.
     *
     * @param portId port identifier
     * @return kubernetes port
     */
    K8sPort port(String portId);

    /**
     * Returns all kubernetes ports registered in the service.
     *
     * @return set of kubernetes ports
     */
    Set<K8sPort> ports();

    /**
     * Returns the kubernetes ports with the given port state.
     *
     * @param state port state
     * @return set of kubernetes ports
     */
    Set<K8sPort> ports(State state);

    /**
     * Returns the kubernetes ports belongs to the given network.
     *
     * @param networkId network identifier
     * @return kubernetes ports
     */
    Set<K8sPort> ports(String networkId);
}
