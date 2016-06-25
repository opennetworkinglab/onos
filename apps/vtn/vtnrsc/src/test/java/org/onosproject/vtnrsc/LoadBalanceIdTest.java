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
package org.onosproject.vtnrsc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for LoadBalanceId class.
 */
public class LoadBalanceIdTest {

    final LoadBalanceId id1 = LoadBalanceId.of((byte) 1);
    final LoadBalanceId sameAsId1 = LoadBalanceId.of((byte) 1);
    final LoadBalanceId id2 = LoadBalanceId.of((byte) 2);

    /**
     * Checks that the LoadBalanceId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(LoadBalanceId.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(id1, sameAsId1).addEqualityGroup(id2)
        .testEquals();
    }

    /**
     * Checks the construction of a LoadBalanceId object.
     */
    @Test
    public void testConstruction() {
        final LoadBalanceId id = LoadBalanceId.of((byte) 1);
        assertThat(id, is(notNullValue()));
        assertThat(id.loadBalanceId(), is((byte) 1));
    }
}
