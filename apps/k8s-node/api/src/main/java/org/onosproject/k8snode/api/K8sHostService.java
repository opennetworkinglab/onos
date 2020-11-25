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
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for interfacing with the inventory of Kubernetes host.
 */
public interface K8sHostService extends ListenerService<K8sHostEvent, K8sHostListener> {

    String APP_ID = "org.onosproject.k8snode";

    /**
     * Returns all registered hosts.
     *
     * @return set of kubernetes hosts
     */
    Set<K8sHost> hosts();

    /**
     * Returns all hosts with complete states.
     *
     * @return set of kubernetes hosts
     */
    Set<K8sHost> completeHosts();

    /**
     * Returns the host with the specified IP address.
     *
     * @param hostIp host IP address
     * @return kubernetes host
     */
    K8sHost host(IpAddress hostIp);

    /**
     * Returns the host with the specified device ID.
     *
     * @param deviceId device identifier
     * @return kubernetes host
     */
    K8sHost host(DeviceId deviceId);

    /**
     * Returns the host with the specified tunnel bridge device ID.
     *
     * @param deviceId tunnel bridge's device ID
     * @return kubernetes host
     */
    K8sHost hostByTunBridge(DeviceId deviceId);

    /**
     * Returns the host with the specified router bridge device ID.
     *
     * @param deviceId router bridge's device ID
     * @return kubernetes host
     */
    K8sHost hostByRouterBridge(DeviceId deviceId);
}
