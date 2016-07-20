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
 * Test for extended fragment value attribute.
 */
public class DefaultExtFragmentTest {

    private List<ExtOperatorValue> fragment = new ArrayList<>();
    private List<ExtOperatorValue> fragment1 = new ArrayList<>();
    private ExtOperatorValue opVal = new ExtOperatorValue((byte) 1, new byte[100]);
    private ExtOperatorValue opVal1 = new ExtOperatorValue((byte) 1, new byte[200]);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.FRAGMENT_LIST;

    @Test
    public void basics() {
        fragment.add(opVal);
        fragment1.add(opVal1);
        DefaultExtFragment data = new DefaultExtFragment(fragment, type);
        DefaultExtFragment sameAsData = new DefaultExtFragment(fragment, type);
        DefaultExtFragment diffData = new DefaultExtFragment(fragment1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}