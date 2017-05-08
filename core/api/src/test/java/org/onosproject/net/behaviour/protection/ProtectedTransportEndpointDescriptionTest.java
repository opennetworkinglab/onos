/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.behaviour.protection;

import java.util.List;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for protected transport endpoint description.
 */
public class ProtectedTransportEndpointDescriptionTest {

    @Test
    public void testConstruction() {
        List<TransportEndpointDescription> paths = ImmutableList.of();
        DeviceId peer = NetTestTools.did("d1");
        String fingerprint = "aaa";

        ProtectedTransportEndpointDescription pted =
            ProtectedTransportEndpointDescription.buildDescription(paths, peer,
                                                                   fingerprint);
        assertThat(pted, notNullValue());
        assertThat(pted.paths(), is(paths));
        assertThat(pted.peer(), is(peer));
        assertThat(pted.fingerprint(), is(fingerprint));
    }

    @Test
    public void testEquals() {
        List<TransportEndpointDescription> paths = ImmutableList.of();
        DeviceId peer = NetTestTools.did("d1");
        String fingerprint = "aaa";

        ProtectedTransportEndpointDescription a =
                ProtectedTransportEndpointDescription.of(paths, peer,
                                                         fingerprint);
        ProtectedTransportEndpointDescription b =
                ProtectedTransportEndpointDescription.of(paths, peer,
                                                         fingerprint);
        new EqualsTester()
                .addEqualityGroup(a)
                .addEqualityGroup(b)
                .testEquals();
    }

}
