/*
 * Copyright 2014 Open Networking Laboratory
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

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.PortNumber.portNumber;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test of the port number.
 */
public class PortNumberTest {

    @Test
    public void basics() {
        new EqualsTester()
        .addEqualityGroup(portNumber(123),
                portNumber("123"))
                .addEqualityGroup(portNumber(321))
                .testEquals();
    }

    @Test
    public void number() {
        assertEquals("incorrect long value", 12345, portNumber(12345).toLong());
    }


}
