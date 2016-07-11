/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225;

import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.L3VpnInstances;

/**
 * Abstraction of an entity which represents the functionality of neL3VpnApiService.
 */
public interface NeL3VpnApiService {

    /**
     * Returns the attribute l3VpnInstances.
     *
     * @return value of l3VpnInstances
     */
    L3VpnInstances getL3VpnInstances();

    /**
     * Sets the value to attribute l3VpnInstances.
     *
     * @param l3VpnInstances value of l3VpnInstances
     */
    void setL3VpnInstances(L3VpnInstances l3VpnInstances);
}
