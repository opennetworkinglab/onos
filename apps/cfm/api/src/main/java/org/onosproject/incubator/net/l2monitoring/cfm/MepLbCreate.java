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

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

/**
 * Grouping of parameters used to create a loopback test on a MEP.
 */
public interface MepLbCreate {
    /**
     * The remote Mep will be identified by either a MacAddress or a MEPId.
     * @return The MAC address of the remoteMep
     */
    MacAddress remoteMepAddress();

    /**
     * The remote Mep will be identified by either a MacAddress or a MEPId.
     * @return The id of the remoteMep
     */
    MepId remoteMepId();

    /**
     * The number of LBM transmissions in a session.
     * [802.1q] 12.14.7.3.2:c, [MEF30] R39
     * @return The number of messages to send
     */
    Integer numberMessages();

    /**
     * An arbitrary amount of data to be included in a Data TLV.
     * [802.1q] 12.14.7.3.d, IEEE8021-CFM-MIB.dot1agCfmMepTransmitLbmDataTlv
     * @return The data that will be sent encoded as hexadecimal (lower case, colon separated bytes)
     */
    String dataTlvHex();

    /**
     * The priority parameter to be used in the transmitted LBMs.
     * [802.1q] 12.14.7.3.2:e
     * @return The priority to be used
     */
    Mep.Priority vlanPriority();

    /**
     * The drop eligible parameter to be used in the transmitted LBMs.
     * @return True or False
     */
    Boolean vlanDropEligible();

    /**
     * Builder for {@link MepLbCreate}.
     */
    interface MepLbCreateBuilder {
        MepLbCreateBuilder numberMessages(int numberMessages);

        /**
         * Define the dataTlv straight from a byte array.
         * @param dataTlv String of hex pairs separated by : e.g. AA:BB:CC
         * @return The builder
         */
        MepLbCreateBuilder dataTlv(byte[] dataTlv);

        /**
         * Define the dataTlv byte array from a Hex string.
         * @param dataTlv String of hex pairs separated by : e.g. AA:BB:CC
         * @return The builder
         */
        MepLbCreateBuilder dataTlvHex(String dataTlv);

        /**
         * Define the dataTlv byte array from a Base64 string.
         * @param dataTlv A string in base64 encoding
         * @return The builder
         */
        MepLbCreateBuilder dataTlvB64(String dataTlv);

        MepLbCreateBuilder vlanPriority(Mep.Priority vlanPriority);

        MepLbCreateBuilder vlanDropEligible(boolean vlanDropEligible);

        MepLbCreate build();
    }
}
