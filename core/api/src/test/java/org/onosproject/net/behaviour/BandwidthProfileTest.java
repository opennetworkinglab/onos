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
package org.onosproject.net.behaviour;

import org.junit.Test;
import org.onlab.packet.DscpClass;
import org.onlab.util.Bandwidth;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import static org.onosproject.net.behaviour.BandwidthProfileAction.Action;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for BandwidthProfile class.
 */
public class BandwidthProfileTest {

    private static final long ONE = 1L;
    private static final long ONE_K = 1_000L;
    private static final long TWO_K = 2_000L;
    private static final long EIGHT_K = 8_000L;
    private static final long ONE_M = 1_000_000L;
    private static final long TEN_M = 10_000_000L;

    @Test
    public void testMeterConversion() {
        DeviceId deviceId = DeviceId.deviceId("netconf:10.0.0.1:22");
        ApplicationId appId = TestApplicationId.create("org.onosproject.foo.app");
        Meter.Builder meterBuilder = new DefaultMeter.Builder()
                .withId(MeterId.meterId(ONE))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .forDevice(deviceId)
                .burst();

        // Create Meter with single band
        Band band1 = DefaultBand.builder()
                .ofType(Band.Type.DROP)
                .withRate(TEN_M)
                .burstSize(TWO_K)
                .build();
        Meter meter = meterBuilder
                .fromApp(appId)
                .withBands(Arrays.asList(band1))
                .build();
        BandwidthProfile bandwidthProfile = BandwidthProfile.fromMeter(meter);

        assertEquals("wrong bw profile name",
                     bandwidthProfile.name(), meter.id().toString());
        assertEquals("wrong bw profile type",
                     bandwidthProfile.type(), BandwidthProfile.Type.sr2CM);
        assertEquals("wrong bw profile CIR",
                     bandwidthProfile.cir().bps(), band1.rate() * EIGHT_K, 0);
        assertEquals("wrong bw profile CBS",
                     (long) bandwidthProfile.cbs(), (long) band1.burst());
        assertNull(bandwidthProfile.pir());
        assertNull(bandwidthProfile.pbs());
        assertNull(bandwidthProfile.ebs());
        assertEquals("wrong green action",
                     bandwidthProfile.greenAction(),
                     getBuilder(Action.PASS).build());
        assertNull(bandwidthProfile.yellowAction());
        assertEquals("wrong red action",
                     bandwidthProfile.redAction(),
                     getBuilder(Action.DISCARD).build());
        assertEquals("wrong color-aware mode",
                     bandwidthProfile.colorAware(), false);

        // Create Meter with two bands
        Band band2 = DefaultBand.builder().burstSize(ONE_K)
                .ofType(Band.Type.REMARK)
                .dropPrecedence((short) 0b001010)
                .withRate(ONE_M)
                .build();
        meter = meterBuilder
                .fromApp(appId)
                .withBands(Arrays.asList(band1, band2))
                .build();
        bandwidthProfile = BandwidthProfile.fromMeter(meter);

        assertEquals("wrong bw profile name",
                     bandwidthProfile.name(), meter.id().toString());
        assertEquals("wrong bw profile type",
                     bandwidthProfile.type(), BandwidthProfile.Type.trTCM);
        assertEquals("wrong bw profile CIR",
                     bandwidthProfile.cir().bps(), band2.rate() * EIGHT_K, 0);
        assertEquals("wrong bw profile CBS",
                     (long) bandwidthProfile.cbs(), (long) band2.burst());
        assertEquals("wrong bw profile PIR",
                     bandwidthProfile.pir().bps(), band1.rate() * EIGHT_K, 0);
        assertEquals("wrong bw profile PBS",
                     (long) bandwidthProfile.pbs(), (long) band1.burst());
        assertNull(bandwidthProfile.ebs());
        assertEquals("wrong green action",
                     bandwidthProfile.greenAction(),
                     getBuilder(Action.PASS).build());
        assertEquals("wrong yellow action",
                     bandwidthProfile.yellowAction(),
                     getBuilder(Action.REMARK)
                             .dscpClass(DscpClass.AF11)
                             .build());
        assertEquals("wrong red action",
                     bandwidthProfile.redAction(),
                     getBuilder(Action.DISCARD).build());
        assertEquals("wrong color-aware mode",
                     bandwidthProfile.colorAware(), false);
    }

    @Test
    public void testType() {
        BandwidthProfile.Builder bwProfileBuilder = BandwidthProfile.builder()
                .name("profile")
                .cir(Bandwidth.bps(ONE_M))
                .cbs((int) ONE_K)
                .greenAction(getBuilder(Action.PASS).build())
                .redAction(getBuilder(Action.DISCARD).build())
                .colorAware(false);
        assertEquals("wrong bw profile type",
                     bwProfileBuilder.build().type(),
                     BandwidthProfile.Type.sr2CM);

        bwProfileBuilder.ebs((int) TWO_K)
                .yellowAction(getBuilder(Action.REMARK)
                                      .dscpClass(DscpClass.AF11)
                                      .build());
        assertEquals("wrong bw profile type",
                     bwProfileBuilder.build().type(),
                     BandwidthProfile.Type.srTCM);
        bwProfileBuilder.ebs(null);

        bwProfileBuilder.pir(Bandwidth.bps(TEN_M))
                .pbs((int) TWO_K);
        assertEquals("wrong bw profile type",
                     bwProfileBuilder.build().type(),
                     BandwidthProfile.Type.trTCM);
    }

    @Test
    public void testEquals() {
        BandwidthProfile bwProfile1 = new BandwidthProfile.Builder()
                .name("profile1")
                .cir(Bandwidth.bps(ONE_M))
                .cbs((int) ONE_K)
                .pir(Bandwidth.bps(TEN_M))
                .pbs((int) TWO_K)
                .greenAction(getBuilder(Action.PASS).build())
                .yellowAction(getBuilder(Action.REMARK)
                                      .dscpClass(DscpClass.AF11)
                                      .build())
                .redAction(getBuilder(Action.DISCARD).build())
                .colorAware(false)
                .build();
        BandwidthProfile bwProfile2 = new BandwidthProfile.Builder()
                .name("profile2")
                .cir(Bandwidth.bps(ONE_M))
                .cbs((int) ONE_K)
                .pir(Bandwidth.bps(TEN_M))
                .pbs((int) TWO_K)
                .greenAction(getBuilder(Action.PASS).build())
                .yellowAction(getBuilder(Action.REMARK)
                                      .dscpClass(DscpClass.AF11)
                                      .build())
                .redAction(getBuilder(Action.DISCARD).build())
                .colorAware(false)
                .build();
        assertTrue("wrong equals method", bwProfile1.equals(bwProfile2));
    }

    private static BandwidthProfileAction.Builder getBuilder(Action action) {
        return new BandwidthProfileAction.Builder().action(action);
    }
}
