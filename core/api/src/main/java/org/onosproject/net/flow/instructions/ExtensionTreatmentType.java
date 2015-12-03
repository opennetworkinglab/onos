/*
 * Copyright 2015 Open Networking Laboratory
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
        NICIRA_RESUBMIT_TABLE(14),
        NICIRA_SET_NSH_SPI(32),
        NICIRA_SET_NSH_SI(33),
        NICIRA_SET_NSH_CH1(34),
        NICIRA_SET_NSH_CH2(35),
        NICIRA_SET_NSH_CH3(36),
        NICIRA_SET_NSH_CH4(37),
        NICIRA_MOV_ARP_SHA_TO_THA(2),
        NICIRA_MOV_ARP_SPA_TO_TPA(3),
        NICIRA_MOV_ETH_SRC_TO_DST(4),
        NICIRA_MOV_IP_SRC_TO_DST(5);

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
