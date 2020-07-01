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

import org.onlab.packet.IpAddress;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of Kubernetes Host; not intended for direct use.
 */
public interface K8sHostStore extends Store<K8sHostEvent, K8sHostStoreDelegate> {

    /**
     * Creates a new host.
     *
     * @param host kubernetes host
     */
    void createHost(K8sHost host);

    /**
     * Updates the host.
     *
     * @param host kubernetes host
     */
    void updateHost(K8sHost host);

    /**
     * Removes the host with the given host IP.
     *
     * @param hostIp kubernetes host IP
     * @return removed kubernetes host; null if no host is associated with the host IP
     */
    K8sHost removeHost(IpAddress hostIp);

    /**
     * Returns all registered hosts.
     *
     * @return set of kubernetes hosts
     */
    Set<K8sHost> hosts();

    /**
     * Returns the host with the specified host IP.
     *
     * @param hostIp host IP address
     * @return kubernetes host
     */
    K8sHost host(IpAddress hostIp);
}
