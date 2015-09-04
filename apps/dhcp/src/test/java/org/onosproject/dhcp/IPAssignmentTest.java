/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.dhcp;

import com.google.common.testing.EqualsTester;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.onlab.packet.Ip4Address;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * Unit Tests for IPAssignment class.
 */
public class IPAssignmentTest extends TestCase {

    private final Date dateNow = new Date();

    private final IPAssignment stats1 = IPAssignment.builder()
            .ipAddress(Ip4Address.valueOf("10.10.10.10"))
            .leasePeriod(300)
            .assignmentStatus(IPAssignment.AssignmentStatus.Option_Expired)
            .timestamp(dateNow)
            .build();

    private final IPAssignment stats2 = IPAssignment.builder()
            .ipAddress(Ip4Address.valueOf("10.10.10.10"))
            .leasePeriod(300)
            .assignmentStatus(IPAssignment.AssignmentStatus.Option_Assigned)
            .timestamp(dateNow)
            .build();

    private final IPAssignment stats3 = IPAssignment.builder(stats1)
            .build();

    /**
     *  Tests the constructor for the class.
     */
    @Test
    public void testConstruction() {
        assertThat(stats3.ipAddress(), is(Ip4Address.valueOf("10.10.10.10")));
        assertThat(stats3.timestamp(), is(dateNow));
        assertThat(stats3.leasePeriod(), is(300));
        assertThat(stats3.assignmentStatus(), is(IPAssignment.AssignmentStatus.Option_Expired));
    }

    /**
     * Tests the equality and inequality of objects using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(stats1, stats1)
                .addEqualityGroup(stats2)
                .testEquals();
    }

    /**
     * Tests if the toString method returns a consistent value for hashing.
     */
    @Test
    public void testToString() {
        assertThat(stats1.toString(), is(stats1.toString()));
    }

    /**
     * Tests if the validateInputs method returns an exception for malformed object.
     */
    @Test
    public void testValidateInputs() {
        try {
            IPAssignment stats4 = IPAssignment.builder()
                    .ipAddress(Ip4Address.valueOf("10.10.10.10"))
                    .leasePeriod(300)
                    .build();

            fail("Construction of a malformed IPAssignment did not throw an exception");
        } catch (NullPointerException e) {
            Assert.assertThat(e.getMessage(), containsString("must be specified"));
        }
    }
}