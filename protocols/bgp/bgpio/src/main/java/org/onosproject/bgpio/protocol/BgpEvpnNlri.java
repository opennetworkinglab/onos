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
package org.onosproject.bgpio.protocol;

import org.onosproject.bgpio.protocol.evpn.BgpEvpnNlriData;
import org.onosproject.bgpio.protocol.evpn.BgpEvpnRouteType;
import org.onosproject.bgpio.types.BgpValueType;

/**
 * Abstraction of an entity providing BGP-EVPN NLRI.
 */
public interface BgpEvpnNlri extends BgpValueType {

    /**
     * Returns route type in Nlri.
     *
     * @return route type in Nlri
     */
    BgpEvpnRouteType getRouteType();

    /**
     * Returns route type specific Nlri.
     *
     * @return route type in Nlri
     */
    BgpEvpnNlriData getNlri();

}
