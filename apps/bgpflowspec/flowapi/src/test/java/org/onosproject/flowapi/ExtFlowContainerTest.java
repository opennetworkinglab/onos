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
 * Test for extended flow container value attribute.
 */
public class ExtFlowContainerTest {

    private List<ExtFlowTypes> container = new ArrayList<>();
    private List<ExtFlowTypes> container1 = new ArrayList<>();
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.EXT_FLOW_RULE_KEY;
    ExtFlowTypes val = new DefaultExtKeyName("Name", type);
    ExtFlowTypes val1 = new DefaultExtKeyName("Name1", type);

    @Test
    public void basics() {
        container.add(val);
        container1.add(val1);
        ExtFlowContainer data = new ExtFlowContainer(container);
        ExtFlowContainer sameAsData = new ExtFlowContainer(container);
        ExtFlowContainer diffData = new ExtFlowContainer(container1);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}