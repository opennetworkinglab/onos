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
import org.onosproject.lisp.msg.types.lcaf.LispAsLcafAddress.AsAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispAsLcafAddress.AsLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispAsLcafAddress.AsLcafAddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispAsLcafAddress class.
 */
public class LispAsLcafAddressTest {

    private LispAsLcafAddress address1;
    private LispAsLcafAddress sameAsAddress1;
    private LispAsLcafAddress address2;

    @Before
    public void setup() {

        AsAddressBuilder builder1 = new AsAddressBuilder();

        LispIpv4Address ipv4Address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        address1 = builder1
                        .withAsNumber(1)
                        .withAddress(ipv4Address1)
                        .build();

        AsAddressBuilder builder2 = new AsAddressBuilder();

        sameAsAddress1 = builder2
                            .withAsNumber(1)
                            .withAddress(ipv4Address1)
                            .build();

        AsAddressBuilder builder3 = new AsAddressBuilder();

        LispIpv4Address ipv4Address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));

        address2 = builder3
                        .withAsNumber(2)
                        .withAddress(ipv4Address2)
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
        LispAsLcafAddress asLcafAddress = address1;

        LispIpv4Address ipv4Address = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        assertThat(asLcafAddress.getAsNumber(), is(1));
        assertThat(asLcafAddress.getAddress(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        AsLcafAddressWriter writer = new AsLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        AsLcafAddressReader reader = new AsLcafAddressReader();
        LispAsLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
