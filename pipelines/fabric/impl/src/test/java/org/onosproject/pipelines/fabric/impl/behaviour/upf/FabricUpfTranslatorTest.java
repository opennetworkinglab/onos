/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.junit.Test;
import org.onosproject.net.behaviour.upf.ForwardingActionRule;
import org.onosproject.net.behaviour.upf.PacketDetectionRule;
import org.onosproject.net.behaviour.upf.UpfInterface;
import org.onosproject.net.behaviour.upf.UpfProgrammableException;
import org.onosproject.net.flow.FlowRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FabricUpfTranslatorTest {

    private final FabricUpfTranslator upfTranslator = new FabricUpfTranslator(TestDistributedFabricUpfStore.build());

    @Test
    public void fabricEntryToUplinkPdrTest() {
        fabricToPdrUplink(TestUpfConstants.UPLINK_PDR, TestUpfConstants.FABRIC_UPLINK_PDR);
    }

    @Test
    public void fabricEntryToUplinkQosPdrTest() {
        fabricToPdrUplink(TestUpfConstants.UPLINK_QOS_PDR, TestUpfConstants.FABRIC_UPLINK_QOS_PDR);
        fabricToPdrUplink(TestUpfConstants.UPLINK_QOS_4G_PDR, TestUpfConstants.FABRIC_UPLINK_QOS_4G_PDR);
    }

    private void fabricToPdrUplink(PacketDetectionRule expected, FlowRule fabricFlow) {
        PacketDetectionRule translatedPdr;
        try {
            translatedPdr = upfTranslator.fabricEntryToPdr(fabricFlow);
        } catch (UpfProgrammableException e) {
            assertThat("Fabric uplink PDR should translate to abstract PDR without error.", false);
            return;
        }
        assertThat("Translated PDR should be uplink.", translatedPdr.matchesEncapped());
        assertThat(translatedPdr, equalTo(expected));
    }

    @Test
    public void fabricEntryToDownlinkPdrTest() {
        fabricToPdrDownlink(TestUpfConstants.DOWNLINK_PDR, TestUpfConstants.FABRIC_DOWNLINK_PDR);
    }

    @Test
    public void fabricEntryToDownlinkQosPdrTest() {
        fabricToPdrDownlink(TestUpfConstants.DOWNLINK_QOS_PDR, TestUpfConstants.FABRIC_DOWNLINK_QOS_PDR);
        fabricToPdrDownlink(TestUpfConstants.DOWNLINK_QOS_4G_PDR, TestUpfConstants.FABRIC_DOWNLINK_QOS_4G_PDR);
    }

    private void fabricToPdrDownlink(PacketDetectionRule expected, FlowRule fabricFlow) {
        PacketDetectionRule translatedPdr;
        try {
            translatedPdr = upfTranslator.fabricEntryToPdr(fabricFlow);
        } catch (UpfProgrammableException e) {
            assertThat("Fabric downlink PDR should translate to abstract PDR without error.", false);
            return;
        }

        assertThat("Translated PDR should be downlink.", translatedPdr.matchesUnencapped());
        assertThat(translatedPdr, equalTo(expected));
    }

    @Test
    public void fabricEntryToUplinkFarTest() {
        ForwardingActionRule translatedFar;
        ForwardingActionRule expectedFar = TestUpfConstants.UPLINK_FAR;
        try {
            translatedFar = upfTranslator.fabricEntryToFar(TestUpfConstants.FABRIC_UPLINK_FAR);
        } catch (UpfProgrammableException e) {
            assertThat("Fabric uplink FAR should correctly translate to abstract FAR without error",
                       false);
            return;
        }
        assertThat("Translated FAR should be uplink.", translatedFar.forwards());
        assertThat(translatedFar, equalTo(expectedFar));
    }

    @Test
    public void fabricEntryToDownlinkFarTest() {
        ForwardingActionRule translatedFar;
        ForwardingActionRule expectedFar = TestUpfConstants.DOWNLINK_FAR;
        try {
            translatedFar = upfTranslator.fabricEntryToFar(TestUpfConstants.FABRIC_DOWNLINK_FAR);
        } catch (UpfProgrammableException e) {
            assertThat("Fabric downlink FAR should correctly translate to abstract FAR without error",
                       false);
            return;
        }
        assertThat("Translated FAR should be downlink.", translatedFar.encaps());
        assertThat(translatedFar, equalTo(expectedFar));
    }

    @Test
    public void fabricEntryToUplinkInterfaceTest() {
        UpfInterface translatedInterface;
        UpfInterface expectedInterface = TestUpfConstants.UPLINK_INTERFACE;
        try {
            translatedInterface = upfTranslator.fabricEntryToInterface(TestUpfConstants.FABRIC_UPLINK_INTERFACE);
        } catch (UpfProgrammableException e) {
            assertThat("Fabric uplink interface should correctly translate to abstract interface without error",
                       false);
            return;
        }
        assertThat("Translated interface should be uplink.", translatedInterface.isAccess());
        assertThat(translatedInterface, equalTo(expectedInterface));
    }

    @Test
    public void fabricEntryToDownlinkInterfaceTest() {
        UpfInterface translatedInterface;
        UpfInterface expectedInterface = TestUpfConstants.DOWNLINK_INTERFACE;
        try {
            translatedInterface = upfTranslator.fabricEntryToInterface(TestUpfConstants.FABRIC_DOWNLINK_INTERFACE);
        } catch (UpfProgrammableException e) {
            assertThat("Fabric downlink interface should correctly translate to abstract interface without error",
                       false);
            return;
        }
        assertThat("Translated interface should be downlink.", translatedInterface.isCore());
        assertThat(translatedInterface, equalTo(expectedInterface));
    }

    @Test
    public void uplinkInterfaceToFabricEntryTest() {
        FlowRule translatedRule;
        FlowRule expectedRule = TestUpfConstants.FABRIC_UPLINK_INTERFACE;
        try {
            translatedRule = upfTranslator.interfaceToFabricEntry(TestUpfConstants.UPLINK_INTERFACE,
                                                                  TestUpfConstants.DEVICE_ID,
                                                                  TestUpfConstants.APP_ID,
                                                                  TestUpfConstants.DEFAULT_PRIORITY);
        } catch (UpfProgrammableException e) {
            assertThat("Abstract uplink interface should correctly translate to Fabric interface without error",
                       false);
            return;
        }
        assertThat(translatedRule, equalTo(expectedRule));
    }

    @Test
    public void downlinkInterfaceToFabricEntryTest() {
        FlowRule translatedRule;
        FlowRule expectedRule = TestUpfConstants.FABRIC_DOWNLINK_INTERFACE;
        try {
            translatedRule = upfTranslator.interfaceToFabricEntry(TestUpfConstants.DOWNLINK_INTERFACE,
                                                                  TestUpfConstants.DEVICE_ID,
                                                                  TestUpfConstants.APP_ID,
                                                                  TestUpfConstants.DEFAULT_PRIORITY);
        } catch (UpfProgrammableException e) {
            assertThat("Abstract downlink interface should correctly translate to Fabric interface without error",
                       false);
            return;
        }
        assertThat(translatedRule, equalTo(expectedRule));
    }

    @Test
    public void downlinkPdrToFabricEntryTest() {
        pdrToFabricDownlink(TestUpfConstants.FABRIC_DOWNLINK_PDR, TestUpfConstants.DOWNLINK_PDR);
    }

    @Test
    public void downlinkPdrToFabricQosEntryTest() {
        pdrToFabricDownlink(TestUpfConstants.FABRIC_DOWNLINK_QOS_PDR, TestUpfConstants.DOWNLINK_QOS_PDR);
        pdrToFabricDownlink(TestUpfConstants.FABRIC_DOWNLINK_QOS_4G_PDR, TestUpfConstants.DOWNLINK_QOS_4G_PDR);
    }

    private void pdrToFabricDownlink(FlowRule expected, PacketDetectionRule pdr) {
        FlowRule translatedRule;
        try {
            translatedRule = upfTranslator.pdrToFabricEntry(pdr,
                                                            TestUpfConstants.DEVICE_ID,
                                                            TestUpfConstants.APP_ID,
                                                            TestUpfConstants.DEFAULT_PRIORITY);
        } catch (UpfProgrammableException e) {
            assertThat("Abstract downlink PDR should correctly translate to Fabric PDR without error",
                       false);
            return;
        }
        assertThat(translatedRule, equalTo(expected));
    }

    @Test
    public void uplinkFarToFabricEntryTest() {
        FlowRule translatedRule;
        FlowRule expectedRule = TestUpfConstants.FABRIC_UPLINK_FAR;
        try {
            translatedRule = upfTranslator.farToFabricEntry(TestUpfConstants.UPLINK_FAR,
                                                            TestUpfConstants.DEVICE_ID,
                                                            TestUpfConstants.APP_ID,
                                                            TestUpfConstants.DEFAULT_PRIORITY);
        } catch (UpfProgrammableException e) {
            assertThat("Abstract uplink FAR should correctly translate to Fabric FAR without error",
                       false);
            return;
        }
        assertThat(translatedRule, equalTo(expectedRule));
    }

    @Test
    public void uplinkPdrToFabricEntryTest() {
        pdrToFabricUplink(TestUpfConstants.FABRIC_UPLINK_PDR, TestUpfConstants.UPLINK_PDR);
    }

    @Test
    public void uplinkQosPdrToFabricEntryTest() {
        pdrToFabricUplink(TestUpfConstants.FABRIC_UPLINK_QOS_PDR, TestUpfConstants.UPLINK_QOS_PDR);
        pdrToFabricUplink(TestUpfConstants.FABRIC_UPLINK_QOS_4G_PDR, TestUpfConstants.UPLINK_QOS_4G_PDR);
    }

    private void pdrToFabricUplink(FlowRule expected, PacketDetectionRule pdr) {
        FlowRule translatedRule;
        try {
            translatedRule = upfTranslator.pdrToFabricEntry(pdr,
                                                            TestUpfConstants.DEVICE_ID,
                                                            TestUpfConstants.APP_ID,
                                                            TestUpfConstants.DEFAULT_PRIORITY);
        } catch (UpfProgrammableException e) {
            assertThat("Abstract uplink PDR should correctly translate to Fabric PDR without error",
                       false);
            return;
        }
        assertThat(translatedRule, equalTo(expected));
    }

    @Test
    public void downlinkFarToFabricEntryTest() {
        FlowRule translatedRule;
        FlowRule expectedRule = TestUpfConstants.FABRIC_DOWNLINK_FAR;
        try {
            translatedRule = upfTranslator.farToFabricEntry(TestUpfConstants.DOWNLINK_FAR,
                                                            TestUpfConstants.DEVICE_ID,
                                                            TestUpfConstants.APP_ID,
                                                            TestUpfConstants.DEFAULT_PRIORITY);
        } catch (UpfProgrammableException e) {
            assertThat("Abstract downlink FAR should correctly translate to Fabric FAR without error",
                       false);
            return;
        }
        assertThat(translatedRule, equalTo(expectedRule));
    }
}
