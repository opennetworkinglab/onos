/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.util.Bandwidth;

import java.util.EnumSet;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class DefaultQueueDescriptionTest {

    private static final Bandwidth MAX_BANDWIDTH_1 = Bandwidth.bps(2L);
    private static final Bandwidth MIN_BANDWIDTH_1 = Bandwidth.bps(1L);
    private static final QueueId QUEUE_ID1 = QueueId.queueId("QUEUE 1");
    private static final Bandwidth MAX_BANDWIDTH_2 = Bandwidth.bps(12L);
    private static final Bandwidth MIN_BANDWIDTH_2 = Bandwidth.bps(11L);
    private static final QueueId QUEUE_ID2 = QueueId.queueId("QUEUE 2");

    private QueueDescription queueDescription1 =
            DefaultQueueDescription.builder()
                    .burst(1L)
                    .dscp(11)
                    .maxRate(MAX_BANDWIDTH_1)
                    .minRate(MIN_BANDWIDTH_1)
                    .priority(1L)
                    .type(EnumSet.of(QueueDescription.Type.MAX))
                    .queueId(QUEUE_ID1)
                    .build();
    private QueueDescription sameAsQueueDescription1 =
            DefaultQueueDescription.builder()
                    .burst(1L)
                    .dscp(11)
                    .maxRate(MAX_BANDWIDTH_1)
                    .minRate(MIN_BANDWIDTH_1)
                    .priority(1L)
                    .type(EnumSet.of(QueueDescription.Type.MAX))
                    .queueId(QUEUE_ID1)
                    .build();
    private QueueDescription queueDescription2 =
            DefaultQueueDescription.builder()
                    .burst(2L)
                    .dscp(12)
                    .maxRate(MAX_BANDWIDTH_2)
                    .minRate(MIN_BANDWIDTH_2)
                    .priority(1L)
                    .type(EnumSet.of(QueueDescription.Type.MAX))
                    .queueId(QUEUE_ID2)
                    .build();

    @Test
    public void testConstruction() {
        assertThat(queueDescription1.burst(), optionalWithValue(is(1L)));
        assertThat(queueDescription1.dscp(), optionalWithValue(is(11)));
        assertThat(queueDescription1.maxRate(), optionalWithValue(is(MAX_BANDWIDTH_1)));
        assertThat(queueDescription1.minRate(), optionalWithValue(is(MIN_BANDWIDTH_1)));
        assertThat(queueDescription1.priority(), optionalWithValue(is(1L)));
        assertThat(queueDescription1.queueId(), is(QUEUE_ID1));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(queueDescription1, sameAsQueueDescription1)
                .addEqualityGroup(queueDescription2)
                .testEquals();
    }
}
