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
 * Test for extended tcp flag value attribute.
 */
public class DefaultExtTcpFlagTest {

    private List<ExtOperatorValue> tcpFlag = new ArrayList<>();
    private List<ExtOperatorValue> tcpFlag1 = new ArrayList<>();
    private ExtOperatorValue opVal = new ExtOperatorValue((byte) 1, new byte[100]);
    private ExtOperatorValue opVal1 = new ExtOperatorValue((byte) 1, new byte[200]);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.TCP_FLAG_LIST;

    @Test
    public void basics() {
        tcpFlag.add(opVal);
        tcpFlag1.add(opVal1);
        DefaultExtTcpFlag data = new DefaultExtTcpFlag(tcpFlag, type);
        DefaultExtTcpFlag sameAsData = new DefaultExtTcpFlag(tcpFlag, type);
        DefaultExtTcpFlag diffData = new DefaultExtTcpFlag(tcpFlag1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}