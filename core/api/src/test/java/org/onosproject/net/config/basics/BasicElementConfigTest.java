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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.config.basics.BasicElementConfig.ZERO_THRESHOLD;

/**
 * Unit tests for {@link BasicElementConfig}.
 */
public class BasicElementConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String E1 = "e1";

    // concrete subclass of abstract class we are testing
    private static class ElmCfg extends BasicElementConfig<String> {
        ElmCfg() {
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
}
