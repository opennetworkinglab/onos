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
package org.onosproject.store.cluster.messaging;

import com.google.common.annotations.Beta;

/**
 * Service for unified communication across controller nodes running multiple software versions.
 * <p>
 * This service supports communicating across nodes running different versions of the software simultaneously. During
 * upgrades, when nodes may be running a mixture of versions, this service can be used to coordinate across those
 * versions. But users of this service must be extremely careful to preserve backward/forward compatibility for
 * messages sent across versions. Encoders and decoders used for messages sent/received on this service should
 * support evolving schemas.
 */
@Beta
public interface UnifiedClusterCommunicationService extends ClusterCommunicator {
}
