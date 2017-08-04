/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.DefaultNotifyBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.NotifyReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.NotifyWriter;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.DefaultMapRecordBuilder;
import org.onosproject.lisp.msg.protocols.LispMapNotify.NotifyBuilder;
import org.onosproject.lisp.msg.protocols.LispMapRecord.MapRecordBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispMapNotify class.
 */
public final class DefaultLispMapNotifyTest {

    private static final String IP_ADDRESS = "192.168.1.1";

    private LispMapNotify notify1;
    private LispMapNotify sameAsNotify1;
    private LispMapNotify notify2;
    private static final String AUTH_KEY = "onos";

    @Before
    public void setup() {

        NotifyBuilder builder1 = new DefaultNotifyBuilder();

        List<LispMapRecord> records1 = ImmutableList.of(getMapRecord(), getMapRecord());

        notify1 = builder1
                        .withKeyId((short) 1)
                        .withAuthKey(AUTH_KEY)
                        .withNonce(1L)
                        .withMapRecords(records1)
                        .build();

        NotifyBuilder builder2 = new DefaultNotifyBuilder();

        List<LispMapRecord> records2 = ImmutableList.of(getMapRecord(), getMapRecord());

        sameAsNotify1 = builder2
                        .withKeyId((short) 1)
                        .withAuthKey(AUTH_KEY)
                        .withNonce(1L)
                        .withMapRecords(records2)
                        .build();

        NotifyBuilder builder3 = new DefaultNotifyBuilder();

        notify2 = builder3
                        .withKeyId((short) 2)
                        .withAuthKey(AUTH_KEY)
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
                .addEqualityGroup(notify1, sameAsNotify1)
                .addEqualityGroup(notify2).testEquals();
    }

    @Test
    public void testConstruction() {
        DefaultLispMapNotify notify = (DefaultLispMapNotify) notify1;

        assertThat(notify.getKeyId(), is((short) 1));
        assertThat(notify.getNonce(), is(1L));
        assertThat(notify.getRecordCount(), is(2));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        NotifyWriter writer = new NotifyWriter();
        writer.writeTo(byteBuf, notify1);

        NotifyReader reader = new NotifyReader();
        LispMapNotify deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(notify1, deserialized).testEquals();
    }
}
