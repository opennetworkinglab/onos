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

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes kubernetes network service event.
 */
public class K8sNetworkEvent extends AbstractEvent<K8sNetworkEvent.Type, K8sNetwork> {

    private final K8sPort port;

    /**
     * Kubernetes network events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes network is created.
         */
        K8S_NETWORK_CREATED,

        /**
         * Signifies that the kubernetes network is updated.
         */
        K8S_NETWORK_UPDATED,

        /**
         * Signifies that the kubernetes network is removed.
         */
        K8S_NETWORK_REMOVED,

        /**
         * Signifies that a new kubernetes port is created.
         */
        K8S_PORT_CREATED,

        /**
         * Signifies that the kubernetes port is updated.
         */
        K8S_PORT_UPDATED,

        /**
         * Signifies that the kubernetes port is removed.
         */
        K8S_PORT_REMOVED,

        /**
         * Signifies that the kubernetes port is activated.
         */
        K8S_PORT_ACTIVATED,

        /**
         * Signifies that the kubernetes port is inactivated.
         */
        K8S_PORT_INACTIVATED,
    }

    /**
     * Creates an event of a given type for the specified network.
     *
     * @param type kubernetes network event type
     * @param network kubernetes network
     */
    public K8sNetworkEvent(Type type, K8sNetwork network) {
        super(type, network);
        this.port = null;
    }

    /**
     * Creates an event of a given type for the specified network and port.
     *
     * @param type kubernetes network event type
     * @param network kubernetes network
     * @param port kubernetes port
     */
    public K8sNetworkEvent(Type type, K8sNetwork network, K8sPort port) {
        super(type, network);
        this.port = port;
    }

    /**
     * Returns the kubernetes port of the network event.
     *
     * @return kubernetes port; null if the event is not port specific
     */
    public K8sPort port() {
        return port;
    }

    @Override
    public String toString() {
        if (port == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("port", port)
                .add("network", subject())
                .toString();
    }
}
