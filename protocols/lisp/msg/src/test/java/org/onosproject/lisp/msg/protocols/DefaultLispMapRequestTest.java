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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRequest.*;

/**
 * Unit tests for DefaultLispMapRequest class.
 */
public final class DefaultLispMapRequestTest {

    private LispMapRequest request1;
    private LispMapRequest sameAsRequest1;
    private LispMapRequest request2;

    @Before
    public void setup() {

        RequestBuilder builder1 = new DefaultRequestBuilder();

        LispIpv4Address ipv4Eid1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        LispIpv4Address ipv4Rloc1 = new LispIpv4Address(IpAddress.valueOf("10.1.1.1"));
        LispIpv4Address ipv4Rloc2 = new LispIpv4Address(IpAddress.valueOf("10.1.1.2"));

        List<LispAfiAddress> rlocs1 = ImmutableList.of(ipv4Rloc1, ipv4Rloc2);
        List<LispEidRecord> records1 = ImmutableList.of(getEidRecord(), getEidRecord());

        request1 = builder1
                        .withIsAuthoritative(true)
                        .withIsMapDataPresent(true)
                        .withIsPitr(false)
                        .withIsProbe(false)
                        .withIsSmr(true)
                        .withIsSmrInvoked(false)
                        .withSourceEid(ipv4Eid1)
                        .withItrRlocs(rlocs1)
                        .withEidRecords(records1)
                        .withNonce(1L)
                        .build();

        RequestBuilder builder2 = new DefaultRequestBuilder();
        List<LispEidRecord> records2 = ImmutableList.of(getEidRecord(), getEidRecord());

        sameAsRequest1 = builder2
                        .withIsAuthoritative(true)
                        .withIsMapDataPresent(true)
                        .withIsPitr(false)
                        .withIsProbe(false)
                        .withIsSmr(true)
                        .withIsSmrInvoked(false)
                        .withSourceEid(ipv4Eid1)
                        .withItrRlocs(rlocs1)
                        .withEidRecords(records2)
                        .withNonce(1L)
                        .build();

        RequestBuilder builder3 = new DefaultRequestBuilder();

        LispIpv4Address ipv4Eid2 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));

        LispIpv4Address ipv4Rloc3 = new LispIpv4Address(IpAddress.valueOf("20.1.1.1"));
        LispIpv4Address ipv4Rloc4 = new LispIpv4Address(IpAddress.valueOf("20.1.1.2"));

        List<LispAfiAddress> rlocs2 = ImmutableList.of(ipv4Rloc3, ipv4Rloc4);

        request2 = builder3
                        .withIsAuthoritative(false)
                        .withIsMapDataPresent(false)
                        .withIsPitr(true)
                        .withIsProbe(true)
                        .withIsSmr(false)
                        .withIsSmrInvoked(true)
                        .withSourceEid(ipv4Eid2)
                        .withItrRlocs(rlocs2)
                        .withNonce(2L)
                        .build();
    }

    private LispEidRecord getEidRecord() {
        LispIpv4Address eid = new LispIpv4Address(IpAddress.valueOf("20.1.1.1"));
        return new LispEidRecord((byte) 24, eid);
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
        assertThat(request.getRecordCount(), is(2));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();
        RequestWriter writer = new RequestWriter();
        writer.writeTo(byteBuf, request1);

        RequestReader reader = new RequestReader();
        LispMapRequest deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(request1, deserialized).testEquals();
    }
}
