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
package org.onosproject.net.group;

import org.junit.Test;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;

/**
 * Default group description unit tests.
 */
public class DefaultGroupDescriptionTest {
    byte[] keyData = "abcdefg".getBytes();
    private final GroupKey key = new DefaultGroupKey(keyData);
    private final TrafficTreatment treatment =
            DefaultTrafficTreatment.emptyTreatment();
    private final GroupBucket bucket =
            DefaultGroupBucket.createSelectGroupBucket(treatment);
    private final GroupBuckets groupBuckets =
            new GroupBuckets(ImmutableList.of(bucket));
    private final DefaultGroupDescription d1 =
            new DefaultGroupDescription(did("2"),
                    GroupDescription.Type.FAILOVER,
                    groupBuckets);
    private final DefaultGroupDescription sameAsD1 =
            new DefaultGroupDescription(d1);
    private final DefaultGroupDescription d2 =
            new DefaultGroupDescription(did("2"),
                    GroupDescription.Type.INDIRECT,
                    groupBuckets);
    private final DefaultGroupDescription d3 =
            new DefaultGroupDescription(did("3"),
                    GroupDescription.Type.FAILOVER,
                    groupBuckets,
                    key,
                    711,
                    APP_ID);

    /**
     * Checks that the Default group description class is immutable and can be
     * inherited from.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(DefaultGroupDescription.class);
    }

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(d1, sameAsD1)
                .addEqualityGroup(d2)
                .addEqualityGroup(d3)
                .testEquals();
    }

    /**
     * Checks that construction of an object was correct.
     */
    @Test
    public void testConstruction() {
        assertThat(d3.deviceId(), is(did("3")));
        assertThat(d3.type(), is(GroupDescription.Type.FAILOVER));
        assertThat(d3.buckets(), is(groupBuckets));
        assertThat(d3.appId(), is(APP_ID));
        assertThat(d3.givenGroupId(), is(711));
        assertThat(key.key(), is(keyData));
        assertThat(d3.appCookie().key(), is(keyData));
    }
}

