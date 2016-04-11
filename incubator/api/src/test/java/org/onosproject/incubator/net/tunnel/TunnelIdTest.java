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

package org.onosproject.incubator.net.tunnel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for tunnel id class.
 */
public class TunnelIdTest {

    final TunnelId tunnelId1 = TunnelId.valueOf("1");
    final TunnelId sameAstunnelId1 = TunnelId.valueOf("1");
    final TunnelId tunnelId2 = TunnelId.valueOf("2");

    /**
     * Checks that the TunnelId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TunnelId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(tunnelId1, sameAstunnelId1)
                .addEqualityGroup(tunnelId2)
                .testEquals();
    }

    /**
     * Checks the construction of a FlowId object.
     */
    @Test
    public void testConstruction() {
        final String tunnelIdValue = "7777";
        final TunnelId tunnelId = TunnelId.valueOf(tunnelIdValue);
        assertThat(tunnelId, is(notNullValue()));
        assertThat(tunnelId.id(), is(tunnelIdValue));
    }
}
