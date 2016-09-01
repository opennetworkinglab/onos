/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pcelabelstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.pcelabelstore.api.LspLocalLabelInfo;

/**
 * Unit tests for DefaultLspLocalLabelInfo class.
 */
public class DefaultLspLocalLabelInfoTest {

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        // create same two objects.
        DeviceId deviceId1 = DeviceId.deviceId("foo");
        LabelResourceId inLabelId1 = LabelResourceId.labelResourceId(1);
        LabelResourceId outLabelId1 = LabelResourceId.labelResourceId(2);
        PortNumber inPort1 = PortNumber.portNumber(5122);
        PortNumber outPort1 = PortNumber.portNumber(5123);

        LspLocalLabelInfo lspLocalLabel1 = DefaultLspLocalLabelInfo.builder()
                .deviceId(deviceId1)
                .inLabelId(inLabelId1)
                .outLabelId(outLabelId1)
                .inPort(inPort1)
                .outPort(outPort1)
                .build();

        // create same object as above object
        LspLocalLabelInfo sameLocalLabel1 = DefaultLspLocalLabelInfo.builder()
                .deviceId(deviceId1)
                .inLabelId(inLabelId1)
                .outLabelId(outLabelId1)
                .inPort(inPort1)
                .outPort(outPort1)
                .build();

        // Create different object.
        DeviceId deviceId2 = DeviceId.deviceId("goo");
        LabelResourceId inLabelId2 = LabelResourceId.labelResourceId(3);
        LabelResourceId outLabelId2 = LabelResourceId.labelResourceId(4);
        PortNumber inPort2 = PortNumber.portNumber(5124);
        PortNumber outPort2 = PortNumber.portNumber(5125);

        LspLocalLabelInfo lspLocalLabel2 = DefaultLspLocalLabelInfo.builder()
                .deviceId(deviceId2)
                .inLabelId(inLabelId2)
                .outLabelId(outLabelId2)
                .inPort(inPort2)
                .outPort(outPort2)
                .build();

        new EqualsTester().addEqualityGroup(lspLocalLabel1, sameLocalLabel1)
                          .addEqualityGroup(lspLocalLabel2)
                          .testEquals();
    }

    /**
     * Checks the construction of a DefaultLspLocalLabelInfo object.
     */
    @Test
    public void testConstruction() {
        DeviceId deviceId = DeviceId.deviceId("foo");
        LabelResourceId inLabelId = LabelResourceId.labelResourceId(1);
        LabelResourceId outLabelId = LabelResourceId.labelResourceId(2);
        PortNumber inPort = PortNumber.portNumber(5122);
        PortNumber outPort = PortNumber.portNumber(5123);

        LspLocalLabelInfo lspLocalLabel = DefaultLspLocalLabelInfo.builder()
                .deviceId(deviceId)
                .inLabelId(inLabelId)
                .outLabelId(outLabelId)
                .inPort(inPort)
                .outPort(outPort)
                .build();

        assertThat(deviceId, is(lspLocalLabel.deviceId()));
        assertThat(inLabelId, is(lspLocalLabel.inLabelId()));
        assertThat(outLabelId, is(lspLocalLabel.outLabelId()));
        assertThat(inPort, is(lspLocalLabel.inPort()));
        assertThat(outPort, is(lspLocalLabel.outPort()));
    }
}
