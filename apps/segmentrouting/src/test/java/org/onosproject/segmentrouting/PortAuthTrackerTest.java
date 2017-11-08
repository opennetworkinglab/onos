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

package org.onosproject.segmentrouting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.PortAuthTracker.BlockState;
import org.onosproject.segmentrouting.config.BlockedPortsConfig;
import org.onosproject.segmentrouting.config.BlockedPortsConfigTest;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.ConnectPoint.deviceConnectPoint;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.segmentrouting.PortAuthTracker.BlockState.AUTHENTICATED;
import static org.onosproject.segmentrouting.PortAuthTracker.BlockState.BLOCKED;
import static org.onosproject.segmentrouting.PortAuthTracker.BlockState.UNCHECKED;

/**
 * Unit Tests for {@link PortAuthTracker}.
 */
public class PortAuthTrackerTest {
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "foo");
    private static final String KEY = "blocked";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PATH_CFG = "/blocked-ports.json";
    private static final String PATH_CFG_ALT = "/blocked-ports-alt.json";

    private static final String DEV1 = "of:0000000000000001";
    private static final String DEV3 = "of:0000000000000003";
    private static final String DEV4 = "of:0000000000000004";

    private BlockedPortsConfig cfg;
    private AugmentedPortAuthTracker tracker;

    private void print(String s) {
        System.out.println(s);
    }

    private void print(Object o) {
        print(o.toString());
    }

    private void print(String fmt, Object... params) {
        print(String.format(fmt, params));
    }

    private void title(String s) {
        print("=== %s ===", s);
    }

    private BlockedPortsConfig makeConfig(String path) throws IOException {
        InputStream blockedPortsJson = BlockedPortsConfigTest.class
                .getResourceAsStream(path);
        JsonNode node = MAPPER.readTree(blockedPortsJson);
        BlockedPortsConfig cfg = new BlockedPortsConfig();
        cfg.init(APP_ID, KEY, node, MAPPER, null);
        return cfg;
    }

    ConnectPoint cp(String devId, int port) {
        return ConnectPoint.deviceConnectPoint(devId + "/" + port);
    }

    @Before
    public void setUp() throws IOException {
        cfg = makeConfig(PATH_CFG);
        tracker = new AugmentedPortAuthTracker();
    }

    private void verifyPortState(String devId, int first, BlockState... states) {
        DeviceId dev = deviceId(devId);
        int last = first + states.length;
        int pn = first;
        int i = 0;
        while (pn < last) {
            PortNumber pnum = portNumber(pn);
            BlockState actual = tracker.currentState(dev, pnum);
            print("%s/%s [%s]  --> %s", devId, pn, states[i], actual);
            assertEquals("oops: " + devId + "/" + pn + "~" + actual,
                         states[i], actual);
            pn++;
            i++;
        }
    }

    @Test
    public void basic() {
        title("basic");
        print(tracker);
        print(cfg);

        assertEquals("wrong entry count", 0, tracker.entryCount());

        // let's assume that the net config just got loaded..
        tracker.configurePortBlocking(cfg);
        assertEquals("wrong entry count", 13, tracker.entryCount());

        verifyPortState(DEV1, 1, BLOCKED, BLOCKED, BLOCKED, BLOCKED, UNCHECKED);
        verifyPortState(DEV1, 6, UNCHECKED, BLOCKED, BLOCKED, BLOCKED, UNCHECKED);

        verifyPortState(DEV3, 1, UNCHECKED, UNCHECKED, UNCHECKED);
        verifyPortState(DEV3, 6, UNCHECKED, BLOCKED, BLOCKED, BLOCKED, UNCHECKED);

        verifyPortState(DEV4, 1, BLOCKED, UNCHECKED, UNCHECKED, UNCHECKED, BLOCKED);
    }

    @Test
    public void logonLogoff() {
        title("logonLogoff");

        tracker.configurePortBlocking(cfg);
        assertEquals("wrong entry count", 13, tracker.entryCount());
        verifyPortState(DEV1, 1, BLOCKED, BLOCKED, BLOCKED);

        ConnectPoint cp = deviceConnectPoint(DEV1 + "/2");
        tracker.radiusAuthorize(cp);
        print("");
        verifyPortState(DEV1, 1, BLOCKED, AUTHENTICATED, BLOCKED);

        tracker.radiusLogoff(cp);
        print("");
        verifyPortState(DEV1, 1, BLOCKED, BLOCKED, BLOCKED);
    }

    @Test
    public void installedFlows() {
        title("installed flows");

        assertEquals(0, tracker.installed.size());
        tracker.configurePortBlocking(cfg);
        assertEquals(13, tracker.installed.size());

        assertTrue(tracker.installed.contains(cp(DEV1, 1)));
        assertTrue(tracker.installed.contains(cp(DEV3, 7)));
        assertTrue(tracker.installed.contains(cp(DEV4, 5)));
    }

    @Test
    public void flowsLogonLogoff() {
        title("flows logon logoff");

        tracker.configurePortBlocking(cfg);

        // let's pick a connect point from the configuration
        ConnectPoint cp = cp(DEV4, 5);

        assertTrue(tracker.installed.contains(cp));
        assertEquals(0, tracker.cleared.size());

        tracker.resetMetrics();
        tracker.radiusAuthorize(cp);
        // verify we requested the blocking flow to be cleared
        assertTrue(tracker.cleared.contains(cp));

        tracker.resetMetrics();
        assertEquals(0, tracker.installed.size());
        tracker.radiusLogoff(cp);
        // verify we requested the blocking flow to be reinstated
        assertTrue(tracker.installed.contains(cp));
    }

    @Test
    public void uncheckedPortIgnored() {
        title("unchecked port ignored");

        tracker.configurePortBlocking(cfg);
        tracker.resetMetrics();

        // let's pick a connect point NOT in the configuration
        ConnectPoint cp = cp(DEV4, 2);
        assertEquals(BlockState.UNCHECKED, tracker.currentState(cp));

        assertEquals(0, tracker.installed.size());
        assertEquals(0, tracker.cleared.size());
        tracker.radiusAuthorize(cp);
        assertEquals(0, tracker.installed.size());
        assertEquals(0, tracker.cleared.size());
        tracker.radiusLogoff(cp);
        assertEquals(0, tracker.installed.size());
        assertEquals(0, tracker.cleared.size());
    }

    @Test
    public void reconfiguration() throws IOException {
        title("reconfiguration");

        /* see 'blocked-ports.json' and 'blocked-ports-alt.json'

          cfg:  "1": ["1-4", "7-9"],
                "3": ["7-9"],
                "4": ["1", "5", "9"]

          alt:  "1": ["1-9"],
                "3": ["7"],
                "4": ["1"]
         */
        tracker.configurePortBlocking(cfg);
        // dev1: ports 5 and 6 are NOT configured in the original CFG
        assertFalse(tracker.installed.contains(cp(DEV1, 5)));
        assertFalse(tracker.installed.contains(cp(DEV1, 6)));

        tracker.resetMetrics();
        assertEquals(0, tracker.installed.size());
        assertEquals(0, tracker.cleared.size());

        BlockedPortsConfig alt = makeConfig(PATH_CFG_ALT);
        tracker.configurePortBlocking(alt);

        // dev1: ports 5 and 6 ARE configured in the alternate CFG
        assertTrue(tracker.installed.contains(cp(DEV1, 5)));
        assertTrue(tracker.installed.contains(cp(DEV1, 6)));

        // also, check for the ports that were decommissioned
        assertTrue(tracker.cleared.contains(cp(DEV3, 8)));
        assertTrue(tracker.cleared.contains(cp(DEV3, 9)));
        assertTrue(tracker.cleared.contains(cp(DEV4, 5)));
        assertTrue(tracker.cleared.contains(cp(DEV4, 9)));
    }
}
