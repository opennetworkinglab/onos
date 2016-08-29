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
import org.onosproject.lisp.msg.types.LispIpv4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.protocols.DefaultLispLocatorRecord.*;

/**
 * Unit tests for DefaultLispLocatorRecord class.
 */
public final class DefaultLispLocatorRecordTest {

    private LispLocatorRecord record1;
    private LispLocatorRecord sameAsRecord1;
    private LispLocatorRecord record2;

    @Before
    public void setup() {

        LispLocatorRecord.LocatorRecordBuilder builder1 =
                    new DefaultLocatorRecordBuilder();

        LispIpv4Address ipv4Locator1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        record1 = builder1
                        .withPriority((byte) 0x01)
                        .withWeight((byte) 0x01)
                        .withMulticastPriority((byte) 0x01)
                        .withMulticastWeight((byte) 0x01)
                        .withLocalLocator(true)
                        .withRlocProbed(false)
                        .withRouted(true)
                        .withLocatorAfi(ipv4Locator1)
                        .build();

        LispLocatorRecord.LocatorRecordBuilder builder2 =
                    new DefaultLocatorRecordBuilder();

        sameAsRecord1 = builder2
                        .withPriority((byte) 0x01)
                        .withWeight((byte) 0x01)
                        .withMulticastPriority((byte) 0x01)
                        .withMulticastWeight((byte) 0x01)
                        .withLocalLocator(true)
                        .withRlocProbed(false)
                        .withRouted(true)
                        .withLocatorAfi(ipv4Locator1)
                        .build();

        LispLocatorRecord.LocatorRecordBuilder builder3 =
                    new DefaultLocatorRecordBuilder();

        LispIpv4Address ipv4Locator2 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));

        record2 = builder3
                        .withPriority((byte) 0x02)
                        .withWeight((byte) 0x02)
                        .withMulticastPriority((byte) 0x02)
                        .withMulticastWeight((byte) 0x02)
                        .withLocalLocator(false)
                        .withRlocProbed(true)
                        .withRouted(false)
                        .withLocatorAfi(ipv4Locator2)
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
        DefaultLispLocatorRecord record = (DefaultLispLocatorRecord) record1;

        LispIpv4Address ipv4Locator = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        assertThat(record.getPriority(), is((byte) 0x01));
        assertThat(record.getWeight(), is((byte) 0x01));
        assertThat(record.getMulticastPriority(), is((byte) 0x01));
        assertThat(record.getMulticastWeight(), is((byte) 0x01));
        assertThat(record.isLocalLocator(), is(true));
        assertThat(record.isRlocProbed(), is(false));
        assertThat(record.isRouted(), is(true));
        assertThat(record.getLocatorAfi(), is(ipv4Locator));
    }

    @Test
    public void testSerialization() throws LispReaderException, LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        LocatorRecordWriter writer = new LocatorRecordWriter();
        writer.writeTo(byteBuf, record1);

        LocatorRecordReader reader = new LocatorRecordReader();
        LispLocatorRecord deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(record1, deserialized).testEquals();
    }
}
