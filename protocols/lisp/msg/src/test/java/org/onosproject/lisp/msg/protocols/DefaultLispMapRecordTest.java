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
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.*;

/**
 * Unit tests for DefaultLispMapRecord class.
 */
public final class DefaultLispMapRecordTest {

    private static final String IP_ADDRESS_1 = "192.168.1.1";
    private static final String IP_ADDRESS_2 = "192.168.1.2";

    private LispMapRecord record1;
    private LispMapRecord sameAsRecord1;
    private LispMapRecord record2;

    @Before
    public void setup() {

        MapRecordBuilder builder1 = new DefaultMapRecordBuilder();

        LispIpv4Address ipv4Locator1 = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        record1 = builder1
                        .withRecordTtl(100)
                        .withIsAuthoritative(true)
                        .withMapVersionNumber((short) 1)
                        .withMaskLength((byte) 0x01)
                        .withAction(LispMapReplyAction.NativelyForward)
                        .withEidPrefixAfi(ipv4Locator1)
                        .build();

        MapRecordBuilder builder2 = new DefaultMapRecordBuilder();

        sameAsRecord1 = builder2
                        .withRecordTtl(100)
                        .withIsAuthoritative(true)
                        .withMapVersionNumber((short) 1)
                        .withMaskLength((byte) 0x01)
                        .withAction(LispMapReplyAction.NativelyForward)
                        .withEidPrefixAfi(ipv4Locator1)
                        .build();

        MapRecordBuilder builder3 = new DefaultMapRecordBuilder();

        LispIpv4Address ipv4Locator2 = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_2));

        record2 = builder3
                        .withRecordTtl(200)
                        .withIsAuthoritative(false)
                        .withMapVersionNumber((short) 2)
                        .withMaskLength((byte) 0x02)
                        .withAction(LispMapReplyAction.Drop)
                        .withEidPrefixAfi(ipv4Locator2)
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
        DefaultLispMapRecord record = (DefaultLispMapRecord) record1;

        LispIpv4Address ipv4Locator = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        assertThat(record.getRecordTtl(), is(100));
        assertThat(record.isAuthoritative(), is(true));
        assertThat(record.getMapVersionNumber(), is((short) 1));
        assertThat(record.getMaskLength(), is((byte) 0x01));
        assertThat(record.getAction(), is(LispMapReplyAction.NativelyForward));
        assertThat(record.getEidPrefixAfi(), is(ipv4Locator));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        MapRecordWriter writer = new MapRecordWriter();
        writer.writeTo(byteBuf, record1);

        MapRecordReader reader = new MapRecordReader();
        LispMapRecord deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(record1, deserialized).testEquals();
    }
}
