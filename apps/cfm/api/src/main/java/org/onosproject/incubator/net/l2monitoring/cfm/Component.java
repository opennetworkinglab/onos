/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm;

import com.google.common.annotations.Beta;
import org.onlab.packet.EthType;
import org.onlab.packet.VlanId;

import java.util.Collection;

/**
 * Components which can be managed in a manner equivalent to an 802.1Q bridge.
 *
 * Direct child of the {@link MaintenanceAssociation}.
 */
public interface Component {

    int componentId();

    /**
     * The VID(s) monitored by this MA, or 0, if the MA is not attached to any VID.
     *
     * The first VID returned is the MA's Primary VID
     * @return A collection of VIDs
     */
    Collection<VlanId> vidList();

    /**
     * Defines how the MA can create MHFs (MIP Half Function) for this VID at this MA.
     * @return An enumerated value
     */
    MhfCreationType mhfCreationType();

    /**
     * indicates what, if anything, is to be included in the Sender ID TLV.
     * The Sender ID TLV is transmitted by MPs configured in this MA.
     * @return An enumerated value
     */
    IdPermissionType idPermission();

    /**
     * Indicates the tag type for this component.
     * @return The type of Tag active on this VLAN
     */
    @Beta
    TagType tagType();

    /**
     * Builder for {@link Component}.
     */
    interface ComponentBuilder {

        ComponentBuilder addToVidList(VlanId vid);

        ComponentBuilder mhfCreationType(MhfCreationType mhfCreationType);

        ComponentBuilder idPermission(IdPermissionType idPermission);

        ComponentBuilder tagType(TagType tagType);

        Component build();
    }

    /**
     * An enumerated type defining how MHFs (MIP Half Function) can be created.
     */
    public enum MhfCreationType {
        /**
         * No MHFs can be created for this VID(s).
         */
        NONE,

        /**
         * MHFs can be created for this VID(s) on any Bridge Port through which the.
         * VID(s) can pass where:
         * - There are no lower active MD levels; or
         * - There is a MEP at the next lower active MD-level on the port
         */
        DEFAULT,

        /**
         * MHFs can be created for this VID(s) only on Bridge Ports through which.
         * this VID(s) can pass, and only if there is a MEP at the next
         * lower active MD-level on the port.
         */
        EXPLICIT,

        /**
         * In the Maintenance Association managed object only, the control of MHF.
         * creation is deferred to the corresponding variable in the
         * enclosing Maintenance Domain
         */
        DEFER
    }

    /**
     * An enumerated value indicating what, if anything, is to be included in.
     * the Sender ID TLV transmitted by maintenance-points configured in the
     * default Maintenance Domain
     * reference
     * [802.1q] 21.5.3, 12.14.3.1.3:e";
     */
    public enum IdPermissionType {
        /**
         * The Sender ID TLV is not to be sent.
         */
        NONE,

        /**
         * The Chassis ID Length, Chassis ID Subtype, and Chassis ID fields of the.
         * Sender ID TLV are to be sent, but not the Management Address
         * Length or Management Address fields.
         */
        CHASSIS,

        /**
         * The Management Address Length and Management Address of the Sender ID.
         * TLV are to be sent, but the Chassis ID Length is to be
         * transmitted with a 0 value, and the Chassis ID Subtype and
         * Chassis ID fields not sent
         */
        MANAGE,

        /**
         * The Chassis ID Length, Chassis ID Subtype, Chassis ID, Management.
         * Address Length, and Management Address fields are all to be sent
         */
        CHASSIS_MANAGE,

        /**
         * The contents of the Sender ID TLV are determined by the Maintenance.
         * Domain managed object
         */
        DEFER
    }

    /**
     * A choice of VLan tag type.
     */
    public enum TagType {
        VLAN_NONE(EthType.EtherType.IPV4),
        VLAN_CTAG(EthType.EtherType.VLAN),
        VLAN_STAG(EthType.EtherType.QINQ);

        private EthType.EtherType type = EthType.EtherType.IPV4;

        TagType(EthType.EtherType type) {
            this.type = type;
        }

        public EthType.EtherType getType() {
            return type;
        }

        public static TagType fromEtherType(EthType.EtherType type) {
            for (TagType tt:values()) {
                if (tt.type.equals(type)) {
                    return tt;
                }
            }
            throw new IllegalArgumentException("Unsupported EtherType: " + type);
        }
    }
}
