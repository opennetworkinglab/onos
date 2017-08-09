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

package org.onosproject.bgpio.protocol.evpn;

import org.jboss.netty.buffer.ChannelBuffer;

public interface BgpEvpnNlriData {

    /**
     * Returns the Type of RouteTypeSpefic.
     *
     * @return short value of type
     */
    BgpEvpnRouteType getType();

    /**
     * Writes the byte Stream of BGP Message to channel buffer.
     *
     * @param cb channel buffer
     * @return length written to channel buffer
     */
    int write(ChannelBuffer cb);

}
