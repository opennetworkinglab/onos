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
 * Unit tests for NiciraNshNp class.
 */
public class NiciraNshNpTest {
    final byte np1 = (byte) 1;
    final byte np2 = (byte) 4;

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        final NiciraNshNp nshNp1 = new NiciraNshNp(np1);
        final NiciraNshNp sameAsNshNp1 = new NiciraNshNp(np1);
        final NiciraNshNp nshNp2 = new NiciraNshNp(np2);

        new EqualsTester().addEqualityGroup(nshNp1, sameAsNshNp1).addEqualityGroup(nshNp2)
                .testEquals();
    }

    /**
     * Checks the construction of a NiciraNshNp object.
     */
    @Test
    public void testConstruction() {
        final NiciraNshNp nshNp1 = new NiciraNshNp(np1);
        assertThat(nshNp1, is(notNullValue()));
        assertThat(nshNp1.nshNp(), is(np1));
    }
}
