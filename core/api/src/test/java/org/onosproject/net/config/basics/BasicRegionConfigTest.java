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
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.region.RegionId.regionId;

/**
 * Test class for {@link BasicRegionConfig}.
 */
public class BasicRegionConfigTest extends AbstractConfigTest {

    private static final String REGION_JSON = "configs.regions.1.json";
    private static final String R1 = "r1";
    private static final String R2 = "r2";
    private static final String R3 = "r3";

    private static final Set<DeviceId> R1_DEVS =
            ImmutableSet.of(dstr("01"), dstr("02"), dstr("03"));
    private static final Set<DeviceId> R2_DEVS =
            ImmutableSet.of(dstr("04"), dstr("05"), dstr("06"));
    private static final Set<DeviceId> R3_DEVS =
            ImmutableSet.of(dstr("07"), dstr("08"), dstr("09"));


    private JsonNode data;

    @Before
    public void setUp() {
        data = getTestJson(REGION_JSON);
    }

    private JsonNode getR(String key) {
        return data.get("regions").get(key).get("basic");
    }

    private void checkRegion(String rid, Region.Type expT, Set<DeviceId> expD) {
        JsonNode r1json = getR(rid);
        print(r1json);

        BasicRegionConfig brc = new BasicRegionConfig();
        brc.init(regionId(rid), rid, r1json, mapper, delegate);

        Region.Type type = brc.getType();
        assertEquals("wrong type", expT, type);

        List<DeviceId> devs = brc.getDevices();
        assertEquals("wr.size", expD.size(), devs.size());
        for (DeviceId d : expD) {
            assertTrue("missing dev: " + d, devs.contains(d));
        }
    }

    @Test
    public void region1Config() {
        checkRegion(R1, Region.Type.CONTINENT, R1_DEVS);
    }

    @Test
    public void region2Config() {
        checkRegion(R2, Region.Type.METRO, R2_DEVS);
    }

    @Test
    public void region3Config() {
        checkRegion(R3, null, R3_DEVS);
    }

}
