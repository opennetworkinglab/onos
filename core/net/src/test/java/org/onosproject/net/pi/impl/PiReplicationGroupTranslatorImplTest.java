/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.pi.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.pi.runtime.PiCloneSessionEntry;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.runtime.PiPreReplica;

import java.util.List;
import java.util.Set;

import static org.onosproject.net.group.GroupDescription.Type.ALL;
import static org.onosproject.net.group.GroupDescription.Type.CLONE;

/**
 * Test for {@link PiReplicationGroupTranslatorImpl}.
 */
public class PiReplicationGroupTranslatorImplTest {
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:dummy:1");
    private static final ApplicationId APP_ID = TestApplicationId.create("dummy");
    private static final int ENTRY_ID = 99;
    private static final GroupId GROUP_ID = GroupId.valueOf(ENTRY_ID);
    private static final GroupKey GROUP_KEY = new DefaultGroupKey(
            String.valueOf(GROUP_ID.id()).getBytes());

    private static final List<GroupBucket> BUCKET_LIST = ImmutableList.of(
            allOutputBucket(1),
            allOutputBucket(2),
            allOutputBucket(3));
    private static final List<GroupBucket> CLONE_BUCKET_LIST = ImmutableList.of(
            cloneOutputBucket(1),
            cloneOutputBucket(2),
            cloneOutputBucket(3));
    private static final List<GroupBucket> BUCKET_LIST_2 = ImmutableList.of(
            allOutputBucket(1),
            allOutputBucket(2),
            allOutputBucket(2),
            allOutputBucket(3),
            allOutputBucket(3));

    private static final Set<PiPreReplica> REPLICAS = ImmutableSet.of(
            new PiPreReplica(PortNumber.portNumber(1), 1),
            new PiPreReplica(PortNumber.portNumber(2), 1),
            new PiPreReplica(PortNumber.portNumber(3), 1));
    private static final Set<PiPreReplica> REPLICAS_2 = ImmutableSet.of(
            new PiPreReplica(PortNumber.portNumber(1), 1),
            new PiPreReplica(PortNumber.portNumber(2), 1),
            new PiPreReplica(PortNumber.portNumber(2), 2),
            new PiPreReplica(PortNumber.portNumber(3), 1),
            new PiPreReplica(PortNumber.portNumber(3), 2));

    private static final PiMulticastGroupEntry PI_MULTICAST_GROUP =
            PiMulticastGroupEntry.builder()
                    .withGroupId(GROUP_ID.id())
                    .addReplicas(REPLICAS)
                    .build();
    private static final PiCloneSessionEntry PI_CLONE_SESSION_ENTRY =
            PiCloneSessionEntry.builder()
                    .withSessionId(ENTRY_ID)
                    .addReplicas(REPLICAS)
                    .build();
    private static final PiMulticastGroupEntry PI_MULTICAST_GROUP_2 =
            PiMulticastGroupEntry.builder()
                    .withGroupId(GROUP_ID.id())
                    .addReplicas(REPLICAS_2)
                    .build();

    private static final GroupBuckets BUCKETS = new GroupBuckets(BUCKET_LIST);
    private static final GroupBuckets CLONE_BUCKETS = new GroupBuckets(CLONE_BUCKET_LIST);
    private static final GroupBuckets BUCKETS_2 = new GroupBuckets(BUCKET_LIST_2);

    private static final GroupDescription ALL_GROUP_DESC = new DefaultGroupDescription(
            DEVICE_ID, ALL, BUCKETS, GROUP_KEY, GROUP_ID.id(), APP_ID);
    private static final Group ALL_GROUP = new DefaultGroup(GROUP_ID, ALL_GROUP_DESC);

    private static final GroupDescription CLONE_GROUP_DESC = new DefaultGroupDescription(
            DEVICE_ID, CLONE, CLONE_BUCKETS, GROUP_KEY, GROUP_ID.id(), APP_ID);
    private static final Group CLONE_GROUP = new DefaultGroup(GROUP_ID, CLONE_GROUP_DESC);

    private static final GroupDescription ALL_GROUP_DESC_2 = new DefaultGroupDescription(
            DEVICE_ID, ALL, BUCKETS_2, GROUP_KEY, GROUP_ID.id(), APP_ID);
    private static final Group ALL_GROUP_2 = new DefaultGroup(GROUP_ID, ALL_GROUP_DESC_2);


    private static GroupBucket allOutputBucket(int portNum) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(portNum))
                .build();
        return DefaultGroupBucket.createAllGroupBucket(treatment);
    }

    private static GroupBucket cloneOutputBucket(int portNum) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(portNum))
                .build();
        return DefaultGroupBucket.createCloneGroupBucket(treatment);
    }

    @Test
    public void testTranslatePreGroups() throws Exception {

        PiPreEntry multicastGroup = PiReplicationGroupTranslatorImpl
                .translate(ALL_GROUP, null, null);
        PiPreEntry multicastGroup2 = PiReplicationGroupTranslatorImpl
                .translate(ALL_GROUP_2, null, null);
        PiPreEntry cloneSessionEntry = PiReplicationGroupTranslatorImpl
                .translate(CLONE_GROUP, null, null);

        new EqualsTester()
                .addEqualityGroup(multicastGroup, PI_MULTICAST_GROUP)
                .addEqualityGroup(multicastGroup2, PI_MULTICAST_GROUP_2)
                .addEqualityGroup(cloneSessionEntry, PI_CLONE_SESSION_ENTRY)
                .testEquals();
    }
}
