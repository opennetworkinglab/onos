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
import org.onosproject.net.NshServiceIndex;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for NiciraSetNshSi class.
 */
public class NiciraSetNshSiTest {

    final NiciraSetNshSi nshSi1 = new NiciraSetNshSi(NshServiceIndex.of((short) 10));
    final NiciraSetNshSi sameAsNshSi1 = new NiciraSetNshSi(NshServiceIndex.of((short) 10));
    final NiciraSetNshSi nshSi2 = new NiciraSetNshSi(NshServiceIndex.of((short) 20));

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(nshSi1, sameAsNshSi1).addEqualityGroup(nshSi2).testEquals();
    }

    /**
     * Checks the construction of a NiciraSetNshSi object.
     */
    @Test
    public void testConstruction() {
        final NiciraSetNshSi niciraSetNshSi = new NiciraSetNshSi(NshServiceIndex.of((short) 15));
        assertThat(niciraSetNshSi, is(notNullValue()));
        assertThat(niciraSetNshSi.nshSi().serviceIndex(), is((short) 15));
    }
}
