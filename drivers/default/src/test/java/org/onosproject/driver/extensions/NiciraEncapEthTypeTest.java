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
package org.onosproject.driver.extensions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for NiciraEncapEthType class.
 */
public class NiciraEncapEthTypeTest {
    final short ethType1 = (short) 0x894f;
    final short ethType2 = (short) 0x800;

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        final NiciraEncapEthType encapEthType1 = new NiciraEncapEthType(ethType1);
        final NiciraEncapEthType sameAsEncapEthType1 = new NiciraEncapEthType(ethType1);
        final NiciraEncapEthType encapEthType2 = new NiciraEncapEthType(ethType2);

        new EqualsTester().addEqualityGroup(encapEthType1, sameAsEncapEthType1).addEqualityGroup(encapEthType2)
                .testEquals();
    }

    /**
     * Checks the construction of a NiciraEncapEthType object.
     */
    @Test
    public void testConstruction() {
        final NiciraEncapEthType encapEthType = new NiciraEncapEthType(ethType1);
        assertThat(encapEthType, is(notNullValue()));
        assertThat(encapEthType.encapEthType(), is(ethType1));
    }
}
