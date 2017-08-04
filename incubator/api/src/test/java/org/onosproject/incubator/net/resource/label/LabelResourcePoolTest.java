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
package org.onosproject.incubator.net.resource.label;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;

import com.google.common.testing.EqualsTester;

/**
 * Tests of the label resource pool.
 */
public class LabelResourcePoolTest extends AbstractEventTest {

    @Test
    public void testEquality() {
        LabelResourcePool h1 = new LabelResourcePool("of:001", 0, 100);
        LabelResourcePool h2 = new LabelResourcePool("of:001", 0, 100);
        LabelResourcePool h3 = new LabelResourcePool("of:002", 0, 100);
        LabelResourcePool h4 = new LabelResourcePool("of:002", 0, 100);
        new EqualsTester().addEqualityGroup(h1, h2).addEqualityGroup(h3, h4)
                .testEquals();
    }

}
