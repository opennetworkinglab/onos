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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoReply.DefaultInfoReplyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoReply.InfoReplyReader;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoReply.InfoReplyWriter;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress;
import org.onosproject.lisp.msg.protocols.LispInfoReply.InfoReplyBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress.NatAddressBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispInfoReply class.
 */
public final class DefaultLispInfoReplyTest {

    private LispInfoReply reply1;
    private LispInfoReply sameAsReply1;
    private LispInfoReply reply2;

    private static final String AUTH_KEY = "onos";

    @Before
    public void setup() {

        InfoReplyBuilder builder1 = new DefaultInfoReplyBuilder();

        short msUdpPortNumber1 = 80;
        short etrUdpPortNumber1 = 100;
        LispIpv4Address globalEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address msRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));
        LispIpv4Address privateEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.3"));

        LispIpv4Address address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.4"));

        LispNatLcafAddress natLcafAddress1 = new NatAddressBuilder()
                                                    .withLength((short) 0)
                                                    .withMsUdpPortNumber(msUdpPortNumber1)
                                                    .withEtrUdpPortNumber(etrUdpPortNumber1)
                                                    .withGlobalEtrRlocAddress(globalEtrRlocAddress1)
                                                    .withMsRlocAddress(msRlocAddress1)
                                                    .withPrivateEtrRlocAddress(privateEtrRlocAddress1)
                                                    .build();

        reply1 = builder1
                    .withNonce(1L)
                    .withKeyId((short) 1)
                    .withAuthKey(AUTH_KEY)
                    .withIsInfoReply(true)
                    .withMaskLength((byte) 1)
                    .withEidPrefix(address1)
                    .withNatLcafAddress(natLcafAddress1).build();

        InfoReplyBuilder builder2 = new DefaultInfoReplyBuilder();

        sameAsReply1 = builder2
                            .withNonce(1L)
                            .withKeyId((short) 1)
                            .withAuthKey(AUTH_KEY)
                            .withIsInfoReply(true)
                            .withMaskLength((byte) 1)
                            .withEidPrefix(address1)
                            .withNatLcafAddress(natLcafAddress1).build();

        InfoReplyBuilder builder3 = new DefaultInfoReplyBuilder();

        short msUdpPortNumber2 = 81;
        short etrUdpPortNumber2 = 101;
        LispIpv4Address globalEtrRlocAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));
        LispIpv4Address msRlocAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.2"));
        LispIpv4Address privateEtrRlocAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.3"));

        LispIpv4Address address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.4"));

        LispNatLcafAddress natLcafAddress2 = new NatAddressBuilder()
                                                    .withLength((short) 0)
                                                    .withMsUdpPortNumber(msUdpPortNumber2)
                                                    .withEtrUdpPortNumber(etrUdpPortNumber2)
                                                    .withGlobalEtrRlocAddress(globalEtrRlocAddress2)
                                                    .withMsRlocAddress(msRlocAddress2)
                                                    .withPrivateEtrRlocAddress(privateEtrRlocAddress2)
                                                    .build();

        reply2 = builder3
                        .withNonce(2L)
                        .withKeyId((short) 2)
                        .withAuthKey(AUTH_KEY)
                        .withIsInfoReply(true)
                        .withMaskLength((byte) 1)
                        .withEidPrefix(address2)
                        .withNatLcafAddress(natLcafAddress2).build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(reply1, sameAsReply1)
                .addEqualityGroup(reply2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispInfoReply reply = (DefaultLispInfoReply) reply1;

        LispIpv4Address address = new LispIpv4Address(IpAddress.valueOf("192.168.1.4"));

        short msUdpPortNumber1 = 80;
        short etrUdpPortNumber1 = 100;
        LispIpv4Address globalEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address msRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));
        LispIpv4Address privateEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.3"));

        LispNatLcafAddress natLcafAddress = new NatAddressBuilder()
                .withLength((short) 0)
                .withMsUdpPortNumber(msUdpPortNumber1)
                .withEtrUdpPortNumber(etrUdpPortNumber1)
                .withGlobalEtrRlocAddress(globalEtrRlocAddress1)
                .withMsRlocAddress(msRlocAddress1)
                .withPrivateEtrRlocAddress(privateEtrRlocAddress1)
                .build();

        assertThat(reply.isInfoReply(), is(true));
        assertThat(reply.getNonce(), is(1L));
        assertThat(reply.getKeyId(), is((short) 1));
        assertThat(reply.getMaskLength(), is((byte) 1));
        assertThat(reply.getPrefix(), is(address));
        assertThat(reply.getNatLcafAddress(), is(natLcafAddress));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        InfoReplyWriter writer = new InfoReplyWriter();
        writer.writeTo(byteBuf, reply1);

        InfoReplyReader reader = new InfoReplyReader();
        LispInfoReply deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(reply1, deserialized).testEquals();
    }
}
