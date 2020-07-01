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

/**
 * Service for administering inventory of Kubernetes hosts.
 */
public interface K8sHostAdminService extends K8sHostService {

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
     * Removes the host.
     *
     * @param hostIp kubernetes host IP address
     * @return removed host; null if the host does not exist
     */
    K8sHost removeHost(IpAddress hostIp);
}
