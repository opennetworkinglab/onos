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
package org.onosproject.lisp.msg.types.lcaf;

import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.lcaf.LispSourceDestLcafAddress.SourceDestAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispSourceDestLcafAddress.SourceDestLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispSourceDestLcafAddress.SourceDestLcafAddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispSourceDestLcafAddress class.
 */
public class LispSourceDestLcafAddressTest {

    private LispSourceDestLcafAddress address1;
    private LispSourceDestLcafAddress sameAsAddress1;
    private LispSourceDestLcafAddress address2;

    @Before
    public void setup() {

        SourceDestAddressBuilder builder1 = new SourceDestAddressBuilder();

        LispIpv4Address srcAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address dstAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));

        address1 = builder1
                        .withReserved((short) 1)
                        .withSrcMaskLength((byte) 0x01)
                        .withDstMaskLength((byte) 0x01)
                        .withSrcPrefix(srcAddress1)
                        .withDstPrefix(dstAddress1)
                        .build();

        SourceDestAddressBuilder builder2 = new SourceDestAddressBuilder();

        sameAsAddress1 = builder2
                            .withReserved((short) 1)
                            .withSrcMaskLength((byte) 0x01)
                            .withDstMaskLength((byte) 0x01)
                            .withSrcPrefix(srcAddress1)
                            .withDstPrefix(dstAddress1)
                            .build();

        SourceDestAddressBuilder builder3 = new SourceDestAddressBuilder();

        LispIpv4Address srcAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));
        LispIpv4Address dstAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.2"));

        address2 = builder3
                        .withReserved((short) 2)
                        .withSrcMaskLength((byte) 0x02)
                        .withDstMaskLength((byte) 0x02)
                        .withSrcPrefix(srcAddress2)
                        .withDstPrefix(dstAddress2)
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
        LispSourceDestLcafAddress sourceDestLcafAddress = address1;

        LispIpv4Address srcAddress = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address dstAddress = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));

        assertThat(sourceDestLcafAddress.getReserved(), is((short) 1));
        assertThat(sourceDestLcafAddress.getSrcMaskLength(), is((byte) 0x01));
        assertThat(sourceDestLcafAddress.getDstMaskLength(), is((byte) 0x01));
        assertThat(sourceDestLcafAddress.getSrcPrefix(), is(srcAddress));
        assertThat(sourceDestLcafAddress.getDstPrefix(), is(dstAddress));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        SourceDestLcafAddressWriter writer = new SourceDestLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        SourceDestLcafAddressReader reader = new SourceDestLcafAddressReader();
        LispSourceDestLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
