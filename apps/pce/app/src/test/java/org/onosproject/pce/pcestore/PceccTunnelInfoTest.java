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
package org.onosproject.pce.pcestore;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pceservice.TunnelConsumerId;
import org.onosproject.pce.pcestore.api.LspLocalLabelInfo;

/**
 * Unit tests for PceccTunnelInfo class.
 */
public class PceccTunnelInfoTest {

   /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        // create same two objects.
        List<LspLocalLabelInfo> lspLocalLabelList1 = new LinkedList<>();
        ResourceConsumer tunnelConsumerId1 = TunnelConsumerId.valueOf(10);

        // create object of DefaultLspLocalLabelInfo
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
        lspLocalLabelList1.add(lspLocalLabel1);

        PceccTunnelInfo pceccTunnelInfo1 = new PceccTunnelInfo(lspLocalLabelList1, tunnelConsumerId1);

        // create same as above object
        PceccTunnelInfo samePceccTunnelInfo1 = new PceccTunnelInfo(lspLocalLabelList1, tunnelConsumerId1);

        // Create different object.
        List<LspLocalLabelInfo> lspLocalLabelInfoList2 = new LinkedList<>();
        ResourceConsumer tunnelConsumerId2 = TunnelConsumerId.valueOf(20);

        // create object of DefaultLspLocalLabelInfo
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
        lspLocalLabelInfoList2.add(lspLocalLabel2);

        PceccTunnelInfo pceccTunnelInfo2 = new PceccTunnelInfo(lspLocalLabelInfoList2, tunnelConsumerId2);

        new EqualsTester().addEqualityGroup(pceccTunnelInfo1, samePceccTunnelInfo1)
                          .addEqualityGroup(pceccTunnelInfo2)
                          .testEquals();
    }

    /**
     * Checks the construction of a PceccTunnelInfo object.
     */
    @Test
    public void testConstruction() {
        List<LspLocalLabelInfo> lspLocalLabelInfoList = new LinkedList<>();
        ResourceConsumer tunnelConsumerId = TunnelConsumerId.valueOf(10);

        // create object of DefaultLspLocalLabelInfo
        DeviceId deviceId = DeviceId.deviceId("foo");
        LabelResourceId inLabelId = LabelResourceId.labelResourceId(1);
        LabelResourceId outLabelId = LabelResourceId.labelResourceId(2);
        PortNumber inPort = PortNumber.portNumber(5122);
        PortNumber outPort = PortNumber.portNumber(5123);

        LspLocalLabelInfo lspLocalLabelInfo = DefaultLspLocalLabelInfo.builder()
                .deviceId(deviceId)
                .inLabelId(inLabelId)
                .outLabelId(outLabelId)
                .inPort(inPort)
                .outPort(outPort)
                .build();
        lspLocalLabelInfoList.add(lspLocalLabelInfo);

        PceccTunnelInfo pceccTunnelInfo = new PceccTunnelInfo(lspLocalLabelInfoList, tunnelConsumerId);

        assertThat(lspLocalLabelInfoList, is(pceccTunnelInfo.lspLocalLabelInfoList()));
    }
}
