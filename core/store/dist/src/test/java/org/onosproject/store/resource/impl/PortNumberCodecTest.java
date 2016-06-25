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

package org.onosproject.store.resource.impl;

import org.junit.Test;
import org.onosproject.net.PortNumber;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PortNumberCodecTest {
    private final PortNumberCodec sut = new PortNumberCodec();

    @Test
    public void testEncode() {
        assertThat(sut.encode(PortNumber.portNumber(100)), is(100));
    }

    @Test
    public void testDecode() {
        assertThat(sut.decode(100), is(PortNumber.portNumber(100)));
    }
}
