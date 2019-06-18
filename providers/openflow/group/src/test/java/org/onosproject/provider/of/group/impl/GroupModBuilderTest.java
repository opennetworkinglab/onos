/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.provider.of.group.impl;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupModCommand;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.ver13.OFFactoryVer13;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class GroupModBuilderTest {

    // Build the data needed for the groups
    private static final TrafficTreatment PORT1_OUTPUT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(1))
            .build();
    private static final GroupBucket PORT1_BUCKET = DefaultGroupBucket.createSelectGroupBucket(PORT1_OUTPUT);
    private static final TrafficTreatment PORT2_OUTPUT = DefaultTrafficTreatment.builder()
            .setOutput(PortNumber.portNumber(2))
            .build();
    private static final GroupBucket PORT2_BUCKET = DefaultGroupBucket.createSelectGroupBucket(PORT2_OUTPUT);
    private static final GroupBuckets GROUP_BUCKETS = new GroupBuckets(Lists.newArrayList(PORT1_BUCKET,
                                                                                          PORT2_BUCKET));
    private static final GroupId GROUP_ID = GroupId.valueOf(1);

    @Test
    public void groupAdd() {
        OFGroupMod groupAdd = GroupModBuilder.builder(GROUP_BUCKETS,
                                                      GROUP_ID,
                                                      GroupDescription.Type.SELECT,
                                                      OFFactoryVer13.INSTANCE,
                                                      Optional.of(Long.MAX_VALUE)).buildGroupAdd();
        assertThat(groupAdd.getBuckets().size(), is(2));
        assertThat(groupAdd.getGroup().getGroupNumber(), is(GROUP_ID.id()));
        assertThat(getGroupType(groupAdd.getGroupType()), is(GroupDescription.Type.SELECT));
        assertThat(groupAdd.getXid(), is(Long.MAX_VALUE));
        assertThat(groupAdd.getCommand(), is(OFGroupModCommand.ADD));
    }

    @Test
    public void groupMod() {
        OFGroupMod groupMod = GroupModBuilder.builder(GROUP_BUCKETS,
                                                      GROUP_ID,
                                                      GroupDescription.Type.INDIRECT,
                                                      OFFactoryVer13.INSTANCE,
                                                      Optional.of(Long.MAX_VALUE)).buildGroupMod();
        assertThat(groupMod.getBuckets().size(), is(2));
        assertThat(groupMod.getGroup().getGroupNumber(), is(GROUP_ID.id()));
        assertThat(getGroupType(groupMod.getGroupType()), is(GroupDescription.Type.INDIRECT));
        assertThat(groupMod.getXid(), is(Long.MAX_VALUE));
        assertThat(groupMod.getCommand(), is(OFGroupModCommand.MODIFY));
    }

    @Test
    public void groupDel() {
        OFGroupMod groupMod = GroupModBuilder.builder(GROUP_BUCKETS,
                                                      GROUP_ID,
                                                      GroupDescription.Type.ALL,
                                                      OFFactoryVer13.INSTANCE,
                                                      Optional.of(Long.MAX_VALUE)).buildGroupDel();
        assertThat(groupMod.getBuckets().size(), is(0));
        assertThat(groupMod.getGroup().getGroupNumber(), is(GROUP_ID.id()));
        assertThat(getGroupType(groupMod.getGroupType()), is(GroupDescription.Type.ALL));
        assertThat(groupMod.getXid(), is(Long.MAX_VALUE));
        assertThat(groupMod.getCommand(), is(OFGroupModCommand.DELETE));
    }

    private GroupDescription.Type getGroupType(OFGroupType type) {
        switch (type) {
            case ALL:
                return GroupDescription.Type.ALL;
            case INDIRECT:
                return GroupDescription.Type.INDIRECT;
            case SELECT:
                return GroupDescription.Type.SELECT;
            case FF:
                return GroupDescription.Type.FAILOVER;
            default:
                break;
        }
        return null;
    }

}
