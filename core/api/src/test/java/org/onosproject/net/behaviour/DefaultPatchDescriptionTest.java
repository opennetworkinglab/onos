/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.behaviour;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;


public class DefaultPatchDescriptionTest {

    private String deviceId1 = "d1";
    private String ifaceName1 = "i1";
    private String peerName1 = "p1";

    private PatchDescription defaultPatchDescription1 =
            DefaultPatchDescription.builder()
            .deviceId(deviceId1)
            .ifaceName(ifaceName1)
            .peer(peerName1)
            .build();
    private PatchDescription sameAsDefaultPatchDescription1 =
            DefaultPatchDescription.builder()
                    .deviceId(deviceId1)
                    .ifaceName(ifaceName1)
                    .peer(peerName1)
                    .build();
    private PatchDescription defaultPatchDescription2 =
            DefaultPatchDescription.builder()
                    .deviceId(deviceId1 + "2")
                    .ifaceName(ifaceName1)
                    .peer(peerName1)
                    .build();
    private PatchDescription defaultPatchDescription3 =
            DefaultPatchDescription.builder()
                    .deviceId(deviceId1)
                    .ifaceName(ifaceName1 + "2")
                    .peer(peerName1)
                    .build();
    private PatchDescription defaultPatchDescription4 =
            DefaultPatchDescription.builder()
                    .deviceId(deviceId1)
                    .ifaceName(ifaceName1)
                    .peer(peerName1 + "2")
                    .build();
    private PatchDescription defaultPatchDescriptionNoDeviceId =
            DefaultPatchDescription.builder()
                    .ifaceName(ifaceName1)
                    .peer(peerName1 + "2")
                    .build();

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPatchDescription.class);
    }

    @Test
    public void testConstruction() {
        assertThat(defaultPatchDescription1.deviceId(), optionalWithValue(is(deviceId1)));
        assertThat(defaultPatchDescription1.ifaceName(), is(ifaceName1));
        assertThat(defaultPatchDescription1.peer(), is(peerName1));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(defaultPatchDescription1, sameAsDefaultPatchDescription1)
                .addEqualityGroup(defaultPatchDescription2)
                .addEqualityGroup(defaultPatchDescription3)
                .addEqualityGroup(defaultPatchDescription4)
                .addEqualityGroup(defaultPatchDescriptionNoDeviceId)
                .testEquals();
    }
}
