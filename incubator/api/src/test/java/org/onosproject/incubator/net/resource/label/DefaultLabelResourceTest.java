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
 * Tests of default label resource.
 */
public class DefaultLabelResourceTest extends AbstractEventTest {

    @Test
    public void testEquality() {
        String deviceId1 = "of:001";
        String deviceId2 = "of:002";
        long labelResourceId1 = 100;
        long labelResourceId2 = 200;
        DefaultLabelResource h1 = new DefaultLabelResource(deviceId1,
                                                           labelResourceId1);
        DefaultLabelResource h2 = new DefaultLabelResource(deviceId1,
                                                           labelResourceId1);
        DefaultLabelResource h3 = new DefaultLabelResource(deviceId2,
                                                           labelResourceId2);
        DefaultLabelResource h4 = new DefaultLabelResource(deviceId2,
                                                           labelResourceId2);

        new EqualsTester().addEqualityGroup(h1, h2).addEqualityGroup(h3, h4)
                .testEquals();
    }
}
