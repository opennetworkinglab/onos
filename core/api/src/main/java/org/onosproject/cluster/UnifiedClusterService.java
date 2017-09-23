/*
 * Copyright 2017-present Open Networking Foundation
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

import com.google.common.annotations.Beta;

/**
 * Unified multi-version cluster membership service.
 * <p>
 * During upgrades, the nodes within a cluster may be running multiple versions of the software.
 * This service has a view of the entire cluster running any version. Users of this service must be careful when
 * communicating with nodes described by this service as compatibility issues can result from communicating across
 * versions. For an equivalent service that has an isolated view of the cluster, see {@link ClusterService}.
 *
 * @see ClusterService
 */
@Beta
public interface UnifiedClusterService extends MembershipService {
}
