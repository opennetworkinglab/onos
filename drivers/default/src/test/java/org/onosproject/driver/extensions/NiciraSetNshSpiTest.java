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
import org.onosproject.net.NshServicePathId;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for NiciraSetNshSpi class.
 */
public class NiciraSetNshSpiTest {

    final NiciraSetNshSpi nshSpi1 = new NiciraSetNshSpi(NshServicePathId.of(10));
    final NiciraSetNshSpi sameAsNshSpi1 = new NiciraSetNshSpi(NshServicePathId.of(10));
    final NiciraSetNshSpi nshSpi2 = new NiciraSetNshSpi(NshServicePathId.of(20));

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(nshSpi1, sameAsNshSpi1).addEqualityGroup(nshSpi2).testEquals();
    }

    /**
     * Checks the construction of a NiciraSetNshSpi object.
     */
    @Test
    public void testConstruction() {
        final NiciraSetNshSpi niciraSetNshSpi = new NiciraSetNshSpi(NshServicePathId.of(10));
        assertThat(niciraSetNshSpi, is(notNullValue()));
        assertThat(niciraSetNshSpi.nshSpi().servicePathId(), is(10));
    }
}
