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
 * Test for extended wide community value attribute.
 */
public class DefaultExtWideCommunityIntTest {

    private List<Integer> wCommInt = new ArrayList<>();
    private List<Integer> wCommInt1 = new ArrayList<>();
    private Integer opVal = new Integer(1);
    private Integer opVal1 = new Integer(2);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.WIDE_COMM_COMMUNITY;

    @Test
    public void basics() {
        wCommInt.add(opVal);
        wCommInt1.add(opVal1);
        DefaultExtWideCommunityInt data = new DefaultExtWideCommunityInt(wCommInt, type);
        DefaultExtWideCommunityInt sameAsData = new DefaultExtWideCommunityInt(wCommInt, type);
        DefaultExtWideCommunityInt diffData = new DefaultExtWideCommunityInt(wCommInt1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}