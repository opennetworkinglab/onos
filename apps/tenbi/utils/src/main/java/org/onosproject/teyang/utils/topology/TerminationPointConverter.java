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

import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ietfnetworktopology
        .networks.network.node.augmentedndnode.TerminationPoint;

/**
 * The termination point translations.
 */
public final class TerminationPointConverter {

    private static final String E_NULL_TE_SUBSYSTEM_TP =
            "TeSubsystem terminationPoint object cannot be null";
    private static final String E_NULL_YANG_TP =
            "YANG terminationPoint object cannot be null";

    // no instantiation
    private TerminationPointConverter() {
    }

    /**
     * TerminationPoint object translation from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem termination point
     * @return Termination point in YANG Java data structure
     */
    public static TerminationPoint teSubsystem2YangTerminationPoint(
            org.onosproject.tetopology.management.api.node.TerminationPoint teSubsystem) {

        //TODO: implementation to be submitted as a separate review
        return null;
    }


    /**
     * TerminationPoint object translation from YANG to TE Topology subsystem.
     *
     * @param yangTp Termination point in YANG Java data structure
     * @return TerminationPoint TE Topology subsystem termination point
     */
    public static org.onosproject.tetopology.management.api.node.TerminationPoint
    yang2teSubsystemTerminationPoint(TerminationPoint yangTp) {

        // TODO: implementation to be submitted as separate review
        return null;
    }

}
