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
 * Unit tests for NiciraNshMdType class.
 */
public class NiciraNshMdTypeTest {
    final byte mdType1 = (byte) 1;
    final byte mdType2 = (byte) 2;

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        final NiciraNshMdType nshMdType1 = new NiciraNshMdType(mdType1);
        final NiciraNshMdType sameAsnshMdType1 = new NiciraNshMdType(mdType1);
        final NiciraNshMdType nshMdType2 = new NiciraNshMdType(mdType2);

        new EqualsTester().addEqualityGroup(nshMdType1, sameAsnshMdType1).addEqualityGroup(nshMdType2)
        .testEquals();
    }

    /**
     * Checks the construction of a NiciraNshMdType object.
     */
    @Test
    public void testConstruction() {
        final NiciraNshMdType nshMdType = new NiciraNshMdType(mdType1);
        assertThat(nshMdType, is(notNullValue()));
        assertThat(nshMdType.nshMdType(), is(mdType1));
    }
}
