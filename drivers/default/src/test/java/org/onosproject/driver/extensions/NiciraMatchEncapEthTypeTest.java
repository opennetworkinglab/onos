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
package org.onosproject.driver.extensions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for NiciraMatchEncapEthType class.
 */
public class NiciraMatchEncapEthTypeTest {
    final short ethType1 = (short) 0x894f;
    final short ethType2 = (short) 0x800;

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        final NiciraMatchEncapEthType encapEthType1 = new NiciraMatchEncapEthType(ethType1);
        final NiciraMatchEncapEthType sameAsEncapEthType1 = new NiciraMatchEncapEthType(ethType1);
        final NiciraMatchEncapEthType encapEthType2 = new NiciraMatchEncapEthType(ethType2);

        new EqualsTester().addEqualityGroup(encapEthType1, sameAsEncapEthType1).addEqualityGroup(encapEthType2)
        .testEquals();
    }

    /**
     * Checks the construction of a NiciraMatchEncapEthType object.
     */
    @Test
    public void testConstruction() {
        final NiciraMatchEncapEthType encapEthType = new NiciraMatchEncapEthType(ethType1);
        assertThat(encapEthType, is(notNullValue()));
        assertThat(encapEthType.encapEthType(), is(ethType1));
    }
}
