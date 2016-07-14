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
package org.onosproject.store.cluster.messaging;


import org.junit.Test;
import org.onlab.packet.IpAddress;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the Endpoint class.
 */
public class EndpointTest {
    IpAddress host1 = IpAddress.valueOf("1.2.3.4");
    IpAddress host2 = IpAddress.valueOf("1.2.3.5");

    private final Endpoint endpoint1 = new Endpoint(host1, 1);
    private final Endpoint sameAsEndpoint1 = new Endpoint(host1, 1);
    private final Endpoint endpoint2 = new Endpoint(host2, 1);
    private final Endpoint endpoint3 = new Endpoint(host1, 2);

    /**
     * Checks that the MessageSubject class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(Endpoint.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(endpoint1, sameAsEndpoint1)
                .addEqualityGroup(endpoint2)
                .addEqualityGroup(endpoint3)
                .testEquals();
    }

    /**
     * Checks the construction of a MessageSubject object.
     */
    @Test
    public void testConstruction() {
        assertThat(endpoint2.host(), is(host2));
        assertThat(endpoint2.port(), is(1));
    }
}
