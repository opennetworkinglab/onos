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

import org.onlab.packet.IpAddress;
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * IP Address Management service for kubernetes network.
 */
public interface K8sIpamService extends ListenerService<K8sIpamEvent, K8sIpamListener> {

    /**
     * Returns the already allocated IP addresses.
     *
     * @param networkId network identifier
     * @return allocated IP addresses
     */
    Set<IpAddress> allocatedIps(String networkId);

    /**
     * Returns the available IP addresses.
     *
     * @param networkId network identifier
     * @return available IP addresses
     */
    Set<IpAddress> availableIps(String networkId);
}
