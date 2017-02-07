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
import org.onosproject.lisp.msg.protocols.DefaultLispReferralRecord.DefaultReferralRecordBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispReferralRecord.ReferralRecordReader;
import org.onosproject.lisp.msg.protocols.DefaultLispReferralRecord.ReferralRecordWriter;
import org.onosproject.lisp.msg.protocols.LispReferralRecord.ReferralRecordBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispReferralRecord class.
 */
public final class DefaultLispReferralRecordTest {

    private static final String IP_ADDRESS1 = "192.168.1.1";
    private static final String IP_ADDRESS2 = "192.168.1.2";

    private LispReferralRecord record1;
    private LispReferralRecord sameAsRecord1;
    private LispReferralRecord record2;

    @Before
    public void setup() {

        ReferralRecordBuilder builder1 = new DefaultReferralRecordBuilder();

        LispIpv4Address ipv4Address1 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS1));

        record1 = builder1
                    .withRecordTtl(100)
                    .withIsAuthoritative(true)
                    .withIsIncomplete(false)
                    .withMapVersionNumber((short) 1)
                    .withMaskLength((byte) 0x01)
                    .withAction(LispMapReplyAction.NativelyForward)
                    .withEidPrefixAfi(ipv4Address1)
                    .build();

        ReferralRecordBuilder builder2 = new DefaultReferralRecordBuilder();

        sameAsRecord1 = builder2
                            .withRecordTtl(100)
                            .withIsAuthoritative(true)
                            .withIsIncomplete(false)
                            .withMapVersionNumber((short) 1)
                            .withMaskLength((byte) 0x01)
                            .withAction(LispMapReplyAction.NativelyForward)
                            .withEidPrefixAfi(ipv4Address1)
                            .build();

        ReferralRecordBuilder builder3 = new DefaultReferralRecordBuilder();

        LispIpv4Address ipv4Address2 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS2));

        record2 = builder3
                        .withRecordTtl(200)
                        .withIsAuthoritative(false)
                        .withIsIncomplete(true)
                        .withMapVersionNumber((short) 2)
                        .withMaskLength((byte) 0x02)
                        .withAction(LispMapReplyAction.Drop)
                        .withEidPrefixAfi(ipv4Address2)
                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(record1, sameAsRecord1)
                .addEqualityGroup(record2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispReferralRecord record = record1;

        LispIpv4Address ipv4Address1 =
                        new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS1));

        assertThat(record.getRecordTtl(), is(100));
        assertThat(record.isAuthoritative(), is(true));
        assertThat(record.isIncomplete(), is(false));
        assertThat(record.getMapVersionNumber(), is((short) 1));
        assertThat(record.getMaskLength(), is((byte) 0x01));
        assertThat(record.getAction(), is(LispMapReplyAction.NativelyForward));
        assertThat(record.getEidPrefixAfi(), is(ipv4Address1));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException,
                                    LispParseError, DeserializationException {
        ByteBuf byteBuf = Unpooled.buffer();

        ReferralRecordWriter writer = new ReferralRecordWriter();
        writer.writeTo(byteBuf, record1);

        ReferralRecordReader reader = new ReferralRecordReader();
        LispReferralRecord deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(record1, deserialized).testEquals();
    }
}
