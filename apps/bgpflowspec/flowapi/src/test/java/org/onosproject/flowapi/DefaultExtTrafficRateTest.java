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
 * Test for extended traffic rate value attribute.
 */
public class DefaultExtTrafficRateTest {

    private Short asn = new Short((short) 1);
    private Float rate = new Float(1.0);
    private Short asn1 = new Short((short) 2);
    private Float rate1 = new Float(1.0);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.TRAFFIC_RATE;

    @Test
    public void basics() {
        DefaultExtTrafficRate data = new DefaultExtTrafficRate(asn, rate, type);
        DefaultExtTrafficRate sameAsData = new DefaultExtTrafficRate(asn, rate, type);
        DefaultExtTrafficRate diffData = new DefaultExtTrafficRate(asn1, rate1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}