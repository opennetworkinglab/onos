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
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress.NatAddressBuilder;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress.NatLcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress.NatLcafAddressWriter;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispNatLcafAddress class.
 */
public class LispNatLcafAddressTest {

    private LispNatLcafAddress address1;
    private LispNatLcafAddress sameAsAddress1;
    private LispNatLcafAddress address2;

    @Before
    public void setup() {

        NatAddressBuilder builder1 = new NatAddressBuilder();

        short msUdpPortNumber1 = 80;
        short etrUdpPortNumber1 = 100;
        LispIpv4Address globalEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address msRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));
        LispIpv4Address privateEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.3"));

        LispIpv4Address rtrRloc11 = new LispIpv4Address(IpAddress.valueOf("10.1.1.1"));
        LispIpv4Address rtrRloc12 = new LispIpv4Address(IpAddress.valueOf("10.1.1.2"));
        List<LispAfiAddress> rtrRlocs1 = ImmutableList.of(rtrRloc11, rtrRloc12);

        address1 = builder1
                        .withLength((short) 0)
                        .withMsUdpPortNumber(msUdpPortNumber1)
                        .withEtrUdpPortNumber(etrUdpPortNumber1)
                        .withGlobalEtrRlocAddress(globalEtrRlocAddress1)
                        .withMsRlocAddress(msRlocAddress1)
                        .withPrivateEtrRlocAddress(privateEtrRlocAddress1)
                        .withRtrRlocAddresses(rtrRlocs1)
                        .build();

        NatAddressBuilder builder2 = new NatAddressBuilder();

        sameAsAddress1 = builder2
                        .withLength((short) 0)
                        .withMsUdpPortNumber(msUdpPortNumber1)
                        .withEtrUdpPortNumber(etrUdpPortNumber1)
                        .withGlobalEtrRlocAddress(globalEtrRlocAddress1)
                        .withMsRlocAddress(msRlocAddress1)
                        .withPrivateEtrRlocAddress(privateEtrRlocAddress1)
                        .withRtrRlocAddresses(rtrRlocs1)
                        .build();

        NatAddressBuilder builder3 = new NatAddressBuilder();

        short msUdpPortNumber2 = 81;
        short etrUdpPortNumber2 = 101;
        LispIpv4Address globalEtrRlocAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));
        LispIpv4Address msRlocAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.2"));
        LispIpv4Address privateEtrRlocAddress2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.3"));

        LispIpv4Address rtrRloc21 = new LispIpv4Address(IpAddress.valueOf("10.1.2.1"));
        LispIpv4Address rtrRloc22 = new LispIpv4Address(IpAddress.valueOf("10.1.2.2"));
        List<LispAfiAddress> rtrRlocs2 = ImmutableList.of(rtrRloc21, rtrRloc22);

        address2 = builder3
                        .withLength((short) 0)
                        .withMsUdpPortNumber(msUdpPortNumber2)
                        .withEtrUdpPortNumber(etrUdpPortNumber2)
                        .withGlobalEtrRlocAddress(globalEtrRlocAddress2)
                        .withMsRlocAddress(msRlocAddress2)
                        .withPrivateEtrRlocAddress(privateEtrRlocAddress2)
                        .withRtrRlocAddresses(rtrRlocs2)
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
        LispNatLcafAddress natLcafAddress = address1;

        LispIpv4Address globalEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispIpv4Address msRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.2"));
        LispIpv4Address privateEtrRlocAddress1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.3"));

        assertThat(natLcafAddress.getLength(), is((short) 0));
        assertThat(natLcafAddress.getMsUdpPortNumber(), is((short) 80));
        assertThat(natLcafAddress.getEtrUdpPortNumber(), is((short) 100));
        assertThat(natLcafAddress.getGlobalEtrRlocAddress(), is(globalEtrRlocAddress1));
        assertThat(natLcafAddress.getMsRlocAddress(), is(msRlocAddress1));
        assertThat(natLcafAddress.getPrivateEtrRlocAddress(), is(privateEtrRlocAddress1));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        NatLcafAddressWriter writer = new NatLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        NatLcafAddressReader reader = new NatLcafAddressReader();
        LispNatLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
