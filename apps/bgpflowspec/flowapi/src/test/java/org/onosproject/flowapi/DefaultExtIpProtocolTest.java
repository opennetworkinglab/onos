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
package org.onosproject.flowapi;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for extended icmp type value attribute.
 */
public class DefaultExtIpProtocolTest {

    private List<ExtOperatorValue> ipProtocol = new ArrayList<>();
    private List<ExtOperatorValue> ipProtocol1 = new ArrayList<>();
    private ExtOperatorValue opVal = new ExtOperatorValue((byte) 1, new byte[100]);
    private ExtOperatorValue opVal1 = new ExtOperatorValue((byte) 1, new byte[200]);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.IP_PROTO_LIST;

    @Test
    public void basics() {
        ipProtocol.add(opVal);
        ipProtocol1.add(opVal1);
        DefaultExtIpProtocol data = new DefaultExtIpProtocol(ipProtocol, type);
        DefaultExtIpProtocol sameAsData = new DefaultExtIpProtocol(ipProtocol, type);
        DefaultExtIpProtocol diffData = new DefaultExtIpProtocol(ipProtocol1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}