/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.types.attr;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for BGP link IGP metric attribute.
 */
public class BgpLinkAttrIgpMetricTest {
    private final int val = 0x010203;
    private final int valLen = 3;
    private final int val1 = 0x01020304;
    private final int val1Len = 4;

    private final BgpLinkAttrIgpMetric data = BgpLinkAttrIgpMetric.of(val,
                                                                      valLen);
    private final BgpLinkAttrIgpMetric sameAsData = BgpLinkAttrIgpMetric
            .of(val, valLen);
    private final BgpLinkAttrIgpMetric diffData = BgpLinkAttrIgpMetric
            .of(val1, val1Len);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(data, sameAsData)
        .addEqualityGroup(diffData).testEquals();
    }
}
