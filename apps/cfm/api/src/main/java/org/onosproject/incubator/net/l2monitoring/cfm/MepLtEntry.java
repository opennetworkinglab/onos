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

import java.util.Collection;

/**
 * An extension of the MepEntry for LinkTrace state attributes.
 *
 */
public interface MepLtEntry {
    /**
     * Get the next LTM Transaction Identifier to be sent in an LTM.
     * LTM = Link Trace Message
     * @return The next LTM Transaction Identifier to be sent in an LTM
     */
    int nextLtmIdentifier();

    /**
     * Get the total number of unexpected LTRs received.
     * @return The total number of unexpected LTRs received
     */
    int countLtrUnexpected();

    /**
     * Get the total number of LBRs transmitted.
     * LBR = LoopBack Response
     * @return The total number of LBRs transmitted
     */
    int countLbrTransmitted();

    /**
     * Get the linktrace database of results.
     * @return A collection of Linktrace transaction details
     */
    Collection<MepLtTransactionEntry> linktraceDatabase();

    /**
     * Builder for {@link MepLtEntry}.
     */
    interface MepLtEntryBuilder {
        MepLtEntryBuilder nextLtmIdentifier(int nextLtmIdentifier);

        MepLtEntryBuilder countLtrUnexpected(int countLtrUnexpected);

        MepLtEntryBuilder countLbrTransmitted(int countLbrTransmitted);

        MepLtEntryBuilder addToLinktraceDatabase(MepLtTransactionEntry linktrace);

        MepLtEntryBuilder build();
    }
}
