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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link BlockedPortsConfig}.
 */
public class BlockedPortsConfigTest {

    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "foo");
    private static final String KEY = "blocked";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEV1 = "of:0000000000000001";
    private static final String DEV2 = "of:0000000000000002";
    private static final String DEV3 = "of:0000000000000003";
    private static final String DEV4 = "of:0000000000000004";
    private static final String RANGE_14 = "1-4";
    private static final String RANGE_79 = "7-9";
    private static final String P1 = "1";
    private static final String P5 = "5";
    private static final String P9 = "9";

    private BlockedPortsConfig cfg;
    private BlockedPortsConfig.Range range;

    private void print(String s) {
        System.out.println(s);
    }

    private void print(Object o) {
        print(o.toString());
    }

    @Before
    public void setUp() throws IOException {
        InputStream blockedPortsJson = BlockedPortsConfigTest.class
                .getResourceAsStream("/blocked-ports.json");
        JsonNode node = MAPPER.readTree(blockedPortsJson);
        cfg = new BlockedPortsConfig();
        cfg.init(APP_ID, KEY, node, MAPPER, null);
    }

    @Test
    public void basic() {
        cfg = new BlockedPortsConfig();
        print(cfg);

        assertEquals("non-empty devices list", 0, cfg.deviceIds().size());
        assertEquals("non-empty port-ranges list", 0, cfg.portRanges("non-exist").size());
    }


    @Test
    public void overIteratePort() {
        Iterator<Long> iterator = cfg.portIterator(DEV3);
        while (iterator.hasNext()) {
            print(iterator.next());
        }

        try {
            print(iterator.next());
            fail("NoSuchElement exception NOT thrown");
        } catch (NoSuchElementException e) {
            print("<good> " + e);
        }
    }

    @Test
    public void overIterateRange() {
        range = new BlockedPortsConfig.Range("4-6");

        Iterator<Long> iterator = range.iterator();
        while (iterator.hasNext()) {
            print(iterator.next());
        }

        try {
            print(iterator.next());
            fail("NoSuchElement exception NOT thrown");
        } catch (NoSuchElementException e) {
            print("<good> " + e);
        }
    }


    @Test
    public void simple() {
        List<String> devIds = cfg.deviceIds();
        print(devIds);
        assertEquals("wrong dev id count", 3, devIds.size());
        assertEquals("missing dev 1", true, devIds.contains(DEV1));
        assertEquals("dev 2??", false, devIds.contains(DEV2));
        assertEquals("missing dev 3", true, devIds.contains(DEV3));

        List<String> d1ranges = cfg.portRanges(DEV1);
        print(d1ranges);
        assertEquals("wrong d1 range count", 2, d1ranges.size());
        assertEquals("missing 1-4", true, d1ranges.contains(RANGE_14));
        assertEquals("missing 7-9", true, d1ranges.contains(RANGE_79));

        List<String> d2ranges = cfg.portRanges(DEV2);
        print(d2ranges);
        assertEquals("wrong d2 range count", 0, d2ranges.size());

        List<String> d3ranges = cfg.portRanges(DEV3);
        print(d3ranges);
        assertEquals("wrong d3 range count", 1, d3ranges.size());
        assertEquals("range 1-4?", false, d3ranges.contains(RANGE_14));
        assertEquals("missing 7-9", true, d3ranges.contains(RANGE_79));
    }


    private void verifyPorts(List<Long> ports, long... exp) {
        assertEquals("Wrong port count", exp.length, ports.size());
        for (long e : exp) {
            assertEquals("missing port", true, ports.contains(e));
        }
    }

    private void verifyPortIterator(String devid, long... exp) {
        List<Long> ports = new ArrayList<>();
        Iterator<Long> iter = cfg.portIterator(devid);
        iter.forEachRemaining(ports::add);
        print(ports);
        verifyPorts(ports, exp);
    }

    @Test
    public void rangeIterators() {
        verifyPortIterator(DEV1, 1, 2, 3, 4, 7, 8, 9);
        verifyPortIterator(DEV2);
        verifyPortIterator(DEV3, 7, 8, 9);
    }

    @Test
    public void singlePorts() {
        List<String> devIds = cfg.deviceIds();
        print(devIds);
        assertEquals("wrong dev id count", 3, devIds.size());
        assertEquals("missing dev 4", true, devIds.contains(DEV4));

        List<String> d1ranges = cfg.portRanges(DEV4);
        print(d1ranges);
        assertEquals("wrong d4 range count", 3, d1ranges.size());
        assertEquals("missing 1", true, d1ranges.contains(P1));
        assertEquals("missing 5", true, d1ranges.contains(P5));
        assertEquals("missing 9", true, d1ranges.contains(P9));

        verifyPortIterator(DEV4, 1, 5, 9);
    }


    // test Range inner class

    @Test
    public void rangeBadFormat() {
        try {
            range = new BlockedPortsConfig.Range("not-a-range-format");
            fail("no exception thrown");
        } catch (IllegalArgumentException iar) {
            print(iar);
            assertEquals("wrong msg", "Bad Range Format not-a-range-format", iar.getMessage());
        }
    }

    @Test
    public void rangeBadHi() {
        try {
            range = new BlockedPortsConfig.Range("2-nine");
            fail("no exception thrown");
        } catch (IllegalArgumentException iar) {
            print(iar);
            assertEquals("wrong msg", "Bad Range Format 2-nine", iar.getMessage());
        }
    }

    @Test
    public void rangeHiLessThanLo() {
        try {
            range = new BlockedPortsConfig.Range("9-5");
            fail("no exception thrown");
        } catch (IllegalArgumentException iar) {
            print(iar);
            assertEquals("wrong msg", "Bad Range Format 9-5", iar.getMessage());
        }
    }

    @Test
    public void rangeNegative() {
        try {
            range = new BlockedPortsConfig.Range("-2-4");
            fail("no exception thrown");
        } catch (IllegalArgumentException iar) {
            print(iar);
            assertEquals("wrong msg", "Bad Range Format -2-4", iar.getMessage());
        }
    }

    @Test
    public void rangeGood() {
        range = new BlockedPortsConfig.Range("100-104");
        List<Long> values = new ArrayList<>();
        range.iterator().forEachRemaining(values::add);
        print(values);
        verifyPorts(values, 100, 101, 102, 103, 104);
    }
}
