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
package org.onosproject.net.device;

import org.junit.Test;
import org.onosproject.net.PortNumber;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onosproject.net.Port.Type.COPPER;

/**
 * Unit tests for the DefaultPortDescription test.
 */
public class DefaultPortDescriptionTest {

    private static PortNumber port1 = PortNumber.portNumber(1);
    private static long portSpeed1 = 111L;
    private static DefaultPortDescription portDescription1 =
            DefaultPortDescription.builder().withPortNumber(port1).isEnabled(true)
                    .type(COPPER).portSpeed(portSpeed1).build();

    private static DefaultPortDescription sameAsPortDescription1 =
            DefaultPortDescription.builder(portDescription1)
                            .annotations(portDescription1.annotations())
                            .build();

    private static PortNumber port2 = PortNumber.portNumber(2);
    private static DefaultPortDescription portDescription2 =
            DefaultPortDescription.builder().withPortNumber(port2).isEnabled(true).build();

    private static DefaultPortDescription portDescription3 =
            new DefaultPortDescription();
    /**
     * Tests the immutability of {@link DefaultPortDescription}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutableBaseClass(DefaultPortDescription.class);
    }

    /**
     * Tests object construction and fetching of member data.
     */
    @Test
    public void testConstruction() {
        assertThat(portDescription1.portNumber(), is(port1));
        assertThat(portDescription1.isEnabled(), is(true));
        assertThat(portDescription1.portSpeed(), is(portSpeed1));
        assertThat(portDescription1.type(), is(COPPER));
    }

    /**
     * Tests equals(), hashCode(), and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(portDescription1, sameAsPortDescription1)
                .addEqualityGroup(portDescription2)
                .addEqualityGroup(portDescription3)
                .testEquals();
    }
}
