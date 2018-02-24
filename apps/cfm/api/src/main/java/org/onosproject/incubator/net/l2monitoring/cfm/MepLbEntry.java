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
 * An extension of the MepEntry for Loopback state attributes.
 *
 */
public interface MepLbEntry {
    /**
     * Get the next Loopback Transaction Identifier to be sent in an LBM.
     * LBM = LoopBack Message
     * @return The next Loopback Transaction Identifier to be sent in an LBM
     */
    long nextLbmIdentifier();

    /**
     * Get the total number of LBRs transmitted by this Mep.
     * @return The total number of LBRs transmitted
     */
    long countLbrTransmitted();

    /**
     * Get the total number of LBRs received by this Mep.
     * @return The total number of LBRs received
     */
    long countLbrReceived();

    /**
     * Get the total number of valid, in-order LBRs received.
     * @return The total number of valid, in-order LBRs received
     */
    long countLbrValidInOrder();

    /**
     * Get the total number of valid, out-of-order LBRs received.
     * @return The total number of valid, out-of-order LBRs received
     */
    long countLbrValidOutOfOrder();

    /**
     * Get the total number of LBRs received whose mac_service_data_unit did not match LBM.
     * @return the total number of LBRs received whose mac_service_data_unit did not match LBM
     */
    long countLbrMacMisMatch();

    /**
     * Builder for {@link MepLbEntry}.
     */
    interface MepLbEntryBuilder {
        MepLbEntryBuilder nextLbmIdentifier(long nextLbmIdentifier);

        MepLbEntryBuilder countLbrTransmitted(long countLbrTransmitted);

        MepLbEntryBuilder countLbrReceived(long countLbrRecieved);

        MepLbEntryBuilder countLbrValidInOrder(long countLbrValidInOrder);

        MepLbEntryBuilder countLbrValidOutOfOrder(long countLbrValidOutOfOrder);

        MepLbEntryBuilder countLbrMacMisMatch(long countLbrMacMisMatch);

        MepLbEntry build();
    }
}
