/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import com.google.common.collect.ImmutableList;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory to build criteria to punt packet to the CPU from the BNG pipeline.
 */
final class PuntCpuCriterionFactory {

    private static final ImmutableList<Byte> PPPOE_CODE =
            ImmutableList.<Byte>builder()
                    .add((byte) 0x09) // PADI
                    .add((byte) 0x07) // PADO
                    .add((byte) 0x19) // PADR
                    .add((byte) 0x65) // PADS
                    .add((byte) 0xa7) // PADT
                    .build();

    private static final ImmutableList<Short> PPP_PROTOCOL =
            ImmutableList.<Short>builder()
                    .add((short) 0xc021) // LCP
                    .add((short) 0x8021) // IPCP
                    .add((short) 0xc023) // PAP
                    .add((short) 0xc223) // CHAP
                    .build();

    private static final byte PPP_PROTOCOL_DEFAULT_MASK = (byte) 0xFFFF;

    private PuntCpuCriterionFactory() {
        // Hide constructor
    }

    /**
     * Build all the Protocol Independent criteria starting from all the PPPoE
     * codes.
     *
     * @return The list of Protocol Independent criteria.
     */
    static Set<Criterion> getAllPuntCriterion() {
        Set<Criterion> criteria = PPPOE_CODE.stream()
                .map(PuntCpuCriterionFactory::getPppoePiCriterion).collect(Collectors.toSet());

        criteria.addAll(PPP_PROTOCOL.stream()
                                .map(PuntCpuCriterionFactory::getPppPiCriterion)
                                .collect(Collectors.toSet()));
        return criteria;
    }

    /**
     * Build the Protocol Independent criterion related to the specific PPPoE
     * code.
     *
     * @param pppoeCode PPPoE code field.
     * @return The built criterion.
     */
    private static PiCriterion getPppoePiCriterion(byte pppoeCode) {
        return PiCriterion.builder()
                .matchExact(FabricConstants.HDR_PPPOE_CODE, new byte[]{pppoeCode})
                .build();
    }

    /**
     * Build the Protocol Independent criterion related to the specified PPPoE
     * code and PPP protocol matching.
     * <p>
     * Match on PPPoE Protocol will be done with 0xFFFF as mask.
     *
     * @param pppProtocol PPP protocol field.
     * @return The built criterion.
     */
    private static PiCriterion getPppPiCriterion(short pppProtocol) {
        return PiCriterion.builder()
                .matchExact(FabricConstants.HDR_PPPOE_CODE, 0)
                .matchTernary(FabricConstants.HDR_PPPOE_PROTOCOL,
                              pppProtocol,
                              PPP_PROTOCOL_DEFAULT_MASK)
                .build();
    }
}

