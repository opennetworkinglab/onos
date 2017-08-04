/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for the LinkKey class.
 */
public class LinkKeyTest {

    static final DeviceId D1 = deviceId("1");
    static final DeviceId D2 = deviceId("2");
    static final PortNumber P1 = portNumber(1);
    static final PortNumber P2 = portNumber(2);

    static final ConnectPoint SRC1 = new ConnectPoint(D1, P1);
    static final ConnectPoint DST1 = new ConnectPoint(D2, P1);
    static final ConnectPoint DST2 = new ConnectPoint(D2, P2);


    /**
     * Checks that the LinkKey class is immutable.
     */
    @Test
    public void testLinkKeyImmutability() {
        assertThatClassIsImmutable(LinkKey.class);
    }

    /**
     * Check null source connection.
     */
    @Test(expected = NullPointerException.class)
    public void testNullSrc() {
        LinkKey key = LinkKey.linkKey(null, DST1);
    }

    /**
     * Check null destination connection.
     */
    @Test(expected = NullPointerException.class)
    public void testNullDst() {
        LinkKey key = LinkKey.linkKey(SRC1, null);
    }

    /**
     * Check that two LinkKeys based on the same source/destination pair compare
     * equal.
     */
    @Test
    public void testCompareEquals() {
        LinkKey k1 = LinkKey.linkKey(SRC1, DST2);
        LinkKey k2 = LinkKey.linkKey(SRC1, DST2);

        assertThat(k1, is(equalTo(k2)));
    }

    /**
     * Check that two LinkKeys based on different source/destination pairs compare
     * not equal.
     */
    @Test
    public void testCompareNotEquals() {
        LinkKey k1 = LinkKey.linkKey(SRC1, DST1);
        LinkKey k2 = LinkKey.linkKey(SRC1, DST2);

        assertThat(k1, is(not(equalTo(k2))));
        assertThat(k1, is(not(equalTo(new Object()))));
    }

    /**
     * Check that two LinkKeys based on the same source/destination pair compare
     * equal.
     */
    @Test
    public void testHashCodeEquals() {
        LinkKey k1 = LinkKey.linkKey(SRC1, DST2);
        LinkKey k2 = LinkKey.linkKey(SRC1, DST2);

        assertThat(k1.hashCode(), is(equalTo(k2.hashCode())));
    }

    /**
     * Check that two LinkKeys based on different source/destination pairs compare
     * not equal.
     */
    @Test
    public void testHashCodeNotEquals() {
        LinkKey k1 = LinkKey.linkKey(SRC1, DST1);
        LinkKey k2 = LinkKey.linkKey(SRC1, DST2);

        assertThat(k1.hashCode(), is(not(equalTo(k2.hashCode()))));
    }

    /**
     * Check the toString() method of LinkKey.
     */
    @Test
    public void testToString() {
        LinkKey k1 = LinkKey.linkKey(SRC1, DST1);
        String k1String = k1.toString();
        assertThat(k1String, allOf(containsString("LinkKey{"),
                                   containsString("src=1/1"),
                                   containsString("dst=2/1}")));
    }

    @Test
    public void asId() {
        LinkKey k1 = LinkKey.linkKey(SRC1, DST2);
        assertThat(k1.asId(), is(equalTo("1/1-2/2")));
    }
}
