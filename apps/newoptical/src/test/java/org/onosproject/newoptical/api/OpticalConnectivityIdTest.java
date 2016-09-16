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

package org.onosproject.newoptical.api;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test class for OpticalConnectivityId.
 */
public class OpticalConnectivityIdTest {

    /**
     * Checks that OpticalConnectivityId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(OpticalConnectivityId.class);
    }

    /**
     * Checks the construction of OpticalConnectivityId object.
     */
    @Test
    public void testConstruction() {
        OpticalConnectivityId tid = OpticalConnectivityId.of(1L);
        assertNotNull(tid);
        assertEquals(tid.id().longValue(), 1L);
    }

    /**
     * Checks the equality of OpticalConnectivityId objects.
     */
    @Test
    public void testEquality() {
        OpticalConnectivityId tid1 = OpticalConnectivityId.of(1L);
        OpticalConnectivityId tid2 = OpticalConnectivityId.of(1L);
        OpticalConnectivityId tid3 = OpticalConnectivityId.of(2L);

        new EqualsTester()
                .addEqualityGroup(tid1, tid2)
                .addEqualityGroup(tid3)
                .testEquals();
    }
}