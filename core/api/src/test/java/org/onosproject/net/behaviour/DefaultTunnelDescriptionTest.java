/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class DefaultTunnelDescriptionTest {

    private static final String DID_1 = "device1";
    private static final String IFACE_NAME_1 = "eth1";
    private static final TunnelKey<Long> KEY_1 = new TunnelKey<>(1L);
    private static final TunnelEndPoint<Long> LOCAL_1 =
            new TunnelEndPoint<>(11L);
    private static final TunnelEndPoint<Long> REMOTE_1 =
            new TunnelEndPoint<>(12L);
    private static final SparseAnnotations ANNOTATIONS_1 =
            DefaultAnnotations.builder()
                    .set("AAA", "AAA")
                    .build();

    private TunnelDescription tunnelDescription1 =
            DefaultTunnelDescription.builder()
                    .deviceId(DID_1)
                    .ifaceName(IFACE_NAME_1)
                    .key(KEY_1)
                    .type(TunnelDescription.Type.GRE)
                    .local(LOCAL_1)
                    .remote(REMOTE_1)
                    .otherConfigs(ANNOTATIONS_1)
                    .build();

    private TunnelDescription tunnelDescription2 =
            DefaultTunnelDescription.builder()
                    .deviceId(DID_1)
                    .ifaceName(IFACE_NAME_1)
                    .key(KEY_1)
                    .type(TunnelDescription.Type.GRE)
                    .remote(LOCAL_1)
                    .local(REMOTE_1)
                    .build();

    @Test
    public void testConstruction() {
        assertThat(tunnelDescription1.deviceId(), optionalWithValue(is(DID_1)));
        assertThat(tunnelDescription1.ifaceName(), is(IFACE_NAME_1));
        assertThat(tunnelDescription1.key(), optionalWithValue(is(KEY_1)));
        assertThat(tunnelDescription1.type(), is(TunnelDescription.Type.GRE));
        assertThat(tunnelDescription1.local(), optionalWithValue(is(LOCAL_1)));
        assertThat(tunnelDescription1.remote(), optionalWithValue(is(REMOTE_1)));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(tunnelDescription1)
                .addEqualityGroup(tunnelDescription2)
                .testEquals();
    }
}
