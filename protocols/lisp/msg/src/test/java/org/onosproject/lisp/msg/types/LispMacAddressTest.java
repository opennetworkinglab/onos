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
import org.onlab.packet.MacAddress;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.types.LispMacAddress.MacAddressReader;
import static org.onosproject.lisp.msg.types.LispMacAddress.MacAddressWriter;

/**
 * Unit tests for LispMacAddress class.
 */
public class LispMacAddressTest {

    private LispMacAddress address1;
    private LispMacAddress sameAsAddress1;
    private LispMacAddress address2;

    @Before
    public void setup() {

        address1 = new LispMacAddress(MacAddress.valueOf("00:00:00:00:00:01"));
        sameAsAddress1 = new LispMacAddress(MacAddress.valueOf("00:00:00:00:00:01"));
        address2 = new LispMacAddress(MacAddress.valueOf("00:00:00:00:00:02"));
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispMacAddress macAddress = address1;
        assertThat(macAddress.getAddress(), is(MacAddress.valueOf("00:00:00:00:00:01")));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        MacAddressWriter writer = new MacAddressWriter();
        writer.writeTo(byteBuf, address1);

        MacAddressReader reader = new MacAddressReader();
        LispMacAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester()
                .addEqualityGroup(address1, deserialized).testEquals();
    }
}
