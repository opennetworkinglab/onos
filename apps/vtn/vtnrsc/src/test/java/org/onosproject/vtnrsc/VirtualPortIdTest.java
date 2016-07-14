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

package org.onosproject.vtnrsc;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for VirtualPortId class.
 */
public class VirtualPortIdTest {

    final VirtualPortId virtualPortId1 = VirtualPortId.portId("1");
    final VirtualPortId sameAsVirtualPortId1 = VirtualPortId.portId("1");
    final VirtualPortId virtualPortId2 = VirtualPortId.portId("2");

    /**
     * Checks that the VirtualPortId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(VirtualPortId.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(virtualPortId1, sameAsVirtualPortId1)
                .addEqualityGroup(virtualPortId2).testEquals();
    }

    /**
     * Checks the construction of a VirtualPortId object.
     */
    @Test
    public void testConstruction() {
        final String vPortIdValue = "aaa";
        final VirtualPortId virtualPortId = VirtualPortId.portId(vPortIdValue);
        assertThat(virtualPortId, is(notNullValue()));
        assertThat(virtualPortId.portId(), is(vPortIdValue));

    }
}
