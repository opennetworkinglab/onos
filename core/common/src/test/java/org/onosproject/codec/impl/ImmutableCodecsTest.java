/*
 * Copyright 2015-present Open Networking Foundation
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
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Tests to assure that the codec classes follow the contract of having
 * no local context.
 */
public class ImmutableCodecsTest {

    /**
     * Checks that the codec classes adhere to the contract that there cannot
     * be any local context in a codec.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutableBaseClass(AnnotatedCodec.class);
        assertThatClassIsImmutable(AnnotationsCodec.class);
        assertThatClassIsImmutable(ApplicationCodec.class);
        assertThatClassIsImmutable(ConnectivityIntentCodec.class);
        assertThatClassIsImmutable(ConnectPointCodec.class);
        assertThatClassIsImmutable(ConstraintCodec.class);
        assertThatClassIsImmutable(EncodeConstraintCodecHelper.class);
        assertThatClassIsImmutable(DecodeConstraintCodecHelper.class);
        assertThatClassIsImmutable(CriterionCodec.class);
        assertThatClassIsImmutable(EncodeCriterionCodecHelper.class);
        assertThatClassIsImmutable(DecodeCriterionCodecHelper.class);
        assertThatClassIsImmutable(DeviceCodec.class);
        assertThatClassIsImmutable(EthernetCodec.class);
        assertThatClassIsImmutable(FlowEntryCodec.class);
        assertThatClassIsImmutable(HostCodec.class);
        assertThatClassIsImmutable(HostLocationCodec.class);
        assertThatClassIsImmutable(HostToHostIntentCodec.class);
        assertThatClassIsImmutable(InstructionCodec.class);
        assertThatClassIsImmutable(EncodeInstructionCodecHelper.class);
        assertThatClassIsImmutable(DecodeInstructionCodecHelper.class);
        assertThatClassIsImmutable(IntentCodec.class);
        assertThatClassIsImmutable(LinkCodec.class);
        assertThatClassIsImmutable(PathCodec.class);
        assertThatClassIsImmutable(PointToPointIntentCodec.class);
        assertThatClassIsImmutable(PortCodec.class);
        assertThatClassIsImmutable(TopologyClusterCodec.class);
        assertThatClassIsImmutable(TopologyCodec.class);
        assertThatClassIsImmutable(TrafficSelectorCodec.class);
        assertThatClassIsImmutable(TrafficTreatmentCodec.class);
        assertThatClassIsImmutable(FlowRuleCodec.class);
    }
}
