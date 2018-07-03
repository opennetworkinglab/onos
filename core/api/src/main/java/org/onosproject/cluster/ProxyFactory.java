/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.cluster;

/**
 * Constructs proxy instances for nodes in the cluster.
 * <p>
 * The proxy factory constructs proxy instances for the master node of a given {@link NodeId}.
 * When a proxy method is invoked, the method will be called on the provided node.
 */
public interface ProxyFactory<T> {

    /**
     * Returns the proxy for the given peer.
     * <p>
     * The proxy instance is a multiton that is cached internally in the factory, so calling this method
     *
     * @param nodeId the peer node identifier
     * @return the proxy for the given peer
     */
    T getProxyFor(NodeId nodeId);

}
