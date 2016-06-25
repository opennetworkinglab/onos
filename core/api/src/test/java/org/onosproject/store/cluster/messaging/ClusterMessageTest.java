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
import org.onosproject.cluster.NodeId;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Units tests for ClusterMessage class.
 */
public class ClusterMessageTest {
    private final MessageSubject subject1 = new MessageSubject("Message 1");
    private final MessageSubject subject2 = new MessageSubject("Message 2");

    private final byte[] payload1 = {0, 1, 2, 3, 4, 5};
    private final byte[] payload2 = {0, 1, 2, 3, 4, 5, 6};

    private final NodeId nodeId = new NodeId("node");

    private final ClusterMessage message1 =
            new ClusterMessage(nodeId, subject1, payload1);
    private final ClusterMessage sameAsMessage1 =
            new ClusterMessage(nodeId, subject1, payload1);
    private final ClusterMessage message2 =
            new ClusterMessage(nodeId, subject1, payload2);
    private final ClusterMessage message3 =
            new ClusterMessage(nodeId, subject2, payload1);

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(message1, sameAsMessage1)
                .addEqualityGroup(message2)
                .addEqualityGroup(message3)
                .testEquals();
    }

    /**
     * Checks the construction of a FlowId object.
     */
    @Test
    public void testConstruction() {
        assertThat(message1.payload(), is(payload1));
        assertThat(message1.sender(), is(nodeId));
        assertThat(message1.subject(), is(subject1));

        byte[] response = {2, 2, 2, 2, 2, 2, 2, 2};
        message1.respond(response);
        assertThat(message1.response(), is(response));
    }

    /**
     * Tests the toBytes and fromBytes methods.
     */
    @Test
    public void testByteMethods() {
        byte[] fromBytes = message3.getBytes();
        ClusterMessage message = ClusterMessage.fromBytes(fromBytes);
        assertThat(message, is(message3));
    }
}
