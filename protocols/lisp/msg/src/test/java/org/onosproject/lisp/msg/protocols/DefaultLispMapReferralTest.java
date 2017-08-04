/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onlab.packet.DeserializationException;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReferral.DefaultMapReferralBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReferral.MapReferralReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReferral.MapReferralWriter;
import org.onosproject.lisp.msg.protocols.DefaultLispReferralRecord.DefaultReferralRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapReferral.MapReferralBuilder;
import org.onosproject.lisp.msg.protocols.LispReferralRecord.ReferralRecordBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispMapReferral class.
 */
public final class DefaultLispMapReferralTest {

    private static final String IP_ADDRESS = "192.168.1.1";

    private LispMapReferral referral1;
    private LispMapReferral sameAsReferral1;
    private LispMapReferral referral2;

    @Before
    public void setup() {

        MapReferralBuilder builder1 = new DefaultMapReferralBuilder();

        List<LispReferralRecord> records1 =
                ImmutableList.of(getReferralRecord(), getReferralRecord());

        referral1 = builder1
                .withNonce(1L)
                .withReferralRecords(records1)
                .build();

        MapReferralBuilder builder2 = new DefaultMapReferralBuilder();

        List<LispReferralRecord> records2 =
                ImmutableList.of(getReferralRecord(), getReferralRecord());

        sameAsReferral1 = builder2
                .withNonce(1L)
                .withReferralRecords(records1)
                .build();

        MapReferralBuilder builder3 = new DefaultMapReferralBuilder();

        referral2 = builder3
                .withNonce(2L)
                .build();
    }

    private LispReferralRecord getReferralRecord() {
        ReferralRecordBuilder builder = new DefaultReferralRecordBuilder();

        LispIpv4Address ipv4Locator = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS));

        return builder
                .withRecordTtl(100)
                .withIsAuthoritative(true)
                .withIsIncomplete(false)
                .withMapVersionNumber((short) 1)
                .withMaskLength((byte) 0x01)
                .withAction(LispMapReplyAction.NativelyForward)
                .withEidPrefixAfi(ipv4Locator)
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
        LispMapReferral referral = referral1;

        assertThat(referral.getNonce(), is(1L));
        assertThat(referral.getRecordCount(), is(2));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException,
            LispParseError, DeserializationException {
        ByteBuf byteBuf = Unpooled.buffer();

        MapReferralWriter writer = new MapReferralWriter();
        writer.writeTo(byteBuf, referral1);

        MapReferralReader reader = new MapReferralReader();
        LispMapReferral deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(referral1, deserialized).testEquals();
    }
}
