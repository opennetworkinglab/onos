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
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.network
        .Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.TeNodeEvent;

/**
 * Node conversion functions.
 */
public final class NodeConverter {

    private static final String E_NULL_TE_SUBSYSTEM_TE_NODE =
            "TeSubsystem teNode object cannot be null";
    private static final String E_NULL_TE_SUBSYSTEM_TE_TUNNEL_TP =
            "TeSubsystem teTunnelTp object cannot be null";
    private static final String E_NULL_TE_SUBSYSTEM_NODE =
            "TeSubsystem ndoe object cannot be null";
    private static final String E_NULL_YANG_NODE =
            "Yang node object cannot be null";

    // no instantiation
    private NodeConverter() {
    }


    /**
     * Node object conversion from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE subsystem node
     * @return YANG node
     */
    public static Node teSubsystem2YangNode(org.onosproject.tetopology.management.api.node.NetworkNode teSubsystem) {
        //TODO: implementation to be submitted as a separate review
        return null;
    }

    /**
     * Node object conversion from YANG to TE Topology subsystem.
     *
     * @param yangNode      Network node in YANG model
     * @param yangNetworkId YANG network identifier in YANG model
     * @return TE subsystem node
     */
    public static org.onosproject.tetopology.management.api.node.NetworkNode
    yang2TeSubsystemNode(Node yangNode, NetworkId yangNetworkId) {

        //TODO: implementation to be submitted as separate review

        return null;
    }

    public static NetworkNodeKey yangNodeEvent2NetworkNodeKey(TeNodeEvent yangNodeEvent) {

        //TODO: implementation to be submitted as separate review

        return null;
    }

    public static NetworkNode yangNodeEvent2NetworkNode(TeNodeEvent yangNodeEvent,
                                                        TeTopologyService teTopologyService) {

        //TODO: implementation to be submitted as separate review

        return null;
    }
}
