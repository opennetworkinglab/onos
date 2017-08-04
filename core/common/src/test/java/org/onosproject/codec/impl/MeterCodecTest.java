/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.MeterJsonMatcher.matchesMeter;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for Meter codec.
 */
public class MeterCodecTest {

    MockCodecContext context;
    JsonCodec<Meter> meterCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    /**
     * Sets up for each test.  Creates a context and fetches the meter
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        meterCodec = context.codec(Meter.class);
        assertThat(meterCodec, notNullValue());

        expect(mockCoreService.registerApplication(MeterCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests encoding of a Meter object.
     */
    @Test
    public void testMeterEncode() {
        Band band1 = DefaultBand.builder()
                        .ofType(Band.Type.DROP)
                        .burstSize(10)
                        .withRate(10).build();
        Band band2 = DefaultBand.builder()
                        .ofType(Band.Type.REMARK)
                        .burstSize(10)
                        .withRate(10)
                        .dropPrecedence((short) 10).build();

        Meter meter = DefaultMeter.builder()
                        .fromApp(APP_ID)
                        .withId(MeterId.meterId(1L))
                        .forDevice(NetTestTools.did("d1"))
                        .withBands(ImmutableList.of(band1, band2))
                        .withUnit(Meter.Unit.KB_PER_SEC).build();

        ObjectNode meterJson = meterCodec.encode(meter, context);
        assertThat(meterJson, matchesMeter(meter));
    }
}
