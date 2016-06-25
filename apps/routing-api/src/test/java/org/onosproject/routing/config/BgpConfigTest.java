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

package org.onosproject.routing.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.routing.RoutingService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BgpConfigTest {

    private static final ApplicationId APP_ID =
            new TestApplicationId(RoutingService.ROUTER_APP_ID);

    private static final IpAddress IP1 = IpAddress.valueOf("10.0.1.1");
    private static final IpAddress IP2 = IpAddress.valueOf("10.0.2.1");
    private static final IpAddress IP3 = IpAddress.valueOf("10.0.3.1");
    private static final IpAddress IP4 = IpAddress.valueOf("10.0.101.1");
    private static final IpAddress IP5 = IpAddress.valueOf("10.0.201.1");
    public static final IpAddress IP_NON_EXIST = IpAddress.valueOf("10.101.1.1");

    public static final VlanId NO_VLAN = VlanId.NONE;

    public static final ConnectPoint CONNECT_POINT1 = ConnectPoint.
            deviceConnectPoint("of:0000000000000001/1");
    public static final ConnectPoint CONNECT_POINT2 = ConnectPoint.
            deviceConnectPoint("of:00000000000000a3/1");

    private static final String JSON_TREE = "{\"" + BgpConfig.SPEAKERS +
            "\" : [{\"" + BgpConfig.NAME + "\" : \"bgp1\"," +
            "\"" + BgpConfig.CONNECT_POINT +
            "\" : \"of:0000000000000001/1\"," +
            "\"" + BgpConfig.PEERS + "\" : [" +
            "\"10.0.1.1\",\"10.0.2.1\",\"10.0.3.1\"]}]}";
    private static final String EMPTY_JSON_TREE = "{}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final BgpConfig.BgpSpeakerConfig initialSpeaker = createInitialSpeaker();

    private Set<BgpConfig.BgpSpeakerConfig> speakers = new HashSet<>();
    private BgpConfig bgpConfig = new BgpConfig();
    private BgpConfig emptyBgpConfig = new BgpConfig();

    @Before
    public void setUp() throws Exception {
        JsonNode tree = new ObjectMapper().readTree(JSON_TREE);
        bgpConfig.init(APP_ID, "bgp-test", tree, mapper, delegate);
        JsonNode emptyTree = new ObjectMapper().readTree(EMPTY_JSON_TREE);
        emptyBgpConfig.init(APP_ID, "bgp-test", emptyTree, mapper, delegate);
        speakers.add(initialSpeaker);
    }

    /**
     * Tests if speakers can be retrieved from JSON.
     */
    @Test
    public void testBgpSpeakers() throws Exception {
        assertEquals(speakers, bgpConfig.bgpSpeakers());
    }

    /**
     * Tests if speakers can be retrieved from empty JSON.
     */
    @Test
    public void testEmptyBgpSpeakers() throws Exception {
        assertTrue(emptyBgpConfig.bgpSpeakers().isEmpty());
    }

    /**
     * Tests if speaker can be found by name.
     */
    @Test
    public void testGetSpeakerWithName() throws Exception {
        assertNotNull(bgpConfig.getSpeakerWithName("bgp1"));
        assertNull(bgpConfig.getSpeakerWithName("bgp2"));
    }

    /**
     * Tests addition of new speaker.
     */
    @Test
    public void testAddSpeaker() throws Exception {
        int initialSize = bgpConfig.bgpSpeakers().size();
        BgpConfig.BgpSpeakerConfig newSpeaker = createNewSpeaker();
        bgpConfig.addSpeaker(newSpeaker);
        assertEquals(initialSize + 1, bgpConfig.bgpSpeakers().size());
        speakers.add(newSpeaker);
        assertEquals(speakers, bgpConfig.bgpSpeakers());
    }

    /**
     * Tests addition of new speaker to empty configuration.
     */
    @Test
    public void testAddSpeakerToEmpty() throws Exception {
        BgpConfig.BgpSpeakerConfig newSpeaker = createNewSpeaker();
        emptyBgpConfig.addSpeaker(newSpeaker);

        assertFalse(emptyBgpConfig.bgpSpeakers().isEmpty());
    }

    /**
     * Tests removal of existing speaker.
     */
    @Test
    public void testRemoveExistingSpeaker() throws Exception {
        int initialSize = bgpConfig.bgpSpeakers().size();
        bgpConfig.removeSpeaker("bgp1");

        assertEquals(initialSize - 1, bgpConfig.bgpSpeakers().size());
    }

    /**
     * Tests removal of non-existing speaker.
     */
    @Test
    public void testRemoveInexistingSpeaker() throws Exception {
        int initialSize = bgpConfig.bgpSpeakers().size();
        bgpConfig.removeSpeaker("bgp2");

        assertEquals(initialSize, bgpConfig.bgpSpeakers().size());
    }

    /**
     * Tests addition of new speaker.
     */
    @Test
    public void testAddPeerToSpeaker() throws Exception {
        int initialSize = bgpConfig.getSpeakerWithName("bgp1").peers().size();
        bgpConfig.addPeerToSpeaker("bgp1", IP4);

        assertEquals(initialSize + 1, bgpConfig.getSpeakerWithName("bgp1").peers().size());
    }

    /**
     * Tests addition of new speaker when peer already exists.
     */
    @Test
    public void testAddExistingPeerToSpeaker() throws Exception {
        int initialSize = bgpConfig.getSpeakerWithName("bgp1").peers().size();
        bgpConfig.addPeerToSpeaker("bgp1", IP1);

        assertEquals(initialSize, bgpConfig.getSpeakerWithName("bgp1").peers().size());
    }

    /**
     * Tests retrieval of speaker based on peering address.
     */
    @Test
    public void testGetSpeakerFromPeer() throws Exception {
        assertNotNull(bgpConfig.getSpeakerFromPeer(IP1));
        assertNull(bgpConfig.getSpeakerFromPeer(IP_NON_EXIST));
    }

    /**
     * Tests removal of peer.
     */
    @Test
    public void testRemoveExistingPeerFromSpeaker() throws Exception {
        int initialSize = bgpConfig.getSpeakerWithName("bgp1").peers().size();
        bgpConfig.removePeerFromSpeaker(initialSpeaker, IP1);

        assertEquals(initialSize - 1, bgpConfig.getSpeakerWithName("bgp1").peers().size());
    }

    /**
     * Tests peer removal when peer does not exist.
     */
    @Test
    public void testRemoveNonExistingPeerFromSpeaker() throws Exception {
        int initialSize = bgpConfig.getSpeakerWithName("bgp1").peers().size();
        bgpConfig.removePeerFromSpeaker(initialSpeaker, IP_NON_EXIST);

        assertEquals(initialSize, bgpConfig.getSpeakerWithName("bgp1").peers().size());
    }

    /**
     * Tests if connections to peers are found.
     */
    @Test
    public void testIsConnectedToPeer() {
        BgpConfig.BgpSpeakerConfig speaker = createNewSpeaker();

        assertTrue(speaker.isConnectedToPeer(IP4));
        assertFalse(speaker.isConnectedToPeer(IP_NON_EXIST));
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }

    private BgpConfig.BgpSpeakerConfig createInitialSpeaker() {
        Optional<String> speakerName = Optional.of("bgp1");
        ConnectPoint connectPoint = CONNECT_POINT1;
        Set<IpAddress> connectedPeers = new HashSet<>(Arrays.asList(IP1, IP2, IP3));

        return new BgpConfig.BgpSpeakerConfig(speakerName, NO_VLAN,
                                              connectPoint, connectedPeers);
    }

    private BgpConfig.BgpSpeakerConfig createNewSpeaker() {
        Optional<String> speakerName = Optional.of("newSpeaker");
        ConnectPoint connectPoint = CONNECT_POINT2;
        Set<IpAddress> connectedPeers = new HashSet<>(
                Arrays.asList(IP4, IP5));

        return new BgpConfig.BgpSpeakerConfig(speakerName, NO_VLAN,
                                              connectPoint, connectedPeers);
    }
}
