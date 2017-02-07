/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onlab.packet.DeserializationException;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispReferral.DefaultReferralBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispReferral.ReferralReader;
import org.onosproject.lisp.msg.protocols.DefaultLispReferral.ReferralWriter;
import org.onosproject.lisp.msg.protocols.LispReferral.ReferralBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispReferral class.
 */
public final class DefaultLispReferralTest {

    private static final String IP_ADDRESS1 = "192.168.1.1";
    private static final String IP_ADDRESS2 = "192.168.1.2";

    private LispReferral referral1;
    private LispReferral sameAsReferral1;
    private LispReferral referral2;

    @Before
    public void setup() {

        ReferralBuilder builder1 = new DefaultReferralBuilder();

        LispIpv4Address ipv4Address1 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS1));

        referral1 = builder1
                        .withPriority((byte) 0x01)
                        .withWeight((byte) 0x01)
                        .withMulticastPriority((byte) 0x01)
                        .withMulticastWeight((byte) 0x01)
                        .withLocalLocator(true)
                        .withRlocProbed(false)
                        .withRouted(true)
                        .withLocatorAfi(ipv4Address1)
                        .build();

        ReferralBuilder builder2 = new DefaultReferralBuilder();

        sameAsReferral1 = builder2
                            .withPriority((byte) 0x01)
                            .withWeight((byte) 0x01)
                            .withMulticastPriority((byte) 0x01)
                            .withMulticastWeight((byte) 0x01)
                            .withLocalLocator(true)
                            .withRlocProbed(false)
                            .withRouted(true)
                            .withLocatorAfi(ipv4Address1)
                            .build();

        ReferralBuilder builder3 = new DefaultReferralBuilder();

        LispIpv4Address ipv4Address2 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS2));

        referral2 = builder3
                .withPriority((byte) 0x02)
                .withWeight((byte) 0x02)
                .withMulticastPriority((byte) 0x02)
                .withMulticastWeight((byte) 0x02)
                .withLocalLocator(false)
                .withRlocProbed(true)
                .withRouted(false)
                .withLocatorAfi(ipv4Address2)
                .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(referral1, sameAsReferral1)
                .addEqualityGroup(referral2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispReferral referral = referral1;

        LispIpv4Address ipv4Address =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS1));

        assertThat(referral.getPriority(), is((byte) 0x01));
        assertThat(referral.getWeight(), is((byte) 0x01));
        assertThat(referral.getMulticastPriority(), is((byte) 0x01));
        assertThat(referral.getMulticastWeight(), is((byte) 0x01));
        assertThat(referral.isLocalLocator(), is(true));
        assertThat(referral.isRlocProbed(), is(false));
        assertThat(referral.isRouted(), is(true));
        assertThat(referral.getLocatorAfi(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException,
                                    LispParseError, DeserializationException {
        ByteBuf byteBuf = Unpooled.buffer();

        ReferralWriter writer = new ReferralWriter();
        writer.writeTo(byteBuf, referral1);

        ReferralReader reader = new ReferralReader();
        LispReferral deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(referral1, deserialized).testEquals();
    }
}
