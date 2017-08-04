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

import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.DefaultLocatorBuilder;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.LocatorReader;
import org.onosproject.lisp.msg.protocols.DefaultLispLocator.LocatorWriter;
import org.onosproject.lisp.msg.protocols.LispLocator.LocatorBuilder;
import org.onosproject.lisp.msg.types.LispIpv4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultLispLocator class.
 */
public final class DefaultLispLocatorTest {

    private static final String IP_ADDRESS_1 = "192.168.1.1";
    private static final String IP_ADDRESS_2 = "192.168.1.2";

    private LispLocator record1;
    private LispLocator sameAsRecord1;
    private LispLocator record2;

    @Before
    public void setup() {

        LispLocator.LocatorBuilder builder1 = new DefaultLocatorBuilder();

        LispIpv4Address ipv4Locator1 = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

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

        LocatorBuilder builder2 = new DefaultLocatorBuilder();

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

        LispLocator.LocatorBuilder builder3 = new DefaultLocatorBuilder();

        LispIpv4Address ipv4Locator2 = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_2));

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
        DefaultLispLocator record = (DefaultLispLocator) record1;

        LispIpv4Address ipv4Locator = new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

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

        LocatorWriter writer = new LocatorWriter();
        writer.writeTo(byteBuf, record1);

        LocatorReader reader = new LocatorReader();
        LispLocator deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(record1, deserialized).testEquals();
    }
}
