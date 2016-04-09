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
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for FixedIp class.
 */
public class FixedIpTest {

    final SubnetId subnetId1 = SubnetId.subnetId("lef11-95w-4er-9c9c");
    final SubnetId subnetId2 = SubnetId.subnetId("lefaa-95w-4er-9c9c");
    final IpAddress ip1 = IpAddress.valueOf("192.168.0.1");
    final IpAddress ip2 = IpAddress.valueOf("192.168.1.1");

    /**
     * Checks that the FixedIp class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(FixedIp.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        FixedIp fixedIp1 = FixedIp.fixedIp(subnetId1, ip1);
        FixedIp fixedIp2 = FixedIp.fixedIp(subnetId1, ip1);
        FixedIp fixedIp3 = FixedIp.fixedIp(subnetId2, ip2);
        new EqualsTester().addEqualityGroup(fixedIp1, fixedIp2)
                .addEqualityGroup(fixedIp3).testEquals();
    }

    /**
     * Checks the construction of a FixedIp object.
     */
    @Test
    public void testConstruction() {
        FixedIp fixedIp = FixedIp.fixedIp(subnetId1, ip1);
        assertThat(ip1, is(notNullValue()));
        assertThat(ip1, is(fixedIp.ip()));
        assertThat(subnetId1, is(notNullValue()));
        assertThat(subnetId1, is(fixedIp.subnetId()));
    }
}
