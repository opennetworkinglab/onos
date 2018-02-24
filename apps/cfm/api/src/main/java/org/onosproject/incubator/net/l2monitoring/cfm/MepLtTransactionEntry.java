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

import java.util.BitSet;
import java.util.Collection;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

/**
 * The LTM entry for a previously transmitted LTM.
 * An LTM entry consists of a list of LTR entries, each
 * corresponding to a Linktrace Reply (LTR) PDU received in response to that LTM.
 *
 * See IEEE 802.1Q 12.14.7.5.3
 */
public interface MepLtTransactionEntry {
    /**
     * The LTM Transaction Identifier to which the LTR entries will be attached.
     * @return The id
     */
    int transactionId();

    /**
     * The remote Mep will be identified by either a MacAddress or a MEPId.
     * @return The MAC address of the remoteMep
     */
    MacAddress macAddress();

    /**
     * The remote Mep will be identified by either a MacAddress or a MEPId.
     * @return The id of the remoteMep
     */
    MepId mepId();

    /**
     * The Flags field for LTMs transmitted by the MEP.
     * [802.1q] 12.14.7.4.2:b
     * Bit 0 is use-fdb-only
     * @return A bit set of flags
     */
    BitSet transmitLtmUseFdbOnly();

    /**
     * An initial value for the LTM TTL field. 64 if not specified.
     * [802.1q] 12.14.7.4.2:d
     * @return The default number of hops
     */
    int defaultTtl();

    /**
     * The list of LTRs associated with a specific Linktrace transaction.
     * @return A collection of Replies
     */
    Collection<MepLtReply> ltrReplies();

    /**
     * Builder for {@link MepLtTransactionEntry}.
     */
    interface MepLtEntryBuilder {
        MepLtEntryBuilder macAddress(MacAddress macAddress);

        MepLtEntryBuilder mepId(MepId mepId);

        MepLtEntryBuilder transmitLtmUseFdbOnly(boolean transmitLtmUseFdbOnly);

        MepLtEntryBuilder defaultTtl(int defaultTtl);

        MepLtEntryBuilder addToLtrReplies(MepLtReply ltrReply);

        MepLtEntryBuilder build();
    }
}
