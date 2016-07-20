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
 * Unit tests for TenantId class.
 */
public class TenantIdTest {

    final TenantId tenantId1 = TenantId.tenantId("1");
    final TenantId sameAsTenantId1 = TenantId.tenantId("1");
    final TenantId tenantId2 = TenantId.tenantId("2");

    /**
     * Checks that the TenantId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TenantId.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(tenantId1, sameAsTenantId1).addEqualityGroup(tenantId2)
                .testEquals();
    }

    /**
     * Checks the construction of a TenantId object.
     */
    @Test
    public void testConstruction() {
        final String tenantIdValue = "s";
        final TenantId tenantId = TenantId.tenantId(tenantIdValue);
        assertThat(tenantId, is(notNullValue()));
        assertThat(tenantId.tenantId(), is(tenantIdValue));
    }
}
