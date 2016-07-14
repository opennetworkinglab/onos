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
 * Unit tests for NiciraTunGpeNp class.
 */
public class NiciraTunGpeNpTest {
    final byte np1 = (byte) 1;
    final byte np2 = (byte) 2;

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        final NiciraTunGpeNp tunGpeNp1 = new NiciraTunGpeNp(np1);
        final NiciraTunGpeNp sameAsTunGpeNp1 = new NiciraTunGpeNp(np1);
        final NiciraTunGpeNp tunGpeNp2 = new NiciraTunGpeNp(np2);

        new EqualsTester().addEqualityGroup(tunGpeNp1, sameAsTunGpeNp1).addEqualityGroup(tunGpeNp2)
        .testEquals();
    }

    /**
     * Checks the construction of a NiciraTunGpeNp object.
     */
    @Test
    public void testConstruction() {
        final NiciraTunGpeNp tunGpeNp1 = new NiciraTunGpeNp(np1);
        assertThat(tunGpeNp1, is(notNullValue()));
        assertThat(tunGpeNp1.tunGpeNp(), is(np1));
    }
}
