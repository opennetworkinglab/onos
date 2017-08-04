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

import java.util.Collections;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DeviceId;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

/**
 * Tests of the label resource request.
 */
public class LabelResourceRequestTest extends AbstractEventTest {

    @Test
    public void testEquality() {
        DeviceId deviceId1 = DeviceId.deviceId("of:0001");
        DeviceId deviceId2 = DeviceId.deviceId("of:0002");
        long apply = 2;
        ImmutableSet<LabelResource> releaseCollection = ImmutableSet
                .copyOf(Collections.emptySet());
        LabelResourceRequest h1 = new LabelResourceRequest(
                                                           deviceId1,
                                                           LabelResourceRequest.Type.APPLY,
                                                           apply, null);
        LabelResourceRequest h2 = new LabelResourceRequest(
                                                           deviceId1,
                                                           LabelResourceRequest.Type.APPLY,
                                                           apply, null);
        LabelResourceRequest h3 = new LabelResourceRequest(
                                                           deviceId2,
                                                           LabelResourceRequest.Type.RELEASE,
                                                           0, releaseCollection);
        LabelResourceRequest h4 = new LabelResourceRequest(
                                                           deviceId2,
                                                           LabelResourceRequest.Type.RELEASE,
                                                           0, releaseCollection);

        new EqualsTester().addEqualityGroup(h1, h2).addEqualityGroup(h3, h4)
                .testEquals();
    }
}
