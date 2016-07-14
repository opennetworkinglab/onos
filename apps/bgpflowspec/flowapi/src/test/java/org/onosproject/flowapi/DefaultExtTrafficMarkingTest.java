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
 * Test for extended traffic marking value attribute.
 */
public class DefaultExtTrafficMarkingTest {

    private byte marking = 01;
    private byte marking1 = 02;
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.TRAFFIC_MARKING;

    @Test
    public void basics() {
        DefaultExtTrafficMarking data = new DefaultExtTrafficMarking(marking, type);
        DefaultExtTrafficMarking sameAsData = new DefaultExtTrafficMarking(marking, type);
        DefaultExtTrafficMarking diffData = new DefaultExtTrafficMarking(marking1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}