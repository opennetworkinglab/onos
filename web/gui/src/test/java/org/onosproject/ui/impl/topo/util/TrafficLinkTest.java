/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo.util;

import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.Load;
import org.onosproject.ui.impl.AbstractUiImplTest;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.topo.TopoUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for {@link TrafficLink}.
 */
public class TrafficLinkTest extends AbstractUiImplTest {

    private static final DeviceId D1 = deviceId("1");
    private static final DeviceId D2 = deviceId("2");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    private static final ConnectPoint SRC1 = new ConnectPoint(D1, P1);
    private static final ConnectPoint DST2 = new ConnectPoint(D2, P2);

    private static final RegionId RA = RegionId.regionId("rA");
    private static final RegionId RB = RegionId.regionId("rB");
    private static final String EXP_RA_RB = "rA~rB";


    private TrafficLink createALink() {
        Link linkIngress = DefaultEdgeLink.createEdgeLink(SRC1, true);
        LinkKey key = TopoUtils.canonicalLinkKey(checkNotNull(linkIngress));
        TrafficLink tl = new TrafficLink(key, linkIngress);
        Link linkEgress = DefaultEdgeLink.createEdgeLink(SRC1, false);
        tl.setOther(linkEgress);
        return tl;
    }

    @Test
    public void basic() {
        title("basic");

        TrafficLink tl = createALink();
        Load bigLoad = new DefaultLoad(2000, 0);
        tl.addLoad(bigLoad);
        print(tl);
        assertEquals("bad bytes value", 2000, tl.bytes());
        // NOTE: rate is bytes / period (10 seconds)
        assertEquals("bad rate value", 200, tl.rate());
        // this load does not represent flows
        assertEquals("bad flow count", 0, tl.flows());
    }

    @Test
    public void emptyStats() {
        title("emptyStats");
        UiLinkId uiLinkId = UiLinkId.uiLinkId(RA, RB);
        TrafficLink tl = new TrafficLink(uiLinkId);
        assertEquals("wrong id", EXP_RA_RB, tl.linkId());
    }

    @Test
    public void mergeStatsBytes() {
        title("mergeStatsBytes");
        TrafficLink tla = createALink();
        tla.addLoad(new DefaultLoad(2000, 0));
        print(tla);

        TrafficLink tlb = createALink();
        tlb.addLoad(new DefaultLoad(3000, 0));
        print(tlb);

        tla.mergeStats(tlb);
        print(tla);
        assertEquals("mergedBytes", 5000, tla.bytes());
        assertEquals("mergeRate", 500, tla.rate());
    }

    @Test
    public void mergeStatsFlows() {
        title("mergeStatsFlows");
        TrafficLink tla = createALink();
        tla.addFlows(9);
        print(tla);

        TrafficLink tlb = createALink();
        tlb.addFlows(16);
        print(tlb);

        tla.mergeStats(tlb);
        print(tla);
        assertEquals("mergedFlows", 25, tla.flows());
    }
}
