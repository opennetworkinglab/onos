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
package org.onosproject.mapping.instructions;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;

/**
 * Unit tests for the MappingInstructions class.
 */
public class MappingInstructionsTest {

    /**
     * Checks that a MappingInstruction object has the proper type, ad then
     * converts it to the proper type.
     *
     * @param instruction MappingInstruction object to convert
     * @param type        Enumerated type value for the Criterion class
     * @param clazz       Desired Criterion class
     * @param <T>         The type the caller wants returned
     * @return converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T checkAndConvert(MappingInstruction instruction,
                                  MappingInstruction.Type type, Class clazz) {
        assertThat(instruction, is(notNullValue()));
        assertThat(instruction.type(), is(equalTo(type)));
        assertThat(instruction, instanceOf(clazz));
        return (T) instruction;
    }

    /**
     * Checks the equals() and toString() methods of a MappingInstruction class.
     *
     * @param c1      first object to compare
     * @param c1match object that should be equal to the first
     * @param c2      object that should not be equal to the first
     * @param <T>     type of the arguments
     */
    private <T extends MappingInstruction> void checkEqualsAndToString(T c1,
                                                                       T c1match,
                                                                       T c2) {
        new EqualsTester()
                .addEqualityGroup(c1, c1match)
                .addEqualityGroup(c2)
                .testEquals();
    }

    /**
     * Checks that MappingInstructions is a proper utility class.
     */
    @Test
    public void testMappingInstructionsUtilityClass() {
        assertThatClassIsUtility(MappingInstructions.class);
    }

    /**
     * Checks that the MappingInstruction class implementation are immutable.
     */
    @Test
    public void testImmutabilityOfMappingInstructions() {
        assertThatClassIsImmutable(MulticastMappingInstruction.WeightMappingInstruction.class);
        assertThatClassIsImmutable(MulticastMappingInstruction.PriorityMappingInstruction.class);
        assertThatClassIsImmutable(UnicastMappingInstruction.WeightMappingInstruction.class);
        assertThatClassIsImmutable(UnicastMappingInstruction.PriorityMappingInstruction.class);
    }

    private final UnicastMappingInstruction uniWeight1 =
                                    MappingInstructions.unicastWeight(1);
    private final UnicastMappingInstruction sameAsUniWeight1 =
                                    MappingInstructions.unicastWeight(1);
    private final UnicastMappingInstruction uniWeight2 =
                                    MappingInstructions.unicastWeight(2);

    /**
     * Tests the unicastWeight method.
     */
    @Test
    public void testUnicastWeightMethod() {
        final MappingInstruction instruction = MappingInstructions.unicastWeight(2);
        final UnicastMappingInstruction.WeightMappingInstruction weightInstruction =
                        checkAndConvert(instruction,
                        UnicastMappingInstruction.Type.UNICAST,
                        UnicastMappingInstruction.WeightMappingInstruction.class);
        assertThat(weightInstruction.weight(), is(equalTo(2)));
    }

    /**
     * Test the equals() method of the UnicastWeightInstruction class.
     */
    @Test
    public void testUnicastWeightInstructionEquals() {
        checkEqualsAndToString(uniWeight1, sameAsUniWeight1, uniWeight2);
    }

    /**
     * Tests the hashCode() method of the UnicastWeightInstruction class.
     */
    @Test
    public void testUnicastWeightInstructionHashCode() {
        assertThat(uniWeight1.hashCode(), is(equalTo(sameAsUniWeight1.hashCode())));
        assertThat(uniWeight1.hashCode(), is(not(equalTo(uniWeight2.hashCode()))));
    }

    private final UnicastMappingInstruction uniPriority1 =
                                    MappingInstructions.unicastPriority(1);
    private final UnicastMappingInstruction sameAsUniPriority1 =
                                    MappingInstructions.unicastPriority(1);
    private final UnicastMappingInstruction uniPriority2 =
                                    MappingInstructions.unicastPriority(2);

    /**
     * Tests the unicastPriority method.
     */
    @Test
    public void testUnicastPriorityMethod() {
        final MappingInstruction instruction = MappingInstructions.unicastPriority(2);
        final UnicastMappingInstruction.PriorityMappingInstruction priorityMappingInstruction =
                checkAndConvert(instruction,
                                UnicastMappingInstruction.Type.UNICAST,
                                UnicastMappingInstruction.PriorityMappingInstruction.class);
        assertThat(priorityMappingInstruction.priority(), is(equalTo(2)));
    }

    /**
     * Tests the equals() method of the UnicastPriorityInstruction class.
     */
    @Test
    public void testUnicastPriorityInstructionEquals() {
        checkEqualsAndToString(uniPriority1, sameAsUniPriority1, uniPriority2);
    }

    /**
     * Test the hashCode() method of the UnicastPriorityInstruction class.
     */
    @Test
    public void testUnicastPriorityInstructionHashCode() {
        assertThat(uniPriority1.hashCode(), is(equalTo(sameAsUniPriority1.hashCode())));
        assertThat(uniPriority1.hashCode(), is(not(equalTo(uniPriority2.hashCode()))));
    }

    private final MulticastMappingInstruction multiWeight1 =
                                    MappingInstructions.multicastWeight(1);
    private final MulticastMappingInstruction sameAsMultiWeight1 =
                                    MappingInstructions.multicastWeight(1);
    private final MulticastMappingInstruction multiWeight2 =
                                    MappingInstructions.multicastWeight(2);

