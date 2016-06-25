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
 * Unit tests for RouterId class.
 */
public class RouterIdTest {
    final RouterId routerId1 = RouterId.valueOf("1");
    final RouterId sameAsRouterId1 = RouterId.valueOf("1");
    final RouterId routerId2 = RouterId.valueOf("2");

    /**
     * Checks that the RouterId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(RouterId.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(routerId1, sameAsRouterId1).addEqualityGroup(routerId2)
                .testEquals();
    }

    /**
     * Checks the construction of a RouterId object.
     */
    @Test
    public void testConstruction() {
        final String routerIdValue = "s";
        final RouterId routerId = RouterId.valueOf(routerIdValue);
        assertThat(routerId, is(notNullValue()));
        assertThat(routerId.routerId(), is(routerIdValue));
    }
}
