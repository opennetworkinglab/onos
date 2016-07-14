/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Test for BGP prefix metric attribute.
 */
public class BgpPrefixAttrMetricTest {
    private final int val = 1111;
    private final int val1 = 2222;

    private final BgpPrefixAttrMetric data = BgpPrefixAttrMetric.of(val);
    private final BgpPrefixAttrMetric sameAsData = BgpPrefixAttrMetric.of(val);
    private final BgpPrefixAttrMetric diffData = BgpPrefixAttrMetric.of(val1);

    @Test
    public void basics() {

        new EqualsTester().addEqualityGroup(data, sameAsData)
        .addEqualityGroup(diffData).testEquals();
    }
}
