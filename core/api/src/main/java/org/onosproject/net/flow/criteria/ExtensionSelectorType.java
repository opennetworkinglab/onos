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

package org.onosproject.net.flow.criteria;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Type of selector extensions.
 */
@Beta
public class ExtensionSelectorType {

    /**
     * A list of well-known named extension selector type codes.
     * These numbers have no impact on the actual OF type id.
     */
    public enum ExtensionSelectorTypes {
        NICIRA_MATCH_NSH_SPI(0),
        NICIRA_MATCH_NSH_SI(1),
        NICIRA_MATCH_NSH_CH1(2),
        NICIRA_MATCH_NSH_CH2(3),
        NICIRA_MATCH_NSH_CH3(4),
        NICIRA_MATCH_NSH_CH4(5),
        NICIRA_MATCH_ENCAP_ETH_TYPE(6),
        NICIRA_MATCH_CONNTRACK_STATE(7),
        NICIRA_MATCH_CONNTRACK_ZONE(8),
        NICIRA_MATCH_CONNTRACK_MARK(9),
        NICIRA_MATCH_CONNTRACK_LABEL(10),
        OFDPA_MATCH_VLAN_VID(16),
        OFDPA_MATCH_OVID(17),
        OFDPA_MATCH_MPLS_L2_PORT(18),
        EXT_MATCH_FLOW_TYPE(20),

        UNRESOLVED_TYPE(200);

        private ExtensionSelectorType type;

        /**
         * Creates a new named extension selector type.
         *
         * @param type type code
         */
        ExtensionSelectorTypes(int type) {
            this.type = new ExtensionSelectorType(type);
        }

        /**
         * Gets the extension type object for this named type code.
         *
         * @return extension type object
         */
        public ExtensionSelectorType type() {
            return type;
        }
    }

    private final int type;

    /**
     * Creates an extension type with the given int type code.
     *
     * @param type type code
     */
    public ExtensionSelectorType(int type) {
        this.type = type;
    }

    /**
     * Returns the integer value associated with this type.
     *
     * @return an integer value
     */
    public int toInt() {
        return this.type;
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
        if (obj instanceof ExtensionSelectorType) {
            final ExtensionSelectorType that = (ExtensionSelectorType) obj;
            return this.type == that.type;
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ExtensionSelectorType.class)
                .add("type", type)
                .toString();
    }
}
