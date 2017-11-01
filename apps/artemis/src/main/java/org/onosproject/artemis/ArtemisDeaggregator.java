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

package org.onosproject.artemis;

/**
 * Interface for Deaggregator Service of Artemis.
 *
 * When a prefix hijacking is detected, ARTEMIS automatically launches its mitigation service (deaggregator).
 * Since in Internet routing the most specific prefix is always preferred, ARTEMIS modifies the BGP configuration of
 * the routers so that they announce deaggregated sub-prefixes of the hijacked prefix (that are most preferred from any
 * AS). After BGP converges, the hijacking attack is mitigated and traffic flows normally back to the ARTEMIS-protected
 * AS (the one that runs ARTEMIS). Therefore, ARTEMIS assumes write permissions to the routers of the network, in order
 * to be able to modify their BGP configuration and mitigate the attack. The purpose of this service is to receive all
 * hijack events from the detector service and proceed on writing all the new prefixes to be announced by the BGP
 * Speakers.
 */
public interface ArtemisDeaggregator {
    //TODO: give the ability of other services to announce prefixes to BGP Speakers through this interface
}
