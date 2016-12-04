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
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import org.onosproject.lisp.msg.types.LispDistinguishedNameAddress.DistinguishedNameAddressReader;
import org.onosproject.lisp.msg.types.LispDistinguishedNameAddress.DistinguishedNameAddressWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispDistinguishedNameAddress class.
 */
public class LispDistinguishedNameAddressTest {

    private LispDistinguishedNameAddress address1;
    private LispDistinguishedNameAddress sameAsAddress1;
    private LispDistinguishedNameAddress address2;

    @Before
    public void setup() {

        address1 = new LispDistinguishedNameAddress("distAddress1");
        sameAsAddress1 = new LispDistinguishedNameAddress("distAddress1");
        address2 = new LispDistinguishedNameAddress("distAddress2");
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispDistinguishedNameAddress distinguishedNameAddress = address1;

        assertThat(distinguishedNameAddress.getDistinguishedName(), is("distAddress1"));
    }

    @Test
    public void testSerialization() throws LispWriterException, LispParseError {
        ByteBuf byteBuf = Unpooled.buffer();

        DistinguishedNameAddressWriter writer = new DistinguishedNameAddressWriter();
        writer.writeTo(byteBuf, address1);

        DistinguishedNameAddressReader reader = new DistinguishedNameAddressReader();
        LispDistinguishedNameAddress deserialized = reader.readFrom(byteBuf);

        new EqualsTester().addEqualityGroup(address1, deserialized).testEquals();
    }
}
