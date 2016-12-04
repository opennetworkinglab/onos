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
package org.onosproject.lisp.msg.types;

import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispIpv4Address.Ipv4AddressReader;
import org.onosproject.lisp.msg.types.LispIpv4Address.Ipv4AddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispIpv4Address class.
 */
public class LispIpv4AddressTest {

    private LispIpv4Address address1;
    private LispIpv4Address sameAsAddress1;
    private LispIpv4Address address2;

    @Before
    public void setup() {

        address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        sameAsAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispIpv4Address ipv4Address = address1;
        assertThat(ipv4Address.getAddress(), is(IpAddress.valueOf("192.168.1.1")));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        Ipv4AddressWriter writer = new Ipv4AddressWriter();
        writer.writeTo(byteBuf, address1);

        Ipv4AddressReader reader = new Ipv4AddressReader();
        LispIpv4Address deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
