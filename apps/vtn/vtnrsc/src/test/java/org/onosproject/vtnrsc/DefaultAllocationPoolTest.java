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
import org.onlab.packet.IpAddress;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultAllocationPool class.
 */
public class DefaultAllocationPoolTest {

    final IpAddress startIP1 = IpAddress.valueOf("192.168.1.1");
    final IpAddress startIP2 = IpAddress.valueOf("192.168.1.2");
    final IpAddress endIP1 = IpAddress.valueOf("192.168.1.1");
    final IpAddress endIP2 = IpAddress.valueOf("192.168.1.2");

    /**
     * Checks that the DefaultAllocationPool class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultAllocationPool.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        AllocationPool pool1 = new DefaultAllocationPool(startIP1, endIP1);
        AllocationPool pool2 = new DefaultAllocationPool(startIP1, endIP1);
        AllocationPool pool3 = new DefaultAllocationPool(startIP2, endIP2);
        new EqualsTester().addEqualityGroup(pool1, pool2)
                .addEqualityGroup(pool3).testEquals();
    }

    /**
     * Checks the construction of a DefaultAllocationPool object.
     */
    @Test
    public void testConstruction() {
        final AllocationPool apool = new DefaultAllocationPool(startIP1, endIP1);
        assertThat(startIP1, is(apool.startIp()));
        assertThat(endIP1, is(apool.endIp()));
    }
}
