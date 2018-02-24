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

import java.util.BitSet;

/**
 * Object representing Linktrace create parameters.
 */
public interface MepLtCreate {
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
     * The Flags field for LTMs transmitted by the MEP.
     * [802.1q] 12.14.7.4.2:b
     * Bit 0 is use-fdb-only
     * @return A bit set of flags
     */
    BitSet transmitLtmFlags();

    /**
     * An initial value for the LTM TTL field. 64 if not specified.
     * [802.1q] 12.14.7.4.2:d
     * @return The default number of hops
     */
    Short defaultTtl();


    /**
     * Builder for {@link MepLtCreate}.
     */
    interface MepLtCreateBuilder {
        MepLtCreateBuilder transmitLtmFlags(BitSet transmitLtmFlags);

        MepLtCreateBuilder defaultTtl(Short defaultTtl);

        public MepLtCreate build();
    }
}
