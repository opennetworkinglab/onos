/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.types.RouteDistinguisher;

/**
 * Abstraction of an entity providing BGP-LS NLRI.
 */
public interface BgpLSNlri {
    /**
     * Returns NlriType of BGP-LS NLRI.
     *
     * @return NlriType of BGP-LS NLRI
     */
    NlriType getNlriType();

    /**
     * Returns Identifier in Nlri.
     *
     * @return Identifier in Nlri
     */
    long getIdentifier();

    /**
     * Returns Protocol Id in Nlri.
     *
     * @return Protocol Id in Nlri
     * @throws BgpParseException while getting protocol ID
     */
    ProtocolType getProtocolId() throws BgpParseException;

    /**
     * Returns Route distinguisher in Nlri.
     *
     * @return Route distinguisher in Nlri
     */
    RouteDistinguisher getRouteDistinguisher();
}
