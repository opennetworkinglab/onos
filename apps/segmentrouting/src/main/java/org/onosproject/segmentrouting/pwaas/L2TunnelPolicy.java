/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.segmentrouting.pwaas;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

public interface L2TunnelPolicy {

    /**
     * Returns the first connect point of the policy.
     *
     * @return first connect point
     */
    ConnectPoint cP1();

    /**
     * Returns the second connect point of the policy.
     *
     * @return second connect point
     */
    ConnectPoint cP2();

    /**
     * Returns the cP1 inner vlan tag of the policy.
     *
     * @return cP1 inner vlan tag
     */
    VlanId cP1InnerTag();

    /**
     * Returns the cP1 outer vlan tag of the policy.
     *
     * @return cP1 outer vlan tag
     */
    VlanId cP1OuterTag();

    /**
     * Returns the cP2 inner vlan tag of the policy.
     *
     * @return cP2 inner vlan tag
     */
    VlanId cP2InnerTag();

    /**
     * Returns the cP2 outer vlan tag of the policy.
     *
     * @return cP2 outer vlan tag
     */
    VlanId cP2OuterTag();

    /**
     * Returns the tunnel ID of the policy.
     *
     * @return Tunnel ID
     */
    long tunnelId();
}
