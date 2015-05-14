/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.codec.impl;

import org.junit.Test;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.codec.impl.GroupJsonMatcher.matchesGroup;

/**
 * Group codec unit tests.
 */

public class GroupCodecTest {

    @Test
    public void codecTest() {
        GroupBucket bucket1 = DefaultGroupBucket
                .createSelectGroupBucket(DefaultTrafficTreatment.emptyTreatment());
        GroupBucket bucket2 = DefaultGroupBucket
                .createIndirectGroupBucket(DefaultTrafficTreatment.emptyTreatment());
        GroupBuckets buckets = new GroupBuckets(ImmutableList.of(bucket1, bucket2));


        DefaultGroup group = new DefaultGroup(
                new DefaultGroupId(1),
                NetTestTools.did("d1"),
                GroupDescription.Type.INDIRECT,
                buckets);

        MockCodecContext context = new MockCodecContext();
        GroupCodec codec = new GroupCodec();
        ObjectNode groupJson = codec.encode(group, context);

        assertThat(groupJson, matchesGroup(group));
    }
}
