/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.teyang.utils.topology;

import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
        .networks.network.augmentedndnetwork.Link;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.TeLinkEvent;

/**
 * The conversion functions.
 */
public final class LinkConverter {
    private static final String E_NULL_TELINK_UNDERLAY_PATH =
            "TeSubsystem link underlayPath object cannot be null";
    private static final String E_NULL_TELINK_DATA =
            "TeSubsystem teLinkAttrBuilder data cannot be null";
    private static final String E_NULL_TELINK =
            "TeSubsystem teLink object cannot be null";
    private static final String E_NULL_YANG_TELINK_CONFIG =
            "YANG telink config object cannot be null";
    private static final String E_NULL_YANG_TELINK =
            "YANG Link object cannot be null";

    // no instantiation
    private LinkConverter() {
    }


    /**
     * Link object conversion from YANG to TE Topology subsystem.
     *
     * @param yangLink  YANG link
     * @param networkId YANG networkId
     * @return TE subsystem link
     */
    public static org.onosproject.tetopology.management.api.link.NetworkLink
    yang2TeSubsystemLink(Link yangLink, NetworkId networkId) {

        //TODO: Implementation will be submitted as a separate review

        return null;
    }

    public static Link teSubsystem2YangLink(NetworkLink link) {

        //TODO: Implementation will be submitted as a separate review

        return null;
    }

    public static NetworkLinkKey yangLinkEvent2NetworkLinkKey(TeLinkEvent yangLinkEvent) {

        //TODO: Implementation will be submitted as a separate review

        return null;
    }

    public static NetworkLink yangLinkEvent2NetworkLink(TeLinkEvent yangLinkEvent,
                                                        TeTopologyService teTopologyService) {

        //TODO: Implementation will be submitted as a separate review

        return null;
    }
}
