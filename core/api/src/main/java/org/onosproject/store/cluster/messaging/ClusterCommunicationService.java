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
package org.onosproject.store.cluster.messaging;

/**
 * Service for assisting communications between controller cluster nodes.
 * <p>
 * Communication via this service is isolated to nodes running a single version of the software. During upgrades, when
 * nodes may be running multiple versions simultaneously, this service prevents nodes running different versions of
 * the software from communicating with each other, thus avoiding compatibility issues. For an equivalent cross-version
 * compatible service, see {@link UnifiedClusterCommunicationService}.
 *
 * @see UnifiedClusterCommunicationService
 */
public interface ClusterCommunicationService extends ClusterCommunicator {
}
