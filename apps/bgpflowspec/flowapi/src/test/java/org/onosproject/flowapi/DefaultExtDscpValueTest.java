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
 * Test for extended dscp value attribute.
 */
public class DefaultExtDscpValueTest {

    private List<ExtOperatorValue> dscpValue = new ArrayList<>();
    private List<ExtOperatorValue> dscpValue1 = new ArrayList<>();
    private ExtOperatorValue opVal = new ExtOperatorValue((byte) 1, new byte[100]);
    private ExtOperatorValue opVal1 = new ExtOperatorValue((byte) 1, new byte[200]);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.DSCP_VALUE_LIST;

    @Test
    public void basics() {
        dscpValue.add(opVal);
        dscpValue1.add(opVal1);
        DefaultExtDscpValue data = new DefaultExtDscpValue(dscpValue, type);
        DefaultExtDscpValue sameAsData = new DefaultExtDscpValue(dscpValue, type);
        DefaultExtDscpValue diffData = new DefaultExtDscpValue(dscpValue1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}