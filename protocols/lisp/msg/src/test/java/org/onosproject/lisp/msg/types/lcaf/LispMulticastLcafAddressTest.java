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
import org.onosproject.lisp.msg.types.lcaf.LispMulticastLcafAddress.MulticastAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispMulticastLcafAddress.MulticastLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispMulticastLcafAddress.MulticastLcafAddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispMulticastLcafAddress class.
 */
public class LispMulticastLcafAddressTest {

    private static final String IP_ADDRESS_1 = "192.168.1.1";
    private static final String IP_ADDRESS_2 = "192.168.1.2";

    private LispMulticastLcafAddress address1;
    private LispMulticastLcafAddress sameAsAddress1;
    private LispMulticastLcafAddress address2;

    @Before
    public void setup() {

        MulticastAddressBuilder builder1 = new MulticastAddressBuilder();

        LispIpv4Address ipv4Address1 =
                new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        address1 = builder1
                .withInstanceId(1)
                .withSrcMaskLength((byte) 0x24)
                .withGrpMaskLength((byte) 0x24)
                .withSrcAddress(ipv4Address1)
                .withGrpAddress(ipv4Address1)
                .build();

        MulticastAddressBuilder builder2 = new MulticastAddressBuilder();

        sameAsAddress1 = builder2
                .withInstanceId(1)
                .withSrcMaskLength((byte) 0x24)
                .withGrpMaskLength((byte) 0x24)
                .withSrcAddress(ipv4Address1)
                .withGrpAddress(ipv4Address1)
                .build();

        MulticastAddressBuilder builder3 = new MulticastAddressBuilder();

        LispIpv4Address ipv4Address2 =
                new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_2));

        address2 = builder3
                .withInstanceId(2)
                .withSrcMaskLength((byte) 0x24)
                .withGrpMaskLength((byte) 0x24)
                .withSrcAddress(ipv4Address2)
                .withGrpAddress(ipv4Address2)
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
        LispMulticastLcafAddress multicastLcafAddress = address1;

        LispIpv4Address ipv4Address =
                new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        assertThat(multicastLcafAddress.getInstanceId(), is(1));
        assertThat(multicastLcafAddress.getSrcMaskLength(), is((byte) 0x24));
        assertThat(multicastLcafAddress.getGrpMaskLength(), is((byte) 0x24));
        assertThat(multicastLcafAddress.getSrcAddress(), is(ipv4Address));
        assertThat(multicastLcafAddress.getGrpAddress(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError,
            LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        MulticastLcafAddressWriter writer = new MulticastLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        MulticastLcafAddressReader reader = new MulticastLcafAddressReader();
        LispMulticastLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
