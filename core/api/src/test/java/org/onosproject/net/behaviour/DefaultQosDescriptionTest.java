/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.behaviour;

import java.util.Map;

import org.junit.Test;
import org.onlab.util.Bandwidth;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

public class DefaultQosDescriptionTest {

    private QosId qosId1 = QosId.qosId("1");
    private Bandwidth bandwidth1 = Bandwidth.bps(1);
    private Map<Long, QueueDescription> queues1 = ImmutableMap.of();

    private QosDescription defaultQosDescription1 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(1L)
                    .cir(11L)
                    .maxRate(bandwidth1)
                    .queues(queues1)
                    .type(QosDescription.Type.NOOP)
                    .build();
    private QosDescription sameAsDefaultQosDescription1 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(1L)
                    .cir(11L)
                    .maxRate(bandwidth1)
                    .queues(queues1)
                    .type(QosDescription.Type.NOOP)
                    .build();
    private QosDescription defaultQosDescription2 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(2L)
                    .cir(11L)
                    .maxRate(bandwidth1)
                    .queues(queues1)
                    .type(QosDescription.Type.NOOP)
                    .build();
    private QosDescription defaultQosDescription3 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(1L)
                    .cir(33L)
                    .maxRate(bandwidth1)
                    .queues(queues1)
                    .type(QosDescription.Type.NOOP)
                    .build();
    private QosDescription defaultQosDescription4 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(1L)
                    .cir(11L)
                    .queues(queues1)
                    .type(QosDescription.Type.NOOP)
                    .build();
    private QosDescription defaultQosDescription5 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(1L)
                    .cir(11L)
                    .maxRate(bandwidth1)
                    .type(QosDescription.Type.NOOP)
                    .build();
    private QosDescription defaultQosDescription6 =
            DefaultQosDescription.builder()
                    .qosId(qosId1)
                    .cbs(1L)
                    .cir(11L)
                    .maxRate(bandwidth1)
                    .queues(queues1)
                    .type(QosDescription.Type.CODEL)
                    .build();

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultQosDescription.class);
    }

    @Test
    public void testConstruction() {
        assertThat(defaultQosDescription1.qosId(), is(qosId1));
        assertThat(defaultQosDescription1.cbs(), optionalWithValue(is(1L)));
        assertThat(defaultQosDescription1.cir(), optionalWithValue(is(11L)));
        assertThat(defaultQosDescription1.maxRate(), optionalWithValue(is(bandwidth1)));
        assertThat(defaultQosDescription1.queues(), optionalWithValue(is(queues1)));
        assertThat(defaultQosDescription1.type(), is(QosDescription.Type.NOOP));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(defaultQosDescription1, sameAsDefaultQosDescription1)
                .addEqualityGroup(defaultQosDescription2)
                .addEqualityGroup(defaultQosDescription3)
                .addEqualityGroup(defaultQosDescription4)
                .addEqualityGroup(defaultQosDescription5)
                .addEqualityGroup(defaultQosDescription6)
                .testEquals();
    }
}
