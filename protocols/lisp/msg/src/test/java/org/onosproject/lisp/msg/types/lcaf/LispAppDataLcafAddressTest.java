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
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.lcaf.LispAppDataLcafAddress.AppDataAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispAppDataLcafAddress.AppDataLcafAddressWriter;
import org.onosproject.lisp.msg.types.lcaf.LispAppDataLcafAddress.AppDataLcafAddressReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispAppDataLcafAddress class.
 */
public class LispAppDataLcafAddressTest {

    private LispAppDataLcafAddress address1;
    private LispAppDataLcafAddress sameAsAddress1;
    private LispAppDataLcafAddress address2;

    @Before
    public void setup() {

        AppDataAddressBuilder builder1 = new AppDataAddressBuilder();

        LispAfiAddress ipv4Address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        address1 = builder1
                    .withProtocol((byte) 0x01)
                    .withIpTos((short) 10)
                    .withLocalPortLow((short) 1)
                    .withLocalPortHigh((short) 255)
                    .withRemotePortLow((short) 2)
                    .withRemotePortHigh((short) 254)
                    .withAddress(ipv4Address1)
                    .build();

        AppDataAddressBuilder builder2 = new AppDataAddressBuilder();

        sameAsAddress1 = builder2
                            .withProtocol((byte) 0x01)
                            .withIpTos((short) 10)
                            .withLocalPortLow((short) 1)
                            .withLocalPortHigh((short) 255)
                            .withRemotePortLow((short) 2)
                            .withRemotePortHigh((short) 254)
                            .withAddress(ipv4Address1)
                            .build();

        AppDataAddressBuilder builder3 = new AppDataAddressBuilder();

        LispAfiAddress ipv4Address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));

        address2 = builder3
                        .withProtocol((byte) 0x02)
                        .withIpTos((short) 20)
                        .withLocalPortLow((short) 1)
                        .withLocalPortHigh((short) 255)
                        .withRemotePortLow((short) 2)
                        .withRemotePortHigh((short) 254)
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
        LispAppDataLcafAddress appDataLcafAddress = address1;

        LispAfiAddress ipv4Address = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));

        assertThat(appDataLcafAddress.getProtocol(), is((byte) 0x01));
        assertThat(appDataLcafAddress.getIpTos(), is(10));
        assertThat(appDataLcafAddress.getLocalPortLow(), is((short) 1));
        assertThat(appDataLcafAddress.getLocalPortHigh(), is((short) 255));
        assertThat(appDataLcafAddress.getRemotePortLow(), is((short) 2));
        assertThat(appDataLcafAddress.getRemotePortHigh(), is((short) 254));
        assertThat(appDataLcafAddress.getAddress(), is(ipv4Address));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        AppDataLcafAddressWriter writer = new AppDataLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        AppDataLcafAddressReader reader = new AppDataLcafAddressReader();
        LispAppDataLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
