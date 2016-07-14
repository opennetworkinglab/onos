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
 * Test for extended traffic redirect value attribute.
 */
public class DefaultExtTrafficRedirectTest {

    String redirect = new String("vpnid1");
    String redirect1 = new String("vpnid2");
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.TRAFFIC_REDIRECT;

    @Test
    public void basics() {

        DefaultExtTrafficRedirect data = new DefaultExtTrafficRedirect(redirect, type);
        DefaultExtTrafficRedirect sameAsData = new DefaultExtTrafficRedirect(redirect, type);
        DefaultExtTrafficRedirect diffData = new DefaultExtTrafficRedirect(redirect1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}