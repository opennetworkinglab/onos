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
 * Unit tests for SecurityGroup class.
 */
public class SecurityGroupTest {

    final SecurityGroup securityGroup1 = SecurityGroup.securityGroup("1");
    final SecurityGroup sameAssecurityGroup = SecurityGroup.securityGroup("1");
    final SecurityGroup securityGroup2 = SecurityGroup.securityGroup("2");

    /**
     * Checks that the SecurityGroup class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(SecurityGroup.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        new EqualsTester().addEqualityGroup(securityGroup1, sameAssecurityGroup)
                .addEqualityGroup(securityGroup2).testEquals();
    }

    /**
     * Checks the construction of a SecurityGroup object.
     */
    @Test
    public void testConstruction() {
        final String securityGroupValue = "1";
        final SecurityGroup securityGroup = SecurityGroup.securityGroup(securityGroupValue);
        assertThat(securityGroup, is(notNullValue()));
        assertThat(securityGroup.securityGroup(), is(securityGroupValue));

    }
}
