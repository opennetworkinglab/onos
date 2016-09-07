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

import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.types.LispListLcafAddress.ListLcafAddressReader;
import static org.onosproject.lisp.msg.types.LispListLcafAddress.ListLcafAddressWriter;

/**
 * Unit tests for LispListLcafAddress class.
 */
public class LispListLcafAddressTest {

    private LispListLcafAddress address1;
    private LispListLcafAddress sameAsAddress1;
    private LispListLcafAddress address2;

    @Before
    public void setup() {

        LispAfiAddress ipv4Address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispAfiAddress ipv6Address1 = new LispIpv6Address(IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8885"));

        List<LispAfiAddress> afiAddresses1 = Lists.newArrayList();
        afiAddresses1.add(ipv4Address1);
        afiAddresses1.add(ipv6Address1);

        address1 = new LispListLcafAddress(afiAddresses1);

        sameAsAddress1 = new LispListLcafAddress(afiAddresses1);

        LispAfiAddress ipv4Address2 = new LispIpv4Address(IpAddress.valueOf("192.168.2.1"));
        LispAfiAddress ipv6Address2 = new LispIpv6Address(IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8886"));

        List<LispAfiAddress> afiAddresses2 = Lists.newArrayList();
        afiAddresses2.add(ipv4Address2);
        afiAddresses2.add(ipv6Address2);

        address2 = new LispListLcafAddress(afiAddresses2);
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispListLcafAddress listLcafAddress = address1;

        LispAfiAddress ipv4Address1 = new LispIpv4Address(IpAddress.valueOf("192.168.1.1"));
        LispAfiAddress ipv6Address1 = new LispIpv6Address(IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8885"));

        List<LispAfiAddress> afiAddresses1 = Lists.newArrayList();
        afiAddresses1.add(ipv4Address1);
        afiAddresses1.add(ipv6Address1);

        assertThat(listLcafAddress.getAddresses(), is(afiAddresses1));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError, LispReaderException {
        ByteBuf byteBuf = Unpooled.buffer();

        ListLcafAddressWriter writer = new ListLcafAddressWriter();
        writer.writeTo(byteBuf, address1);

        ListLcafAddressReader reader = new ListLcafAddressReader();
        LispListLcafAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(address1, deserialized).testEquals();
    }
}
