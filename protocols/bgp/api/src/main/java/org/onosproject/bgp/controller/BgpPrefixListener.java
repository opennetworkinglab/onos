/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.onosproject.bgp.controller;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;

/**
 * Allows for providers interested in bgp prefix events to be notified.
 */
public interface BgpPrefixListener {

    /**
     * Notify that prefix was added or updated.
     *
     * @param prefixNlri BGP prefix NLRI
     * @param details path attributes and NLRI information
     * @throws BgpParseException BGP parse exception
     */
    void addPrefix(BgpPrefixIPv4LSNlriVer4 prefixNlri, PathAttrNlriDetails details) throws BgpParseException;

    /**
     * Notify that prefix was removed.
     *
     * @param prefixNlri BGP prefix NLRI
     * @throws BgpParseException BGP parse exception
     */
    void deletePrefix(BgpPrefixIPv4LSNlriVer4 prefixNlri) throws BgpParseException;
}
