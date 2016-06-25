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
package org.onosproject.pcepio.types;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test case for IgpRouterIdSubTlv.
 */
public class IgpRouterIdSubTlvTest {

    private final byte[] value1 = {1, 2 };
    private final Short length1 = 2;
    private final IgpRouterIdSubTlv tlv1 = IgpRouterIdSubTlv.of(value1, length1);

    private final Short length2 = 2;
    private final IgpRouterIdSubTlv tlv2 = IgpRouterIdSubTlv.of(value1, length2);

    private final byte[] value3 = {1, 2, 3 };
    private final Short length3 = 3;
    private final IgpRouterIdSubTlv tlv3 = IgpRouterIdSubTlv.of(value3, length3);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
