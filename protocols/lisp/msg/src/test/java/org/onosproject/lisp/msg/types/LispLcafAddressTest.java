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
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispLcafAddress class.
 */
public class LispLcafAddressTest {

    private LispLcafAddress address1;
    private LispLcafAddress sameAsAddress1;
    private LispLcafAddress address2;

    @Before
    public void setup() {

        address1 = new LispLcafAddress(LispCanonicalAddressFormatEnum.NAT,
                (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01);
        sameAsAddress1 = new LispLcafAddress(LispCanonicalAddressFormatEnum.NAT,
                (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01);
        address2 = new LispLcafAddress(LispCanonicalAddressFormatEnum.NAT,
                (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02);
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispLcafAddress lcafAddress = address1;

        assertThat(lcafAddress.getType(), is(LispCanonicalAddressFormatEnum.NAT));
        assertThat(lcafAddress.getReserved1(), is((byte) 0x01));
        assertThat(lcafAddress.getReserved2(), is((byte) 0x01));
        assertThat(lcafAddress.getFlag(), is((byte) 0x01));
        assertThat(lcafAddress.getLength(), is((short) 1));
    }
}
