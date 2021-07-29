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
import org.junit.Assert;
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
import org.onosproject.net.pi.service.PiTranslationException;

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
    private static final int TRUNCATE_MAC_LEN = 1400;

    private static final List<GroupBucket> BUCKET_LIST = ImmutableList.of(
            allOutputBucket(1),
            allOutputBucket(2),
            allOutputBucket(3));
    private static final List<GroupBucket> BUCKET_LIST_2 = ImmutableList.of(
            allOutputBucket(1),
            allOutputBucket(2),
            allOutputBucket(2),
            allOutputBucket(3),
            allOutputBucket(3));
    private static final List<GroupBucket> CLONE_BUCKET_LIST = ImmutableList.of(
            cloneOutputBucket(1),
            cloneOutputBucket(2),
            cloneOutputBucket(3));
    private static final List<GroupBucket> CLONE_BUCKET_LIST_2 = ImmutableList.of(
            cloneOutputBucket(1, TRUNCATE_MAC_LEN),
            cloneOutputBucket(2, TRUNCATE_MAC_LEN),
            cloneOutputBucket(3, TRUNCATE_MAC_LEN));
    private static final List<GroupBucket> INVALID_BUCKET_LIST = ImmutableList.of(
            DefaultGroupBucket.createAllGroupBucket(
                    DefaultTrafficTreatment.emptyTreatment()
            )
    );
    // This is invalid since we can only use truncate instruction in a clone group.
    private static final List<GroupBucket> INVALID_BUCKET_LIST_2 = ImmutableList.of(
        DefaultGroupBucket.createAllGroupBucket(
                DefaultTrafficTreatment.builder()
                        .setOutput(PortNumber.portNumber(1))
                        .truncate(TRUNCATE_MAC_LEN)
                        .build()
        )
    );
    // Unsupported instruction.
    private static final List<GroupBucket> INVALID_BUCKET_LIST_3 = ImmutableList.of(
            DefaultGroupBucket.createAllGroupBucket(
                    DefaultTrafficTreatment.builder()
                            .setOutput(PortNumber.portNumber(1))
                            .popVlan()
                            .build()
            )
    );
    private static final List<GroupBucket> INVALID_CLONE_BUCKET_LIST = ImmutableList.of(
            DefaultGroupBucket.createCloneGroupBucket(
                    DefaultTrafficTreatment.emptyTreatment()
            )
    );
    // This is invalid since all truncate instruction must be the same.
    private static final List<GroupBucket> INVALID_CLONE_BUCKET_LIST_2 = ImmutableList.of(
            cloneOutputBucket(1, TRUNCATE_MAC_LEN),
            cloneOutputBucket(2, TRUNCATE_MAC_LEN + 1)
    );
    // This is invalid since only one truncate instruction is allowed per bucket.
    private static final List<GroupBucket> INVALID_CLONE_BUCKET_LIST_3 = ImmutableList.of(
            DefaultGroupBucket.createCloneGroupBucket(
                    DefaultTrafficTreatment.builder()
                            .setOutput(PortNumber.portNumber(1))
                            .truncate(TRUNCATE_MAC_LEN)
                            .truncate(TRUNCATE_MAC_LEN)
                            .build()
            )
    );
    // Inconsistent truncate instructions.
    private static final List<GroupBucket> INVALID_CLONE_BUCKET_LIST_4 = ImmutableList.of(
            cloneOutputBucket(1, TRUNCATE_MAC_LEN),
            cloneOutputBucket(2, TRUNCATE_MAC_LEN),
            cloneOutputBucket(3)
    );

    private static final Set<PiPreReplica> REPLICAS = ImmutableSet.of(
            new PiPreReplica(PortNumber.portNumber(1), 0),
            new PiPreReplica(PortNumber.portNumber(2), 0),
            new PiPreReplica(PortNumber.portNumber(3), 0));
    private static final Set<PiPreReplica> REPLICAS_2 = ImmutableSet.of(
            new PiPreReplica(PortNumber.portNumber(1), 0),
            new PiPreReplica(PortNumber.portNumber(2), 0),
            new PiPreReplica(PortNumber.portNumber(2), 1),
            new PiPreReplica(PortNumber.portNumber(3), 0),
            new PiPreReplica(PortNumber.portNumber(3), 1));

    private static final PiMulticastGroupEntry PI_MULTICAST_GROUP =
            PiMulticastGroupEntry.builder()
                    .withGroupId(GROUP_ID.id())
                    .addReplicas(REPLICAS)
                    .build();
    private static final PiMulticastGroupEntry PI_MULTICAST_GROUP_2 =
            PiMulticastGroupEntry.builder()
                    .withGroupId(GROUP_ID.id())
                    .addReplicas(REPLICAS_2)
                    .build();
    private static final PiCloneSessionEntry PI_CLONE_SESSION_ENTRY =
            PiCloneSessionEntry.builder()
                    .withSessionId(ENTRY_ID)
                    .addReplicas(REPLICAS)
                    .build();
    private static final PiCloneSessionEntry PI_CLONE_SESSION_ENTRY_2 =
            PiCloneSessionEntry.builder()
                    .withSessionId(ENTRY_ID)
                    .addReplicas(REPLICAS)
                    .withMaxPacketLengthBytes(TRUNCATE_MAC_LEN)
                    .build();

    private static final Group ALL_GROUP = createGroup(BUCKET_LIST, ALL);
    private static final Group ALL_GROUP_2 = createGroup(BUCKET_LIST_2, ALL);
    private static final Group CLONE_GROUP = createGroup(CLONE_BUCKET_LIST, CLONE);
    private static final Group CLONE_GROUP_2 = createGroup(CLONE_BUCKET_LIST_2, CLONE);
    private static final Group INVALID_ALL_GROUP = createGroup(INVALID_BUCKET_LIST, ALL);
    private static final Group INVALID_ALL_GROUP_2 = createGroup(INVALID_BUCKET_LIST_2, ALL);
    private static final Group INVALID_ALL_GROUP_3 = createGroup(INVALID_BUCKET_LIST_3, ALL);
    private static final Group INVALID_CLONE_GROUP = createGroup(INVALID_CLONE_BUCKET_LIST, CLONE);
    private static final Group INVALID_CLONE_GROUP_2 = createGroup(INVALID_CLONE_BUCKET_LIST_2, CLONE);
    private static final Group INVALID_CLONE_GROUP_3 = createGroup(INVALID_CLONE_BUCKET_LIST_3, CLONE);
    private static final Group INVALID_CLONE_GROUP_4 = createGroup(INVALID_CLONE_BUCKET_LIST_4, CLONE);


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

    private static GroupBucket cloneOutputBucket(int portNum, int truncateMaxLen) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(portNum))
                .truncate(truncateMaxLen)
                .build();
        return DefaultGroupBucket.createCloneGroupBucket(treatment);
    }

    private static Group createGroup(List<GroupBucket> bucketList, GroupDescription.Type type) {
        final GroupBuckets buckets = new GroupBuckets(bucketList);
        final GroupDescription groupDesc = new DefaultGroupDescription(
                DEVICE_ID, type, buckets, GROUP_KEY, GROUP_ID.id(), APP_ID);
        return new DefaultGroup(GROUP_ID, groupDesc);
    }

    @Test
    public void testTranslatePreGroups() throws Exception {
        PiPreEntry multicastGroup = PiReplicationGroupTranslatorImpl
                .translate(ALL_GROUP, null, null);
        PiPreEntry multicastGroup2 = PiReplicationGroupTranslatorImpl
                .translate(ALL_GROUP_2, null, null);
        PiPreEntry cloneSessionEntry = PiReplicationGroupTranslatorImpl
                .translate(CLONE_GROUP, null, null);
        PiPreEntry cloneSessionEntry2 = PiReplicationGroupTranslatorImpl
                .translate(CLONE_GROUP_2, null, null);

        new EqualsTester()
                .addEqualityGroup(multicastGroup, PI_MULTICAST_GROUP)
                .addEqualityGroup(multicastGroup2, PI_MULTICAST_GROUP_2)
                .addEqualityGroup(cloneSessionEntry, PI_CLONE_SESSION_ENTRY)
                .addEqualityGroup(cloneSessionEntry2, PI_CLONE_SESSION_ENTRY_2)
                .testEquals();
    }

    @Test
    public void testInvalidPreGroups() {
        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_ALL_GROUP, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("support only groups with just one OUTPUT instruction per bucket", ex.getMessage());
        }

        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_ALL_GROUP_2, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("only CLONE group support truncate instruction", ex.getMessage());
        }

        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_ALL_GROUP_3, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("bucket contains unsupported instruction(s)", ex.getMessage());
        }

        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_CLONE_GROUP, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("support only groups with just one OUTPUT instruction per bucket", ex.getMessage());
        }

        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_CLONE_GROUP_2, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("all TRUNCATE instruction must be the same in a CLONE group", ex.getMessage());
        }

        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_CLONE_GROUP_3, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("support only groups with just one TRUNCATE instruction per bucket", ex.getMessage());
        }

        try {
            PiReplicationGroupTranslatorImpl
                    .translate(INVALID_CLONE_GROUP_4, null, null);
            Assert.fail("Did not get expected exception.");
        } catch (PiTranslationException ex) {
            Assert.assertEquals("all TRUNCATE instruction must be the same in a CLONE group", ex.getMessage());
        }
    }
}
