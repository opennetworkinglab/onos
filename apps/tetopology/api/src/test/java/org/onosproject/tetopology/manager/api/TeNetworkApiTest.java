/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.manager.api;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.TeTopologyKey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for TE topology APIs.
 */
public class TeNetworkApiTest {
    private static final long DEFAULT_PROVIDER_ID = 1234;
    private static final long DEFAULT_CLIENT_ID = 5678;
    private static final long DEFAULT_TOPOLOGY_ID = 9876;
    private static final String DEFAULT_TOPOLOGY_ID_STRING =
            "default-topology-123";

    private long providerId;
    private long clientId;
    private long topologyId;
    private String topologyIdString;

    @Before
    public void setUp() {
        providerId = DEFAULT_PROVIDER_ID;
        clientId = DEFAULT_CLIENT_ID;
        topologyId = DEFAULT_TOPOLOGY_ID;
        topologyIdString = DEFAULT_TOPOLOGY_ID_STRING;
    }

    @Test
    public void topologyIdEqualOperatorTest() {
        TeTopologyId id1 = new TeTopologyId(providerId, clientId,
                                            topologyIdString);
        TeTopologyId id2 = new TeTopologyId(providerId, clientId,
                                            topologyIdString);
        TeTopologyId id3 = new TeTopologyId(providerId + 1, clientId,
                                            topologyIdString);
        TeTopologyId id4 = new TeTopologyId(providerId, clientId + 1,
                                            topologyIdString);
        TeTopologyId id5 = new TeTopologyId(providerId, clientId,
                                            topologyIdString + "abc");

        assertTrue("Two topology ids must be equal", id1.equals(id2));

        assertFalse("Two topology ids must be unequal", id1.equals(id3));
        assertFalse("Two topology ids must be unequal", id3.equals(id1));

        assertFalse("Two topology ids must be unequal", id1.equals(id4));
        assertFalse("Two topology ids must be unequal", id4.equals(id1));

        assertFalse("Two topology ids must be unequal", id1.equals(id5));
        assertFalse("Two topology ids must be unequal", id5.equals(id1));
    }

    @Test
    public void topologyKeyEqualOperatorTest() {
        TeTopologyKey key1 = new TeTopologyKey(providerId, clientId,
                                               topologyId);
        TeTopologyKey key2 = new TeTopologyKey(providerId, clientId,
                                               topologyId);
        TeTopologyKey key3 = new TeTopologyKey(providerId + 1, clientId,
                                               topologyId);
        TeTopologyKey key4 = new TeTopologyKey(providerId, clientId + 1,
                                               topologyId);
        TeTopologyKey key5 = new TeTopologyKey(providerId, clientId,
                                               topologyId + 1);

        assertTrue("Two topology keys must be equal", key1.equals(key2));

        assertFalse("Two topology keys must be unequal", key1.equals(key3));
        assertFalse("Two topology keys must be unequal", key3.equals(key1));

        assertFalse("Two topology keys must be unequal", key1.equals(key4));
        assertFalse("Two topology keys must be unequal", key4.equals(key1));

        assertFalse("Two topology keys must be unequal", key1.equals(key5));
        assertFalse("Two topology keys must be unequal", key5.equals(key1));
    }
}
