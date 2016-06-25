/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.floatingip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onosproject.vtnrsc.FloatingIpId;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for FloatingIpId class.
 */
public class FloatingIpIdTest {
    private String floatingIpIdStr1 = "5fb63824-4d5c-4b85-9f2f-ebb93c9ce3df";
    private String floatingIpIdStr2 = "fa44f585-fe02-40d3-afe7-d1d7e5782c99";

    /**
     * Checks that the FloatingIpId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(FloatingIpId.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        FloatingIpId id1 = FloatingIpId.of(floatingIpIdStr1);
        FloatingIpId id2 = FloatingIpId.of(floatingIpIdStr1);
        FloatingIpId id3 = FloatingIpId.of(floatingIpIdStr2);
        new EqualsTester().addEqualityGroup(id1, id2).addEqualityGroup(id3)
                .testEquals();
    }

    /**
     * Checks the construction of a FloatingIpId object.
     */
    @Test
    public void testConstruction() {
        final FloatingIpId id = FloatingIpId.of(floatingIpIdStr1);
        assertThat(id, is(notNullValue()));
        assertThat(id.floatingIpId().toString(), is(floatingIpIdStr1));
    }
}
