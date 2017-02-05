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
import org.onosproject.lisp.msg.types.lcaf.LispNonceLcafAddress.NonceAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispNonceLcafAddress.NonceLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispNonceLcafAddress.NonceLcafAddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispNonceLcafAddress class.
 */
public class LispNonceLcafAddressTest {

    private static final String IP_ADDRESS_1 = "192.168.1.1";
    private static final String IP_ADDRESS_2 = "192.168.1.2";

    private static final int NONCE_1 = 1048576;
    private static final int NONCE_2 = 1;

    private LispNonceLcafAddress address1;
    private LispNonceLcafAddress sameAsAddress1;
    private LispNonceLcafAddress address2;

    @Before
    public void setup() {

        NonceAddressBuilder builder1 = new NonceAddressBuilder();

        LispIpv4Address ipv4Address1 =
                new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        address1 = builder1
                .withNonce(NONCE_1)
                .withAddress(ipv4Address1)
                .build();

        NonceAddressBuilder builder2 = new NonceAddressBuilder();

        sameAsAddress1 = builder2
                .withNonce(NONCE_1)
                .withAddress(ipv4Address1)
                .build();

        NonceAddressBuilder builder3 = new NonceAddressBuilder();

        LispIpv4Address ipv4Address2 =
                new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_2));

        address2 = builder3
                .withNonce(NONCE_2)
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
        LispNonceLcafAddress nonceLcafAddress = address1;

        LispIpv4Address ipv4Address =
                new LispIpv4Address(IpAddress.valueOf(IP_ADDRESS_1));

        assertThat(nonceLcafAddress.getNonce(), is(NONCE_1));
        assertThat(nonceLcafAddress.getAddress(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError,
                                            LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        NonceLcafAddressWriter writer = new NonceLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        NonceLcafAddressReader reader = new NonceLcafAddressReader();
        LispNonceLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
