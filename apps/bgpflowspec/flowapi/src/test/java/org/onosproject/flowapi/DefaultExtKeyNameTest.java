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

/**
 * Test for extended key name value attribute.
 */
public class DefaultExtKeyNameTest {

    private String keyName = new String("hello");
    private String keyName1 = new String("Hi");

    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.EXT_FLOW_RULE_KEY;

    @Test
    public void basics() {

        DefaultExtKeyName data = new DefaultExtKeyName(keyName, type);
        DefaultExtKeyName sameAsData = new DefaultExtKeyName(keyName, type);
        DefaultExtKeyName diffData = new DefaultExtKeyName(keyName1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}