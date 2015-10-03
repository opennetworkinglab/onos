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
package org.onosproject.pcepio;

import org.junit.Test;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentidiersTlv;

import com.google.common.testing.EqualsTester;

public class StatefulIPv4LspIdentidiersTlvTest {

    private final int ipv4IngressAddress = 1;
    private final short lspId = 1;
    private final short tunnelId = 1;
    private final int extendedTunnelId = 1;
    private final int ipv4EgressAddress = 1;

    private final StatefulIPv4LspIdentidiersTlv tlv1 = StatefulIPv4LspIdentidiersTlv.of(ipv4IngressAddress, lspId,
            tunnelId, extendedTunnelId, ipv4EgressAddress);

    private final int ipv4IngressAddress1 = 1;
    private final short lspId1 = 1;
    private final short tunnelId1 = 1;
    private final int extendedTunnelId1 = 1;
    private final int ipv4EgressAddress1 = 1;

    private final StatefulIPv4LspIdentidiersTlv tlv2 = StatefulIPv4LspIdentidiersTlv.of(ipv4IngressAddress1, lspId1,
            tunnelId1, extendedTunnelId1, ipv4EgressAddress1);

    private final int ipv4IngressAddress2 = 2;
    private final short lspId2 = 2;
    private final short tunnelId2 = 2;
    private final int extendedTunnelId2 = 2;
    private final int ipv4EgressAddress2 = 2;

    private final StatefulIPv4LspIdentidiersTlv tlv3 = StatefulIPv4LspIdentidiersTlv.of(ipv4IngressAddress2, lspId2,
            tunnelId2, extendedTunnelId2, ipv4EgressAddress2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();

    }
}
