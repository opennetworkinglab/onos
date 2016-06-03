/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.PortNumber.Logical;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.onosproject.net.PortNumber.portNumber;

import java.util.List;

/**
 * Test of the port number.
 */
public class PortNumberTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(portNumber(123), portNumber("123"))
                .addEqualityGroup(portNumber(321), portNumber(321, "LIM-3-2-1"))
                .testEquals();
    }

    @Test
    public void number() {
        assertEquals("incorrect long value", 12345, portNumber(12345).toLong());
    }

    @Test
    public void decimalPortNumberIsReconstructableFromString() {
        List<PortNumber> ps = ImmutableList.<PortNumber>builder()
                                .add(portNumber(0))
                                .add(portNumber(1))
                                .add(portNumber(6653))
                                .add(portNumber(PortNumber.MAX_NUMBER))
                                .build();
        ps.forEach(p -> assertEquals(p, PortNumber.fromString(p.toString())));
    }

    @Test
    public void logicalPortNumberIsReconstructableFromString() {
        List<PortNumber> ps = ImmutableList.copyOf(Logical.values())
                                .stream().map(Logical::instance).collect(toList());

        ps.forEach(p -> assertEquals(p, PortNumber.fromString(p.toString())));

        PortNumber unknown = portNumber(-42);
        assertEquals(unknown, PortNumber.fromString(unknown.toString()));
    }

    @Test
    public void namedPortNumberIsReconstructableFromString() {
        List<PortNumber> ps = ImmutableList.<PortNumber>builder()
                                .add(portNumber(0, "Zero"))
                                .add(portNumber(1, "[ONE]"))
                                .add(portNumber(6653, "OpenFlow (1.3+)"))
                                .add(portNumber(PortNumber.MAX_NUMBER, "(大)"))
                                .build();
        ps.forEach(p -> assertEquals(p, PortNumber.fromString(p.toString())));
    }

    @Test
    public void exactlyEquals() {
        assertTrue(portNumber(0).exactlyEquals(portNumber(0)));
        assertTrue(portNumber(0, "foo").exactlyEquals(portNumber(0, "foo")));

        assertFalse(portNumber(0, "foo").exactlyEquals(portNumber(0, "bar")));
        assertFalse(portNumber(0, "foo").exactlyEquals(portNumber(0)));
        assertFalse(portNumber(0, "foo").exactlyEquals(portNumber(1, "foo")));

        assertFalse(portNumber(123).exactlyEquals(portNumber(123, "123")));
    }

}
