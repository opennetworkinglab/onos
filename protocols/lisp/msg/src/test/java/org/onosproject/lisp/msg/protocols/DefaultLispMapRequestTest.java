/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.protocols;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispMapRequest class.
 */
public final class DefaultLispMapRequestTest {

    private LispMapRequest request1;
    private LispMapRequest sameAsRequest1;
    private LispMapRequest request2;

    @Before
    public void setup() {

        LispMapRequest.RequestBuilder builder1 =
                        new DefaultLispMapRequest.DefaultRequestBuilder();

        request1 = builder1
                        .withIsAuthoritative(true)
                        .withIsMapDataPresent(true)
                        .withIsPitr(false)
                        .withIsProbe(false)
                        .withIsSmr(true)
                        .withIsSmrInvoked(false)
                        .withNonce(1L)
                        .withRecordCount((byte) 0x01)
                        .build();

        LispMapRequest.RequestBuilder builder2 =
                        new DefaultLispMapRequest.DefaultRequestBuilder();

        sameAsRequest1 = builder2
                        .withIsAuthoritative(true)
                        .withIsMapDataPresent(true)
                        .withIsPitr(false)
                        .withIsProbe(false)
                        .withIsSmr(true)
                        .withIsSmrInvoked(false)
                        .withNonce(1L)
                        .withRecordCount((byte) 0x01)
                        .build();

        LispMapRequest.RequestBuilder builder3 =
                        new DefaultLispMapRequest.DefaultRequestBuilder();

        request2 = builder3
                        .withIsAuthoritative(false)
                        .withIsMapDataPresent(false)
                        .withIsPitr(true)
                        .withIsProbe(true)
                        .withIsSmr(false)
                        .withIsSmrInvoked(true)
                        .withNonce(2L)
                        .withRecordCount((byte) 0x02)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(request1, sameAsRequest1)
                .addEqualityGroup(request2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispMapRequest request = (DefaultLispMapRequest) request1;

        assertThat(request.isAuthoritative(), is(true));
        assertThat(request.isMapDataPresent(), is(true));
        assertThat(request.isPitr(), is(false));
        assertThat(request.isProbe(), is(false));
        assertThat(request.isSmr(), is(true));
        assertThat(request.isSmrInvoked(), is(false));
        assertThat(request.getNonce(), is(1L));
        assertThat(request.getRecordCount(), is((byte) 0x01));
    }
}
