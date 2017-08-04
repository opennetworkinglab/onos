/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.bgpio.protocol.flowspec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.onosproject.bgpio.types.BgpFsActionTrafficRate;
import org.onosproject.bgpio.types.BgpFsOperatorValue;
import org.onosproject.bgpio.types.BgpFsPortNum;
import org.onosproject.bgpio.types.BgpValueType;

import com.google.common.testing.EqualsTester;

/**
 * Test for BgpFlowSpecNlri flow specification.
 */
public class BgpFlowSpecNlriTest {

    List<BgpValueType> flowSpecComponents1 = new LinkedList<>();
    private List<BgpFsOperatorValue> operatorValue1 = new ArrayList<>();
    BgpFlowSpecNlri flowSpecDetails1 = new BgpFlowSpecNlri(flowSpecComponents1);
    BgpFsPortNum portNum1 = new BgpFsPortNum(operatorValue1);

    List<BgpValueType> flowSpecComponents = new LinkedList<>();
    private List<BgpFsOperatorValue> operatorValue = new ArrayList<>();
    BgpFlowSpecNlri flowSpecDetails = new BgpFlowSpecNlri(flowSpecComponents);
    BgpFsPortNum portNum = new BgpFsPortNum(operatorValue);

    List<BgpValueType> flowSpecComponents2 = new LinkedList<>();
    private List<BgpFsOperatorValue> operatorValue2 = new ArrayList<>();
    BgpFlowSpecNlri flowSpecDetails2 = new BgpFlowSpecNlri(flowSpecComponents2);
    BgpFsPortNum portNum2 = new BgpFsPortNum(operatorValue2);

    @Test
    public void testEquality() {
        flowSpecComponents1.add(portNum1);
        byte[] port1 = new byte[] {(byte) 0x1 };
        operatorValue1.add(new BgpFsOperatorValue((byte) 0x81, port1));
        byte[] port11 = new byte[] {(byte) 0x1 };
        operatorValue1.add(new BgpFsOperatorValue((byte) 0x82, port11));

        List<BgpValueType> fsTlvs1 = new LinkedList<>();
        fsTlvs1.add(new BgpFsActionTrafficRate((short) 1, 1));
        flowSpecDetails1.setFsActionTlv(fsTlvs1);

        flowSpecComponents.add(portNum);
        byte[] port = new byte[] {(byte) 0x1 };
        operatorValue.add(new BgpFsOperatorValue((byte) 0x81, port));
        byte[] port4 = new byte[] {(byte) 0x1 };
        operatorValue.add(new BgpFsOperatorValue((byte) 0x82, port4));

        List<BgpValueType> fsTlvs = new LinkedList<>();
        fsTlvs.add(new BgpFsActionTrafficRate((short) 1, 1));

        flowSpecComponents2.add(portNum2);
        byte[] port2 = new byte[] {(byte) 0x1 };
        operatorValue2.add(new BgpFsOperatorValue((byte) 0x82, port2));
        byte[] port22 = new byte[] {(byte) 0x1 };
        operatorValue2.add(new BgpFsOperatorValue((byte) 0x82, port22));

        List<BgpValueType> fsTlvs2 = new LinkedList<>();
        fsTlvs2.add(new BgpFsActionTrafficRate((short) 1, 1));
        flowSpecDetails2.setFsActionTlv(fsTlvs2);

        new EqualsTester()
        .addEqualityGroup(flowSpecDetails1, flowSpecDetails)
        .addEqualityGroup(flowSpecDetails2)
        .testEquals();
    }
}
