/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.bgpio.protocol.flowspec;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

/**
 * Test for BgpFsDestinationPrefix flow specification component.
 */
public class BgpFlowSpecPrefixTest {
    private String routeKey1 = "flowRoute1";
    private String routeKey2 = "flowRoute2";

    private final BgpFlowSpecRouteKey tlv1 = new BgpFlowSpecRouteKey(routeKey1);
    private final BgpFlowSpecRouteKey sameAsTlv1 = new BgpFlowSpecRouteKey(routeKey1);
    private final BgpFlowSpecRouteKey tlv2 = new BgpFlowSpecRouteKey(routeKey2);

    @Test
    public void testEquality() {
        new EqualsTester()
        .addEqualityGroup(tlv1, sameAsTlv1)
        .addEqualityGroup(tlv2)
        .testEquals();
    }
}
