/*
 * Copyright 2016-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.onosproject.net.config.basics.BasicElementConfig.ZERO_THRESHOLD;

/**
 * Unit tests for {@link BasicElementConfig}.
 */
public class BasicElementConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String E1 = "e1";
    private static final String GEO = BasicElementConfig.LOC_TYPE_GEO;
    private static final String GRID = BasicElementConfig.LOC_TYPE_GRID;
    public static final ImmutableSet<String> ROLES = ImmutableSet.of("spine", "primary");

    // concrete subclass of abstract class we are testing
    private static class ElmCfg extends BasicElementConfig<String> {
        ElmCfg() {
            mapper = MAPPER;
            object = MAPPER.createObjectNode();
        }

        @Override
        public String toString() {
            return object.toString();
        }
    }

    private static void print(String fmt, Object... args) {
        System.out.println(String.format(fmt, args));
    }

    private static void print(Object o) {
        print("%s", o);
    }

    private BasicElementConfig<?> cfg;

    @Before
    public void setUp() {
        cfg = new ElmCfg().name(E1);
    }

    @Test
    public void basicNoGeo() {
        print(cfg);
        assertFalse("geo set?", cfg.geoCoordsSet());
        assertEquals("lat", 0.0, cfg.latitude(), ZERO_THRESHOLD);
        assertEquals("lon", 0.0, cfg.longitude(), ZERO_THRESHOLD);
    }

    @Test
    public void geoLatitudeOnly() {
        cfg.latitude(0.1);
        print(cfg);
        assertTrue("geo NOT set", cfg.geoCoordsSet());
        assertEquals("lat", 0.1, cfg.latitude(), ZERO_THRESHOLD);
        assertEquals("lon", 0.0, cfg.longitude(), ZERO_THRESHOLD);
    }

    @Test
    public void geoLongitudeOnly() {
        cfg.longitude(-0.1);
        print(cfg);
        assertTrue("geo NOT set", cfg.geoCoordsSet());
        assertEquals("lat", 0.0, cfg.latitude(), ZERO_THRESHOLD);
        assertEquals("lon", -0.1, cfg.longitude(), ZERO_THRESHOLD);
    }

    @Test
    public void geoLatLong() {
        cfg.latitude(3.1415).longitude(2.71828);
        print(cfg);
        assertTrue("geo NOT set", cfg.geoCoordsSet());
        assertEquals("lat", 3.1415, cfg.latitude(), ZERO_THRESHOLD);
        assertEquals("lon", 2.71828, cfg.longitude(), ZERO_THRESHOLD);
    }

    @Test
    public void uiType() {
        print(cfg);
        assertEquals("not default type", null, cfg.uiType());
        cfg.uiType("someOtherType");
        print(cfg);
        assertEquals("not other type", "someOtherType", cfg.uiType());
    }

    @Test
    public void defaultGridCoords() {
        print(cfg);
        assertFalse("grid not origin?", cfg.gridCoordsSet());
        assertEquals("gridx", 0.0, cfg.gridX(), ZERO_THRESHOLD);
        assertEquals("gridy", 0.0, cfg.gridY(), ZERO_THRESHOLD);
    }

    @Test
    public void someGridCoords() {
        cfg.gridX(35.0).gridY(49.7).locType(GRID);
        print(cfg);
        assertTrue("grid at origin?", cfg.gridCoordsSet());
        assertEquals("gridx", 35.0, cfg.gridX(), ZERO_THRESHOLD);
        assertEquals("gridy", 49.7, cfg.gridY(), ZERO_THRESHOLD);
    }

    @Test
    public void defaultLocationType() {
        print(cfg);
        assertEquals("not none", BasicElementConfig.LOC_TYPE_NONE, cfg.locType());
    }

    @Test
    public void geoLocationType() {
        cfg.locType(GEO);
        print(cfg);
        assertEquals("not geo", GEO, cfg.locType());
    }

    @Test
    public void gridLocationType() {
        cfg.locType(GRID);
        print(cfg);
        assertEquals("not grid", GRID, cfg.locType());
    }

    @Test
    public void otherLocationType() {
        cfg.locType("foobar");
        print(cfg);
        assertEquals("not none", BasicElementConfig.LOC_TYPE_NONE, cfg.locType());
    }

    @Test
    public void roles() {
        cfg.roles(ROLES);
        print(cfg);
        assertEquals("not roles", ROLES, cfg.roles());
    }
}
