/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for mapping id class.
 */
public class MappingIdTest {

    final MappingId mappingId1 = MappingId.valueOf(1);
    final MappingId sameAsMappingId1 = MappingId.valueOf(1);
    final MappingId mappingId2 = MappingId.valueOf(2);

    /**
     * Checks that the MappingId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(MappingId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(mappingId1, sameAsMappingId1)
                .addEqualityGroup(mappingId2)
                .testEquals();
    }

    /**
     * Checks the construction of a MappingId object.
     */
    @Test
    public void testConstruction() {
        final long mappingIdValue = 8888L;
        final MappingId mappingId = MappingId.valueOf(mappingIdValue);
        assertThat(mappingId, is(notNullValue()));
        assertThat(mappingId.value(), is(mappingIdValue));
    }
}
