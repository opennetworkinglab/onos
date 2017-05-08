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
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.NetTestTools;

import com.google.common.collect.ImmutableList;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for protected transport endpoint state.
 */
public class ProtectedTransportEndpointStateTest {

    private List<TransportEndpointDescription> paths = ImmutableList.of();
    private DeviceId peer = NetTestTools.did("d1");
    private String fingerprint = "aaa";

    private TransportEndpointDescription description =
        TransportEndpointDescription
            .builder()
            .withEnabled(true)
            .withOutput(new FilteredConnectPoint(NetTestTools.connectPoint("xxx", 1)))
            .build();

    private ProtectedTransportEndpointDescription protectedDescription =
            ProtectedTransportEndpointDescription.buildDescription(paths, peer,
                                                                   fingerprint);
    private TransportEndpointState state1 =
        TransportEndpointState
            .builder()
            .withId(TransportEndpointId.of("1"))
            .withDescription(description)
            .withLive(true)
            .build();
    private List<TransportEndpointState> pathStates = ImmutableList.of(state1);


    @Test
    public void testConstruction() {

        ProtectedTransportEndpointState state =
            ProtectedTransportEndpointState
                .builder()
                .withActivePathIndex(0)
                .withDescription(protectedDescription)
                .withPathStates(pathStates)
                .build();

        assertThat(state, notNullValue());
        assertThat(state.description(), is(protectedDescription));
        assertThat(state.pathStates(), contains(state1));
        assertThat(state.workingPathIndex(), is(0));
    }

    @Test
    public void testToString() {
        ProtectedTransportEndpointState state1 =
                ProtectedTransportEndpointState
                        .builder()
                        .withActivePathIndex(0)
                        .withDescription(protectedDescription)
                        .withPathStates(pathStates)
                        .build();
        ProtectedTransportEndpointState state2 =
                ProtectedTransportEndpointState
                        .builder()
                        .copyFrom(state1)
                        .build();
        assertThat(state1.toString(), is(state2.toString()));
    }
}
