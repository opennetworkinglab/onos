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
package org.onosproject.net.meter;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;


/**
 * DefaultMeter Tests.
 */
public class DefaultMeterTest {

    private Meter m1;
    private Meter sameAsm1;
    private Meter m2;
    private Meter m3;

    @Before
    public void setup() {

        Band band = DefaultBand.builder()
                .ofType(Band.Type.DROP)
                .withRate(500)
                .build();

        Band band1 = DefaultBand.builder()
                .ofType(Band.Type.REMARK)
                .withRate(500)
                .dropPrecedence((short) 1)
                .build();

        m1 = DefaultMeter.builder()
                .forDevice(did("1"))
                .fromApp(APP_ID)
                .withId(MeterId.meterId(1))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        sameAsm1 = DefaultMeter.builder()
                .forDevice(did("1"))
                .fromApp(APP_ID)
                .withId(MeterId.meterId(1))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        m2 = DefaultMeter.builder()
                .forDevice(did("2"))
                .fromApp(APP_ID)
                .withId(MeterId.meterId(2))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band))
                .build();

        m3 = DefaultMeter.builder()
                .forDevice(did("3"))
                .fromApp(APP_ID)
                .withId(MeterId.meterId(3))
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singletonList(band1))
                .build();


    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(m1, sameAsm1)
                .addEqualityGroup(m2)
                .addEqualityGroup(m3).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultMeter m = (DefaultMeter) m1;

        assertThat(m.deviceId(), is(did("1")));
        assertThat(m.appId(), is(APP_ID));
        assertThat(m.id(), is(MeterId.meterId(1)));
        assertThat(m.isBurst(), is(false));

        assertThat(m.life(), is(0L));
        assertThat(m.bytesSeen(), is(0L));
        assertThat(m.packetsSeen(), is(0L));
        assertThat(m.referenceCount(), is(0L));

    }




}
