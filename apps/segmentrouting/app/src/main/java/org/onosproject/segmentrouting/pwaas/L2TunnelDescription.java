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

public interface L2TunnelDescription {

    /**
     * Returns the l2 tunnel.
     *
     * @return the l2 tunnel
     */
    L2Tunnel l2Tunnel();

    /**
     * Returns the l2 tunnel policy.
     *
     * @return the l2 tunnel policy.
     */
    L2TunnelPolicy l2TunnelPolicy();

    /**
     * Sets the l2 tunnel.
     *
     * @param tunnel the l2 tunnel to set.
     */
    void setL2Tunnel(L2Tunnel tunnel);

    /**
     * Sets the l2 policy.
     *
     * @param policy the policy to set.
     */
    void setL2TunnelPolicy(L2TunnelPolicy policy);
}
