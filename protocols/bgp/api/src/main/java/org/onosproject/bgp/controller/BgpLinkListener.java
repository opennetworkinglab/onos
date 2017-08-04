/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;

/**
 * Allows for providers interested in link events to be notified.
 */
public interface BgpLinkListener {

    /**
     * Notify that got a packet of link from network and adds link.
     *
     * @param linkNlri BGP link NLRI
     * @param details path attributes and NLRI information
     * @throws BgpParseException BGP parse exception
     */
    void addLink(BgpLinkLsNlriVer4 linkNlri, PathAttrNlriDetails details) throws BgpParseException;

    /**
     * Notify that got a packet of link from network and remove link.
     *
     * @param linkNlri BGP link NLRI
     * @throws BgpParseException BGP parse exception
     */
    void deleteLink(BgpLinkLsNlriVer4 linkNlri) throws BgpParseException;
}
