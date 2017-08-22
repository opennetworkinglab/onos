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

import java.util.EnumSet;

import org.junit.Test;
import org.onlab.util.Bandwidth;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


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
        assertTrue(queueDescription1.burst().isPresent());
        assertThat(queueDescription1.burst().get(), is(1L));
        assertTrue(queueDescription1.dscp().isPresent());
        assertThat(queueDescription1.dscp().get(), is(11));
        assertTrue(queueDescription1.maxRate().isPresent());
        assertThat(queueDescription1.maxRate().get(), is(MAX_BANDWIDTH_1));
        assertTrue(queueDescription1.minRate().isPresent());
        assertThat(queueDescription1.minRate().get(), is(MIN_BANDWIDTH_1));
        assertThat(queueDescription1.type(), contains(QueueDescription.Type.MAX));
        assertTrue(queueDescription1.priority().isPresent());
        assertThat(queueDescription1.priority().get(), is(1L));
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
