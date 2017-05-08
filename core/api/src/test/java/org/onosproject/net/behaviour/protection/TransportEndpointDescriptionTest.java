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

import org.junit.Test;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.NetTestTools;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the transport endpoint description.
 */
public class TransportEndpointDescriptionTest {

    @Test
    public void testConstruction() {
        FilteredConnectPoint output =
                new FilteredConnectPoint(NetTestTools.connectPoint("xxx", 1));
        TransportEndpointDescription ted =
            TransportEndpointDescription
                .builder()
                .withEnabled(true)
                .withOutput(output)
                .build();

        assertThat(ted, notNullValue());
        assertThat(ted.isEnabled(), is(true));
        assertThat(ted.output(), is(output));
    }

    @Test
    public void testCopyConstruction() {
        FilteredConnectPoint output =
                new FilteredConnectPoint(NetTestTools.connectPoint("xxx", 1));
        TransportEndpointDescription originalTed =
                TransportEndpointDescription
                        .builder()
                        .withEnabled(true)
                        .withOutput(output)
                        .build();
        TransportEndpointDescription ted =
                TransportEndpointDescription
                        .builder()
                        .copyFrom(originalTed)
                        .build();

        assertThat(ted, notNullValue());
        assertThat(ted.isEnabled(), is(true));
        assertThat(ted.output(), is(output));
    }

    @Test
    public void testEquals() {
        FilteredConnectPoint cp =
                new FilteredConnectPoint(NetTestTools.connectPoint("d", 1));

        TransportEndpointDescription ted1 =
                TransportEndpointDescription
                        .builder()
                        .withEnabled(true)
                        .withOutput(cp)
                        .build();

        TransportEndpointDescription ted2 =
                TransportEndpointDescription
                        .builder()
                        .withEnabled(true)
                        .withOutput(cp)
                        .build();

        new EqualsTester()
                .addEqualityGroup(ted1)
                .addEqualityGroup(ted2)
                .testEquals();
    }
}
