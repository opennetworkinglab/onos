/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for the DefaultTrafficTreatment class.
 */
public class DefaultTrafficTreatmentTest {

    // Tests for the nested Builder class

    /**
     * Tests that the Builder constructors return equivalent objects
     * when given the same data.
     */
    @Test
    public void testTreatmentBuilderConstructors() {
        final TrafficTreatment treatment1 =
                DefaultTrafficTreatment.builder()
                        .add(Instructions.modL0Lambda(new IndexedLambda(4)))
                        .build();
        final TrafficTreatment treatment2 =
                DefaultTrafficTreatment.builder(treatment1).build();
        assertThat(treatment1, is(equalTo(treatment2)));
    }

    /**
     * Tests methods defined on the Builder.
     */
    @Test
    public void testBuilderMethods() {
        final Instruction instruction1 =
                Instructions.modL0Lambda(new IndexedLambda(4));

        final TrafficTreatment.Builder builder1 =
                DefaultTrafficTreatment.builder()
                .add(instruction1)
                .setEthDst(MacAddress.BROADCAST)
                .setEthSrc(MacAddress.BROADCAST)
                .setIpDst(IpAddress.valueOf("1.1.1.1"))
                .setIpSrc(IpAddress.valueOf("2.2.2.2"))
                .add(Instructions.modL0Lambda(new IndexedLambda(4)))
                .setOutput(PortNumber.portNumber(2))
                .setVlanId(VlanId.vlanId((short) 4))
                .setVlanPcp((byte) 3);

        final TrafficTreatment treatment1 = builder1.build();

        final List<Instruction> instructions1 = treatment1.immediate();
        assertThat(instructions1, hasSize(9));

        builder1.drop();
        builder1.add(instruction1);

        final List<Instruction> instructions2 = builder1.build().immediate();
        assertThat(instructions2, hasSize(11));

        builder1.deferred()
                .popVlan()
                .pushVlan()
                .setVlanId(VlanId.vlanId((short) 5));

        final List<Instruction> instructions3 = builder1.build().immediate();
        assertThat(instructions3, hasSize(11));
        final List<Instruction> instructions4 = builder1.build().deferred();
        assertThat(instructions4, hasSize(3));
    }

    /**
     * Tests equals(), hashCode() and toString() methods of
     * DefaultTrafficTreatment.
     */
    @Test
    public void testEquals() {
        final IndexedLambda lambda1 = new IndexedLambda(4);
        final IndexedLambda lambda2 = new IndexedLambda(5);
        final TrafficTreatment treatment1 =
                DefaultTrafficTreatment.builder()
                        .add(Instructions.modL0Lambda(lambda1))
                                .build();
        final TrafficTreatment sameAsTreatment1 =
                DefaultTrafficTreatment.builder()
                        .add(Instructions.modL0Lambda(lambda1))
                        .build();
        final TrafficTreatment treatment2 =
                DefaultTrafficTreatment.builder()
                        .add(Instructions.modL0Lambda(lambda2))
                        .build();
        new EqualsTester()
                .addEqualityGroup(treatment1, sameAsTreatment1)
                .addEqualityGroup(treatment2)
                .testEquals();
    }
}
