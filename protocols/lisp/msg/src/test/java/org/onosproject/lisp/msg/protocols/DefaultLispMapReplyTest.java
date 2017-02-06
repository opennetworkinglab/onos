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
import org.onosproject.lisp.msg.protocols.DefaultLispMapReply.ReplyReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReply.ReplyWriter;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.MapRecordBuilder;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapReply.DefaultReplyBuilder;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapReply.ReplyBuilder;

/**
 * Unit tests for DefaultLispMapReply class.
 */
public final class DefaultLispMapReplyTest {

    private static final String IP_ADDRESS = "192.168.1.1";

    private LispMapReply reply1;
    private LispMapReply sameAsReply1;
    private LispMapReply reply2;

    @Before
    public void setup() {

        ReplyBuilder builder1 = new DefaultReplyBuilder();

        List<LispMapRecord> records1 = ImmutableList.of(getMapRecord(), getMapRecord());

        reply1 = builder1
                        .withIsEtr(true)
                        .withIsProbe(false)
                        .withIsSecurity(true)
                        .withNonce(1L)
                        .withMapRecords(records1)
                        .build();

        ReplyBuilder builder2 = new DefaultReplyBuilder();

        List<LispMapRecord> records2 = ImmutableList.of(getMapRecord(), getMapRecord());

        sameAsReply1 = builder2
                        .withIsEtr(true)
                        .withIsProbe(false)
                        .withIsSecurity(true)
                        .withNonce(1L)
                        .withMapRecords(records2)
                        .build();

        ReplyBuilder builder3 = new DefaultReplyBuilder();

        reply2 = builder3
                        .withIsEtr(false)
                        .withIsProbe(true)
                        .withIsSecurity(false)
                        .withNonce(2L)
                        .build();
    }

    private LispMapRecord getMapRecord() {
        MapRecordBuilder builder1 = new DefaultMapRecordBuilder();

        LispIpv4Address ipv4Locator1 = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS));

        return builder1
                .withRecordTtl(100)
                .withIsAuthoritative(true)
                .withMapVersionNumber((short) 1)
                .withMaskLength((byte) 0x01)
                .withAction(LispMapReplyAction.NativelyForward)
                .withEidPrefixAfi(ipv4Locator1)
                .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(reply1, sameAsReply1)
                .addEqualityGroup(reply2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispMapReply reply = (DefaultLispMapReply) reply1;

        assertThat(reply.isEtr(), is(true));
        assertThat(reply.isProbe(), is(false));
        assertThat(reply.isSecurity(), is(true));
        assertThat(reply.getNonce(), is(1L));
        assertThat(reply.getRecordCount(), is(2));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();
        ReplyWriter writer = new ReplyWriter();
        writer.writeTo(byteBuf, reply1);

        ReplyReader reader = new ReplyReader();
        LispMapReply deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(reply1, deserialized).testEquals();
    }
}
