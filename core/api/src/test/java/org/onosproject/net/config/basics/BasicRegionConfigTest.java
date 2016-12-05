/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.InvalidFieldException;
import org.onosproject.net.region.Region;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.region.RegionId.regionId;

/**
 * Test class for {@link BasicRegionConfig}.
 */
public class BasicRegionConfigTest extends AbstractConfigTest {

    private static final String REGION_JSON = "configs.regions.1.json";

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String DEVICES = "devices";

    private static final String R1 = "r1";
    private static final String R2 = "r2";
    private static final String R3 = "r3";

    private static final String EUROPE = "Europe";
    private static final String PARIS = "Paris";
    private static final String AUSTRALIA = "Australia";

    private static final Set<DeviceId> R1_DEVS =
            ImmutableSet.of(dstr("01"), dstr("02"), dstr("03"));
    private static final Set<DeviceId> R2_DEVS =
            ImmutableSet.of(dstr("04"), dstr("05"), dstr("06"));
    private static final Set<DeviceId> R3_DEVS =
            ImmutableSet.of(dstr("07"), dstr("08"), dstr("09"));
    private static final Set<DeviceId> ALT_DEVICES =
            ImmutableSet.of(dstr("0a"), dstr("0b"), dstr("0c"));

    private JsonNode data;
    private BasicRegionConfig cfg;

    @Before
    public void setUp() {
        data = getTestJson(REGION_JSON);
    }

    private JsonNode getR(String key) {
        return data.get("regions").get(key).get("basic");
    }

    // loads a region config from the test resource file
    private void loadRegion(String rid) {
        JsonNode node = getR(rid);
        print(JSON_LOADED, node);

        cfg = new BasicRegionConfig();
        cfg.init(regionId(rid), rid, node, mapper, delegate);
    }

    private void checkRegion(String expN, Region.Type expT, Set<DeviceId> expD) {
        print(CHECKING_S, cfg);
        assertEquals("wrong name", expN, cfg.name());
        assertEquals("wrong type", expT, cfg.type());

        List<DeviceId> devs = cfg.devices();
        if (expD == null) {
            assertNull("unexp device list", devs);
        } else {
            assertNotNull(devs);
            assertEquals("wr.size", expD.size(), devs.size());
            for (DeviceId d : expD) {
                assertTrue("missing dev: " + d, devs.contains(d));
            }
        }
    }

    @Test
    public void region1Config() {
        loadRegion(R1);
        checkRegion(EUROPE, Region.Type.CONTINENT, R1_DEVS);
    }

    @Test
    public void region2Config() {
        loadRegion(R2);
        checkRegion(PARIS, Region.Type.METRO, R2_DEVS);
    }

    @Test
    public void region3Config() {
        loadRegion(R3);
        checkRegion(R3, null, R3_DEVS);
    }

    @Test
    public void modifyName() {
        loadRegion(R1);
        cfg.name(AUSTRALIA);
        checkRegion(AUSTRALIA, Region.Type.CONTINENT, R1_DEVS);
    }

    @Test
    public void clearName() {
        loadRegion(R1);
        checkRegion(EUROPE, Region.Type.CONTINENT, R1_DEVS);
        cfg.name(null);
        // if the friendly name is cleared, name() returns the identifier
        checkRegion(R1, Region.Type.CONTINENT, R1_DEVS);
    }

    @Test
    public void modifyType() {
        loadRegion(R2);
        cfg.type(Region.Type.CAMPUS);
        checkRegion(PARIS, Region.Type.CAMPUS, R2_DEVS);
    }

    @Test
    public void clearType() {
        loadRegion(R2);
        cfg.type(null);
        checkRegion(PARIS, null, R2_DEVS);
    }

    @Test
    public void modifyDevices() {
        loadRegion(R3);
        cfg.devices(ALT_DEVICES);
        checkRegion(R3, null, ALT_DEVICES);
    }

    @Test
    public void clearDevices() {
        loadRegion(R3);
        cfg.devices(null);
        checkRegion(R3, null, null);
    }


    @Test
    public void sampleValidConfig() {
        ObjectNode node = new TmpJson()
                .props(NAME, TYPE)
                .arrays(DEVICES)
                .node();
        cfg = new BasicRegionConfig();
        cfg.init(regionId(R1), BASIC, node, mapper, delegate);

        assertTrue("not valid: " + cfg, cfg.isValid());
    }

    @Test(expected = InvalidFieldException.class)
    public void sampleInvalidConfig() {
        ObjectNode node = new TmpJson()
                .props(NAME, TYPE, "foo")
                .arrays(DEVICES)
                .node();
        cfg = new BasicRegionConfig();
        cfg.init(regionId(R1), BASIC, node, mapper, delegate);

        cfg.isValid();
    }
}
