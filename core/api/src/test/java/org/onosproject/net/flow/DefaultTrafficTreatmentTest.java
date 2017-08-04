/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.flow;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Unit tests for the DefaultTrafficTreatment class.
 */
public class DefaultTrafficTreatmentTest {

    // Tests for the nested Builder class

    /**
     * Tests methods defined on the Builder.
     */
    @Test
    public void testBuilderMethods() {
        final TrafficTreatment.Builder builder1 =
                DefaultTrafficTreatment.builder()
                .setEthDst(MacAddress.BROADCAST)
                .setEthSrc(MacAddress.BROADCAST)
                .setIpDst(IpAddress.valueOf("1.1.1.1"))
                .setIpSrc(IpAddress.valueOf("2.2.2.2"))
                .setOutput(PortNumber.portNumber(2))
                .setVlanId(VlanId.vlanId((short) 4))
                .setVlanPcp((byte) 3);

        final TrafficTreatment treatment1 = builder1.build();

        final List<Instruction> instructions1 = treatment1.immediate();
        assertThat(instructions1, hasSize(7));

        builder1.drop();

        final List<Instruction> instructions2 = builder1.build().immediate();
        assertThat(instructions2, hasSize(8));

        builder1.deferred()
                .popVlan()
                .pushVlan()
                .setVlanId(VlanId.vlanId((short) 5));

        final List<Instruction> instructions3 = builder1.build().immediate();
        assertThat(instructions3, hasSize(8));
        final List<Instruction> instructions4 = builder1.build().deferred();
        assertThat(instructions4, hasSize(3));
    }
}
