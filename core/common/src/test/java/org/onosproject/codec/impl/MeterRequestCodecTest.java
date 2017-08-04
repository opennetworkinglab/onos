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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.MeterRequest;

import java.io.IOException;
import java.io.InputStream;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for MeterRequest codec.
 */
public class MeterRequestCodecTest {
    MockCodecContext context;
    JsonCodec<MeterRequest> meterRequestCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    /**
     * Sets up for each test.  Creates a context and fetches the meterRequest
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        meterRequestCodec = context.codec(MeterRequest.class);
        assertThat(meterRequestCodec, notNullValue());

        expect(mockCoreService.registerApplication(MeterCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Test decoding of a MeterRequest object.
     */
    @Test
    public void testMeterRequestDecode() throws IOException {
        MeterRequest meterRequest = getMeterRequest("simple-meter-request.json");
        checkCommonData(meterRequest);

        assertThat(meterRequest.bands().size(), is(1));
        Band band = meterRequest.bands().iterator().next();
        assertThat(band.type().toString(), is("REMARK"));
        assertThat(band.rate(), is(10L));
        assertThat(band.dropPrecedence(), is((short) 20));
        assertThat(band.burst(), is(30L));
    }

    /**
     * Checks that the data shared by all the resource is correct for a given meterRequest.
     *
     * @param meterRequest meterRequest to check
     */
    private void checkCommonData(MeterRequest meterRequest) {
        assertThat(meterRequest.deviceId().toString(), is("of:0000000000000001"));
        assertThat(meterRequest.appId(), is(APP_ID));
        assertThat(meterRequest.unit().toString(), is("KB_PER_SEC"));
    }

    /**
     * Reads in a meter from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded meterRequest
     * @throws IOException if processing the resource fails
     */
    private MeterRequest getMeterRequest(String resourceName) throws IOException {
        InputStream jsonStream = MeterRequestCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MeterRequest meterRequest = meterRequestCodec.decode((ObjectNode) json, context);
        assertThat(meterRequest, notNullValue());
        return meterRequest;
    }
}
