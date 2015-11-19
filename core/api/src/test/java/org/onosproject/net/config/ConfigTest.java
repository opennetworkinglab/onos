/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.net.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

/**
 * Test of the base network config class.
 */
public class ConfigTest {

    private static final String SUBJECT = "subject";
    private static final String KEY = "key";

    private static final String TEXT = "text";
    private static final String LONG = "long";
    private static final String DOUBLE = "double";
    private static final String MAC = "mac";
    private static final String IP = "ip";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigApplyDelegate delegate = new TestDelegate();

    private Config<String> cfg;
    private JsonNode json;

    @Before
    public void setUp() {
        json = new ObjectMapper().createObjectNode()
                .put(TEXT, "foo").put(LONG, 5).put(DOUBLE, 0.5)
                .put(MAC, "ab:cd:ef:ca:fe:ed").put(IP, "12.34.56.78");
        cfg = new TestConfig();
        cfg.init(SUBJECT, KEY, json, mapper, delegate);
    }

    @Test
    public void hasOnlyFields() {
        assertTrue("has unexpected fields", cfg.hasOnlyFields(TEXT, LONG, DOUBLE, MAC, IP));
        assertFalse("did not detect unexpected fields", cfg.hasOnlyFields(TEXT, LONG, DOUBLE, MAC));
        assertTrue("is not proper text", cfg.isString(TEXT, MANDATORY));
    }

    @Test
    public void isString() {
        assertTrue("is not proper text", cfg.isString(TEXT, MANDATORY));
        assertTrue("is not proper text", cfg.isString(TEXT, MANDATORY, "^f.*"));
        assertTrue("is not proper text", cfg.isString(TEXT, OPTIONAL, "^f.*"));
        assertTrue("is not proper text", cfg.isString(TEXT, OPTIONAL));
        assertTrue("is not proper text", cfg.isString("none", OPTIONAL));
        assertFalse("did not detect missing field", cfg.isString("none", MANDATORY));
    }

    @Test
    public void isNumber() {
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY));
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY, 0));
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY, 0, 10));
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY, 5, 6));
        assertFalse("is not in range", cfg.isNumber(LONG, MANDATORY, 6, 10));
        assertFalse("is not in range", cfg.isNumber(LONG, MANDATORY, 4, 5));
        assertTrue("is not proper number", cfg.isNumber(LONG, OPTIONAL, 0, 10));
        assertTrue("is not proper number", cfg.isNumber(LONG, OPTIONAL));
        assertTrue("is not proper number", cfg.isNumber("none", OPTIONAL));
        assertFalse("did not detect missing field", cfg.isNumber("none", MANDATORY));
    }

    @Test
    public void isDecimal() {
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.0));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.0, 1.0));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.5, 0.6));
        assertFalse("is not in range", cfg.isDecimal(DOUBLE, MANDATORY, 0.6, 1.0));
        assertFalse("is not in range", cfg.isDecimal(DOUBLE, MANDATORY, 0.4, 0.5));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, OPTIONAL, 0.0, 1.0));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, OPTIONAL));
        assertTrue("is not proper decimal", cfg.isDecimal("none", OPTIONAL));
        assertFalse("did not detect missing field", cfg.isDecimal("none", MANDATORY));
    }

    @Test
    public void isMacAddress() {
        assertTrue("is not proper mac", cfg.isMacAddress(MAC, MANDATORY));
        assertTrue("is not proper mac", cfg.isMacAddress(MAC, OPTIONAL));
        assertTrue("is not proper mac", cfg.isMacAddress("none", OPTIONAL));
        assertFalse("did not detect missing field", cfg.isMacAddress("none", MANDATORY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badMacAddress() {
        assertTrue("is not proper mac", cfg.isMacAddress(TEXT, MANDATORY));
    }


    @Test
    public void isIpAddress() {
        assertTrue("is not proper ip", cfg.isIpAddress(IP, MANDATORY));
        assertTrue("is not proper ip", cfg.isIpAddress(IP, OPTIONAL));
        assertTrue("is not proper ip", cfg.isIpAddress("none", OPTIONAL));
        assertFalse("did not detect missing field", cfg.isMacAddress("none", MANDATORY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badIpAddress() {
        assertTrue("is not proper ip", cfg.isIpAddress(TEXT, MANDATORY));
    }


    // TODO: Add tests for other helper methods

    private class TestConfig extends Config<String> {
    }

    private class TestDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}