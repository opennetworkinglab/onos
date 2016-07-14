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
package org.onosproject.pcepio.types;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class PcepNaiUnnumberedAdjacencyIpv4Test {

    private final int localNodeId1 = 1;
    private final int localInterfaceId1 = 1;
    private final int remoteNodeId1 = 1;
    private final int remoteInterfaceId1 = 1;
    private final PcepNaiUnnumberedAdjacencyIpv4 tlv1 = PcepNaiUnnumberedAdjacencyIpv4.of(localNodeId1,
            localInterfaceId1, remoteNodeId1, remoteInterfaceId1);

    private final int localNodeId2 = 1;
    private final int localInterfaceId2 = 1;
    private final int remoteNodeId2 = 1;
    private final int remoteInterfaceId2 = 1;
    private final PcepNaiUnnumberedAdjacencyIpv4 tlv2 = PcepNaiUnnumberedAdjacencyIpv4.of(localNodeId2,
            localInterfaceId2, remoteNodeId2, remoteInterfaceId2);

    private final int localNodeId3 = 2;
    private final int localInterfaceId3 = 2;
    private final int remoteNodeId3 = 2;
    private final int remoteInterfaceId3 = 2;

    private final PcepNaiUnnumberedAdjacencyIpv4 tlv3 = PcepNaiUnnumberedAdjacencyIpv4.of(localNodeId3,
            localInterfaceId3, remoteNodeId3, remoteInterfaceId3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }
}
