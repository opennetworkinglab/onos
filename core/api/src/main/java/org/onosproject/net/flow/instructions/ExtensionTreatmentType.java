/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.flow.instructions;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Type of treatment extensions.
 */
@Beta
public final class ExtensionTreatmentType {

    /**
     * A list of well-known named extension instruction type codes.
     * These numbers have no impact on the actual OF type id.
     */
    public enum ExtensionTreatmentTypes {
        NICIRA_SET_TUNNEL_DST(0),
        NICIRA_RESUBMIT(1),
        NICIRA_MOV_ARP_SHA_TO_THA(2),
        NICIRA_MOV_ARP_SPA_TO_TPA(3),
        NICIRA_MOV_ETH_SRC_TO_DST(4),
        NICIRA_MOV_IP_SRC_TO_DST(5),
        NICIRA_MOV_NSH_C1_TO_C1(6),
        NICIRA_MOV_NSH_C2_TO_C2(7),
        NICIRA_MOV_NSH_C3_TO_C3(8),
        NICIRA_MOV_NSH_C4_TO_C4(9),
        NICIRA_MOV_TUN_IPV4_DST_TO_TUN_IPV4_DST(10),
        NICIRA_MOV_TUN_ID_TO_TUN_ID(11),
        NICIRA_MOV_NSH_C2_TO_TUN_ID(12),
        NICIRA_RESUBMIT_TABLE(14),
        NICIRA_LOAD(20),
        NICIRA_PUSH_NSH(38),
        NICIRA_POP_NSH(39),
        NICIRA_CT(40),
        NICIRA_NAT(41),
        NICIRA_CT_CLEAR(42),
        OFDPA_SET_VLAN_ID(64),
        OFDPA_SET_MPLS_TYPE(65),
        OFDPA_SET_OVID(66),
        OFDPA_SET_MPLS_L2_PORT(67),
        OFDPA_SET_QOS_INDEX(68),
        OFDPA_PUSH_L2_HEADER(69),
        OFDPA_PUSH_CW(70),
        OFDPA_POP_L2_HEADER(71),
        OFDPA_POP_CW(72),
        NICIRA_TUN_GPE_NP(111),
        NICIRA_SET_NSH_SPI(113),
        NICIRA_SET_NSH_SI(114),
        NICIRA_SET_NSH_CH1(115),
        NICIRA_SET_NSH_CH2(116),
        NICIRA_SET_NSH_CH3(117),
        NICIRA_SET_NSH_CH4(118),
        NICIRA_NSH_MDTYPE(119),
        NICIRA_NSH_NP(120),
        NICIRA_ENCAP_ETH_SRC(121),
        NICIRA_ENCAP_ETH_DST(122),
        NICIRA_ENCAP_ETH_TYPE(123),
        OPLINK_ATTENUATION(130),
        OFDPA_ALLOW_VLAN_TRANSLATION(131),
        ONF_COPY_FIELD(132),
        UNRESOLVED_TYPE(200);

        private ExtensionTreatmentType type;

        /**
         * Creates a new named extension treatment type.
         *
         * @param type type code
         */
        ExtensionTreatmentTypes(int type) {
            this.type = new ExtensionTreatmentType(type);
        }

        /**
         * Gets the extension type object for this named type code.
         *
         * @return extension type object
         */
        public ExtensionTreatmentType type() {
            return type;
        }
    }

    private final int type;

    /**
     * Creates an extension type with the given int type code.
     *
     * @param type type code
     */
    public ExtensionTreatmentType(int type) {
        this.type = type;
    }

    /**
     * Returns extension treatment type.
     *
     * @return extension treatment type
     */
    public int type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExtensionTreatmentType) {
            final ExtensionTreatmentType that = (ExtensionTreatmentType) obj;
            return this.type == that.type;
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ExtensionTreatmentType.class)
                .add("type", type)
                .toString();
    }
}