    /**
     * Tests the multicastWeight method.
     */
    @Test
    public void testMulticastWeightMethod() {
        final MappingInstruction instruction = MappingInstructions.multicastWeight(2);
        final MulticastMappingInstruction.WeightMappingInstruction weightMappingInstruction =
                checkAndConvert(instruction,
                                MulticastMappingInstruction.Type.MULTICAST,
                                MulticastMappingInstruction.WeightMappingInstruction.class);
        assertThat(weightMappingInstruction.weight(), is(equalTo(2)));
    }

    /**
     * Tests the equals() method of the MulticastWeightInstruction class.
     */
    @Test
    public void testMulticastWeightInstructionEquals() {
        checkEqualsAndToString(multiWeight1, sameAsMultiWeight1, multiWeight2);
    }

    /**
     * Tests the hashCode() method of the MulticastWeightInstruction class.
     */
    @Test
    public void testMulticastWeightInstructionHashCode() {
        assertThat(multiWeight1.hashCode(), is(equalTo(sameAsMultiWeight1.hashCode())));
        assertThat(multiWeight1.hashCode(), is(not(equalTo(multiWeight2.hashCode()))));
    }

    private final MulticastMappingInstruction multiPriority1 =
                                    MappingInstructions.multicastPriority(1);
    private final MulticastMappingInstruction sameAsMultiPriority1 =
                                    MappingInstructions.multicastPriority(1);
    private final MulticastMappingInstruction multiPriority2 =
                                    MappingInstructions.multicastPriority(2);

    /**
     * Tests the multicastPriority method.
     */
    @Test
    public void testMulticastPriorityMethod() {
        final MappingInstruction instruction = MappingInstructions.multicastPriority(2);
        final MulticastMappingInstruction.PriorityMappingInstruction priorityMappingInstruction =
                checkAndConvert(instruction,
                                MulticastMappingInstruction.Type.MULTICAST,
                                MulticastMappingInstruction.PriorityMappingInstruction.class);
        assertThat(priorityMappingInstruction.priority(), is(equalTo(2)));
    }

    /**
     * Tests the equals() method of the MulticastPriorityInstruction class.
     */
    @Test
    public void testMulticastPriorityInstructionEquals() {
        checkEqualsAndToString(multiPriority1, sameAsMultiPriority1, multiPriority2);
    }

    /**
     * Tests the hashCode() method of the MulticastPriorityInstruction class.
     */
    @Test
    public void testMulticastPriorityInstructionHashCode() {
        assertThat(multiPriority1.hashCode(), is(equalTo(sameAsMultiPriority1.hashCode())));
        assertThat(multiPriority1.hashCode(), is(not(equalTo(multiPriority2.hashCode()))));
    }

    // ExtensionMappingInstructionWrapper

    class MockExtensionTreatment implements ExtensionTreatment {
        int type;

        MockExtensionTreatment(int type) {
            this.type = type;
        }

        @Override
        public ExtensionTreatmentType type() {
            return new ExtensionTreatmentType(type);
        }

        @Override
        public <T> void setPropertyValue(String key, T value) throws ExtensionPropertyException {

        }

        @Override
        public <T> T getPropertyValue(String key) throws ExtensionPropertyException {
            return null;
        }

        @Override
        public List<String> getProperties() {
            return null;
        }

        @Override
        public byte[] serialize() {
            return new byte[0];
        }

        @Override
        public void deserialize(byte[] data) {

        }
    }

    private final ExtensionTreatment extensionTreatment1 = new MockExtensionTreatment(111);
    private final ExtensionTreatment extensionTreatment2 = new MockExtensionTreatment(222);

    private final DeviceId deviceId1 = DeviceId.deviceId("of:1");
    private final DeviceId deviceId2 = DeviceId.deviceId("of:2");

    private final MappingInstruction extensionInstruction1 =
            MappingInstructions.extension(extensionTreatment1, deviceId1);
    private final MappingInstruction sameAsExtensionInstruction1 =
            MappingInstructions.extension(extensionTreatment1, deviceId1);
    private final MappingInstruction extensionInstruction2 =
            MappingInstructions.extension(extensionTreatment2, deviceId2);

    /**
     * Tests the extension method.
     */
    @Test
    public void testExtensionMethod() {
        final MappingInstruction instruction =
                MappingInstructions.extension(extensionTreatment1, deviceId1);
        final MappingInstructions.ExtensionMappingInstructionWrapper wrapper =
                checkAndConvert(instruction,
                                MappingInstruction.Type.EXTENSION,
                                MappingInstructions.ExtensionMappingInstructionWrapper.class);
        assertThat(wrapper.deviceId(), is(deviceId1));
        assertThat(wrapper.extensionMappingInstruction(), is(extensionTreatment1));
    }

    /**
     * Tests the equals() method of the ExtensionMappingInstructionWrapper class.
     */
    @Test
    public void testExtensionMappingInstructionWrapperEquals() {
        checkEqualsAndToString(extensionInstruction1,
                                sameAsExtensionInstruction1,
                                extensionInstruction2);
    }

    /**
     * Tests the hashCode() method of the ExtensionMappingInstructionWrapper class.
     */
    @Test
    public void testExtensionMappingInstructionWrapperHashCode() {
        assertThat(extensionInstruction1.hashCode(),
                            is(equalTo(sameAsExtensionInstruction1.hashCode())));
        assertThat(extensionInstruction1.hashCode(),
                            is(not(equalTo(extensionInstruction2.hashCode()))));
    }
}
