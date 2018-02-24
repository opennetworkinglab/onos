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

/**
 * The list of LTRs associated with a specific Linktrace transaction.
 */
public interface MepLtReply {
    /**
     * An index to distinguish among multiple LTRs with the same LTR transaction-id field value.
     * reply-order are assigned sequentially from 1, in the order that the Linktrace Initiator received the LTR
     * @return The index
     */
    int replyOrder();

    /**
     * The integer Reply TTL field value returned in the LTR.
     * @return the Reply TTL field value
     */
    int replyTtl();

    /**
     * A Boolean value stating whether an LTM was forwarded by the responding MP.
     * @return true when the LTM was forwarded
     */
    boolean forwarded();

    /**
     * A Boolean value stating whether the forwarded LTM reached a MEP for its MA.
     * @return true when the forwarded LTM reached a MEP
     */
    boolean terminalMep();

    /**
     * An octet string holding the Last Egress Identifier field returned in the LTR Egress Identifier TLV of the LTR.
     * @return the Last Egress Identifier
     */
    byte[] lastEgressIdentifier();

    /**
     * An octet string holding the Next Egress Identifier field returned in the LTR Egress Identifier TLV of the LTR.
     * @return the Next Egress Identifier
     */
    byte[] nextEgressIdentifier();

    /**
     * An enumerated value indicating the value returned in the Relay Action field.
     * @return the Relay Action
     */
    LtrReply ltrRelay();

    /**
     * Builder for {@link MepLtReply}.
     */
    public interface MepLtReplyBuilder {
        MepLtReplyBuilder replyOrder(int replyOrder);

        MepLtReplyBuilder replyTtl(int replyTtl);

        MepLtReplyBuilder forwarded(boolean forwarded);

        MepLtReplyBuilder terminalMep(boolean terminalMep);

        MepLtReplyBuilder lastEgressIdentifier(byte[] lastEgressIdentifier);

        MepLtReplyBuilder nextEgressIdentifier(byte[] nextEgressIdentifier);

        MepLtReplyBuilder ltrRelay(LtrReply ltrRelay);

        MepLtReplyBuilder build();
    }

    /**
     * An enumerated value indicating the value returned in the Relay Action field.
     * reference [802.1q] 12.14.7.5.3:g, Table 21-27 IEEE8021-CFM-MIB.Dot1agCfmRelayActionFieldValue
     */
    public enum LtrReply {
        /**
         * Indicates the LTM reached an MP whose MAC address matches the target MAC address.
         */
        RELAY_HIT,
        /**
         * Indicates the Egress Port was determined by consulting the Filtering Database.
         */
        RELAY_FILTERING_DATABASE,
        /**
         * Indicates the Egress Port was determined by consulting the MIP CCM Database.
         */
        RELAY_MIP_CCM_DATABASE;
    }
}
