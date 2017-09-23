/*
 * Copyright 2014-present Open Networking Foundation
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
 * Service for obtaining information about the individual nodes within the controller cluster.
 * <p>
 * This service's view of the nodes in the cluster is isolated to a single version of the software. During upgrades,
 * when multiple versions of the software are running in the same cluster, users of this service will only be able
 * to see nodes running the same version as the local node. This is useful for limiting communication to nodes running
 * the same version of the software.
 */
public interface ClusterService extends MembershipService {
}
