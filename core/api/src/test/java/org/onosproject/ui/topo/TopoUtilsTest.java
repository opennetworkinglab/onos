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

package org.onosproject.ui.topo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ui.AbstractUiTest;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.ConnectPoint.deviceConnectPoint;

/**
 * Unit tests for {@link TopoUtils}.
 */
public class TopoUtilsTest extends AbstractUiTest {
    private static final String AM_WL = "wrong label";
    private static final String AM_WM = "wrong magnitude";
    private static final String AM_CL = "clipped?";
    private static final String AM_NCL = "not clipped?";

    private static final ConnectPoint CP_FU = deviceConnectPoint("fu:001/3");
    private static final ConnectPoint CP_BAH = deviceConnectPoint("bah:002/5");

    private static Locale systemLocale;

    @BeforeClass
    public static void classSetup() {
        systemLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass
    public static void classTeardown() {
        Locale.setDefault(systemLocale);
    }

    private static final Link LINK_FU_BAH = DefaultLink.builder()
            .src(CP_FU)
            .dst(CP_BAH)
            .type(Link.Type.DIRECT)
            .providerId(ProviderId.NONE)
            .build();

    private static final Link LINK_BAH_FU = DefaultLink.builder()
            .src(CP_BAH)
            .dst(CP_FU)
            .type(Link.Type.DIRECT)
            .providerId(ProviderId.NONE)
            .build();

    private static final Link LINK_7_TO_3 = DefaultLink.builder()
            .src(deviceConnectPoint("of:0000000000000007/2"))
            .dst(deviceConnectPoint("of:0000000000000003/2"))
            .type(Link.Type.DIRECT)
            .providerId(ProviderId.NONE)
            .build();

    private static final Link LINK_3_TO_7 = DefaultLink.builder()
            .src(deviceConnectPoint("of:0000000000000003/2"))
            .dst(deviceConnectPoint("of:0000000000000007/2"))
            .type(Link.Type.DIRECT)
            .providerId(ProviderId.NONE)
            .build();


    private TopoUtils.ValueLabel vl;

    @Test
    public void linkStringFuBah() {
        String compact = TopoUtils.compactLinkString(LINK_FU_BAH);
        assertEquals("wrong link id", "fu:001/3-bah:002/5", compact);
    }

    @Test
    public void linkStringBahFu() {
        String compact = TopoUtils.compactLinkString(LINK_BAH_FU);
        assertEquals("wrong link id", "bah:002/5-fu:001/3", compact);
    }

    @Test
    public void canonLinkKey() {
        LinkKey fb = TopoUtils.canonicalLinkKey(LINK_FU_BAH);
        LinkKey bf = TopoUtils.canonicalLinkKey(LINK_BAH_FU);
        assertEquals("not canonical", fb, bf);
    }

    @Test
    public void canon723() {
        LinkKey lk1 = TopoUtils.canonicalLinkKey(LINK_7_TO_3);
        print(lk1);
        LinkKey lk2 = TopoUtils.canonicalLinkKey(LINK_3_TO_7);
        print(lk2);
        assertEquals("not canonical 3/7", lk1, lk2);
    }

    @Test
    public void formatSmallBytes() {
        vl = TopoUtils.formatBytes(1_000L);
        assertEquals(AM_WM, TopoUtils.Magnitude.ONE, vl.magnitude());
        assertEquals(AM_WL, "1,000 B", vl.toString());
    }

    @Test
    public void formatKiloBytes() {
        vl = TopoUtils.formatBytes(2_000L);
        assertEquals(AM_WM, TopoUtils.Magnitude.KILO, vl.magnitude());
        assertEquals(AM_WL, "1.95 KB", vl.toString());
    }

    @Test
    public void formatMegaBytes() {
        vl = TopoUtils.formatBytes(3_000_000L);
        assertEquals(AM_WM, TopoUtils.Magnitude.MEGA, vl.magnitude());
        assertEquals(AM_WL, "2.86 MB", vl.toString());
    }

    @Test
    public void formatGigaBytes() {
        vl = TopoUtils.formatBytes(4_000_000_000L);
        assertEquals(AM_WM, TopoUtils.Magnitude.GIGA, vl.magnitude());
        assertEquals(AM_WL, "3.73 GB", vl.toString());
    }

    @Test
    public void formatTeraBytes() {
        vl = TopoUtils.formatBytes(5_000_000_000_000L);
        assertEquals(AM_WM, TopoUtils.Magnitude.GIGA, vl.magnitude());
        assertEquals(AM_WL, "4,656.61 GB", vl.toString());
    }

    @Test
    public void formatPacketRateSmall() {
        vl = TopoUtils.formatPacketRate(37);
        assertEquals(AM_WL, "37 pps", vl.toString());
    }

    @Test
    public void formatPacketRateKilo() {
        vl = TopoUtils.formatPacketRate(1024);
        assertEquals(AM_WL, "1 Kpps", vl.toString());
    }

    @Test
    public void formatPacketRateKilo2() {
        vl = TopoUtils.formatPacketRate(1034);
        assertEquals(AM_WL, "1.01 Kpps", vl.toString());
    }

    @Test
    public void formatPacketRateMega() {
        vl = TopoUtils.formatPacketRate(9_000_000);
        assertEquals(AM_WL, "8.58 Mpps", vl.toString());
    }

    // remember for the following method calls, the input is in bytes!
    @Test
    public void formatClippedBitsSmall() {
        vl = TopoUtils.formatClippedBitRate(8);
        assertEquals(AM_WL, "64 bps", vl.toString());
        assertFalse(AM_CL, vl.clipped());
    }

    @Test
    public void formatClippedBitsKilo() {
        vl = TopoUtils.formatClippedBitRate(2_004);
        assertEquals(AM_WL, "15.66 Kbps", vl.toString());
        assertFalse(AM_CL, vl.clipped());
    }

    @Test
    public void formatClippedBitsMega() {
        vl = TopoUtils.formatClippedBitRate(3_123_123);
        assertEquals(AM_WL, "23.83 Mbps", vl.toString());
        assertFalse(AM_CL, vl.clipped());
    }

    @Test
    public void formatClippedBitsGiga() {
        vl = TopoUtils.formatClippedBitRate(500_000_000);
        assertEquals(AM_WL, "3.73 Gbps", vl.toString());
        assertFalse(AM_CL, vl.clipped());
    }

    @Test
    public void formatClippedBitsGigaExceedThreshold() {
        vl = TopoUtils.formatClippedBitRate(15_000_000_000L);
        // approx. 111.75 Gbps
        assertEquals(AM_WL, "100 Gbps", vl.toString());
        assertTrue(AM_NCL, vl.clipped());
    }

    @Test
    public void formatNoFlows() {
        String f = TopoUtils.formatFlows(0);
        assertEquals(AM_WL, "", f);
    }

    @Test
    public void formatNegativeFlows() {
        String f = TopoUtils.formatFlows(-3);
        assertEquals(AM_WL, "", f);
    }

    @Test
    public void formatOneFlow() {
        String f = TopoUtils.formatFlows(1);
        assertEquals(AM_WL, "1 flow", f);
    }

    @Test
    public void formatManyFlows() {
        String f = TopoUtils.formatFlows(42);
        assertEquals(AM_WL, "42 flows", f);
    }
}
