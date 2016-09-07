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
package org.onosproject.lisp.msg.types;

import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.types.LispSegmentLcafAddress.SegmentLcafAddressReader;
import static org.onosproject.lisp.msg.types.LispSegmentLcafAddress.SegmentLcafAddressWriter;

/**
 * Unit tests for LispSegmentLcafAddress class.
 */
public class LispSegmentLcafAddressTest {

    private LispSegmentLcafAddress address1;
    private LispSegmentLcafAddress sameAsAddress1;
    private LispSegmentLcafAddress address2;

    @Before
    public void setup() {

        LispSegmentLcafAddress.SegmentAddressBuilder builder1 =
                            new LispSegmentLcafAddress.SegmentAddressBuilder();

        LispIpv4Address ipv4Address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        address1 = builder1
                        .withIdMaskLength((byte) 0x01)
                        .withInstanceId(1)
                        .withAddress(ipv4Address1)
                        .build();

        LispSegmentLcafAddress.SegmentAddressBuilder builder2 =
                            new LispSegmentLcafAddress.SegmentAddressBuilder();

        sameAsAddress1 = builder2
                            .withIdMaskLength((byte) 0x01)
                            .withInstanceId(1)
                            .withAddress(ipv4Address1)
                            .build();

        LispSegmentLcafAddress.SegmentAddressBuilder builder3 =
                            new LispSegmentLcafAddress.SegmentAddressBuilder();

        LispIpv4Address ipv4Address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));
        address2 = builder3
                        .withIdMaskLength((byte) 0x02)
                        .withInstanceId(2)
                        .withAddress(ipv4Address2)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispSegmentLcafAddress segmentLcafAddress = address1;

        LispIpv4Address ipv4Address = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        assertThat(segmentLcafAddress.getIdMaskLength(), is((byte) 0x01));
        assertThat(segmentLcafAddress.getInstanceId(), is(1));
        assertThat(segmentLcafAddress.getAddress(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        SegmentLcafAddressWriter writer = new SegmentLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        SegmentLcafAddressReader reader = new SegmentLcafAddressReader();
        LispSegmentLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(address1, deserialized).testEquals();
    }
}
