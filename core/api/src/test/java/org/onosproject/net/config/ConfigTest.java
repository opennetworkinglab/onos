/*
 * Copyright 2015-present Open Networking Foundation
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

import static junit.framework.TestCase.assertFalse;
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
    private static final String BOOLEAN = "boolean";
    private static final String MAC = "mac";
    private static final String BAD_MAC = "badMac";
    private static final String IP = "ip";
    private static final String BAD_IP = "badIp";
    private static final String PREFIX = "prefix";
    private static final String BAD_PREFIX = "badPrefix";
    private static final String CONNECT_POINT = "connectPoint";
    private static final String BAD_CONNECT_POINT = "badConnectPoint";
    private static final String TP_PORT = "tpPort";
    private static final String BAD_TP_PORT = "badTpPort";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigApplyDelegate delegate = new TestDelegate();

    private Config<String> cfg;
    private JsonNode json;

    @Before
    public void setUp() {
        json = new ObjectMapper().createObjectNode()
                .put(TEXT, "foo").put(LONG, 5).put(DOUBLE, 0.5)
                .put(BOOLEAN, "true")
                .put(MAC, "ab:cd:ef:ca:fe:ed").put(BAD_MAC, "ab:cd:ef:ca:fe.ed")
                .put(IP, "12.34.56.78").put(BAD_IP, "12.34-56.78")
                .put(PREFIX, "12.34.56.78/18").put(BAD_PREFIX, "12.34.56.78-18")
                .put(CONNECT_POINT, "of:0000000000000001/1")
                .put(BAD_CONNECT_POINT, "of:0000000000000001-1")
                .put(TP_PORT, 65535).put(BAD_TP_PORT, 65536);
        cfg = new TestConfig();
        cfg.init(SUBJECT, KEY, json, mapper, delegate);
    }

    @Test
    public void hasField() {
        assertTrue("missing field", cfg.hasField(MAC));
        assertFalse("unexpected field", cfg.hasField("non-existent"));
    }

    @Test
    public void hasOnlyFields() {
        assertTrue("has unexpected fields",
                cfg.hasOnlyFields(TEXT, LONG, DOUBLE, BOOLEAN, MAC, BAD_MAC,
                        IP, BAD_IP, PREFIX, BAD_PREFIX,
                        CONNECT_POINT, BAD_CONNECT_POINT, TP_PORT, BAD_TP_PORT));
        assertTrue("did not detect unexpected fields",
                expectInvalidField(() -> cfg.hasOnlyFields(TEXT, LONG, DOUBLE, MAC)));
    }

    @Test
    public void hasFields() {
        assertTrue("does not have mandatory field",
                cfg.hasFields(TEXT, LONG, DOUBLE, MAC));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.hasFields("none")));
    }

    @Test
    public void isString() {
        assertTrue("is not proper text", cfg.isString(TEXT, MANDATORY));
        assertTrue("is not proper text", cfg.isString(TEXT, MANDATORY, "^f.*"));
        assertTrue("is not proper text", cfg.isString(TEXT, OPTIONAL, "^f.*"));
        assertTrue("is not proper text", cfg.isString(TEXT, OPTIONAL));
        assertTrue("is not proper text", cfg.isString("none", OPTIONAL));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isString("none", MANDATORY)));
        assertTrue("did not detect bad text",
                expectInvalidField(() -> cfg.isString(TEXT, OPTIONAL, "^b.*")));
    }

    @Test
    public void isNumber() {
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY));
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY, 0));
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY, 0, 10));
        assertTrue("is not proper number", cfg.isNumber(LONG, MANDATORY, 5, 6));
        assertTrue("is not in range",
                expectInvalidField(() -> cfg.isNumber(LONG, MANDATORY, 6, 10)));
        assertTrue("is not in range", cfg.isNumber(LONG, MANDATORY, 4, 5));
        assertTrue("is not proper number", cfg.isNumber(LONG, OPTIONAL, 0, 10));
        assertTrue("is not proper number", cfg.isNumber(LONG, OPTIONAL));
        assertTrue("is not proper number", cfg.isNumber("none", OPTIONAL));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isNumber("none", MANDATORY)));
        assertTrue("is not proper number",
                expectInvalidField(() -> cfg.isNumber(TEXT, MANDATORY)));

        assertTrue("is not proper number", cfg.isNumber(DOUBLE, MANDATORY, 0, 1));
        assertTrue("is not in range",
                expectInvalidField(() -> cfg.isNumber(DOUBLE, MANDATORY, 1, 2)));
    }

    @Test
    public void isIntegralNumber() {
        assertTrue("is not proper number", cfg.isIntegralNumber(LONG, MANDATORY));
        assertTrue("is not proper number", cfg.isIntegralNumber(LONG, MANDATORY, 0));
        assertTrue("is not proper number", cfg.isIntegralNumber(LONG, MANDATORY, 0, 10));
        assertTrue("is not proper number", cfg.isIntegralNumber(LONG, MANDATORY, 5, 6));
        assertTrue("is not in range",
                expectInvalidField(() -> cfg.isIntegralNumber(LONG, MANDATORY, 6, 10)));
        assertTrue("is not in range", cfg.isIntegralNumber(LONG, MANDATORY, 4, 5));
        assertTrue("is not proper number", cfg.isIntegralNumber(LONG, OPTIONAL, 0, 10));
        assertTrue("is not proper number", cfg.isIntegralNumber(LONG, OPTIONAL));
        assertTrue("is not proper number", cfg.isIntegralNumber("none", OPTIONAL));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isIntegralNumber("none", MANDATORY)));
        assertTrue("is not proper number",
                expectInvalidField(() -> cfg.isIntegralNumber(TEXT, MANDATORY)));

        assertTrue("is not in range",
                expectInvalidField(() -> cfg.isIntegralNumber(DOUBLE, MANDATORY, 0, 10)));
    }

    @Test
    public void isDecimal() {
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.0));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.0, 1.0));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.5, 0.6));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, MANDATORY, 0.4, 0.5));
        assertTrue("is not in range",
                expectInvalidField(() -> cfg.isDecimal(DOUBLE, MANDATORY, 0.6, 1.0)));
        assertTrue("is not in range",
                expectInvalidField(() -> cfg.isDecimal(DOUBLE, MANDATORY, 0.3, 0.4)));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, OPTIONAL, 0.0, 1.0));
        assertTrue("is not proper decimal", cfg.isDecimal(DOUBLE, OPTIONAL));
        assertTrue("is not proper decimal", cfg.isDecimal("none", OPTIONAL));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isDecimal("none", MANDATORY)));
    }

    @Test
    public void isBoolean() {
        assertTrue("is not proper boolean", cfg.isBoolean(BOOLEAN, MANDATORY));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isBoolean("none", MANDATORY)));
        assertTrue("is not proper boolean", cfg.isBoolean("none", OPTIONAL));
        assertTrue("did not detect bad boolean",
                expectInvalidField(() -> cfg.isBoolean(TEXT, MANDATORY)));
    }

    @Test
    public void isMacAddress() {
        assertTrue("is not proper mac", cfg.isMacAddress(MAC, MANDATORY));
        assertTrue("is not proper mac", cfg.isMacAddress(MAC, OPTIONAL));
        assertTrue("is not proper mac", cfg.isMacAddress("none", OPTIONAL));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isMacAddress("none", MANDATORY)));
        assertTrue("did not detect bad ip",
                expectInvalidField(() -> cfg.isMacAddress(BAD_MAC, MANDATORY)));
    }

    @Test
    public void isIpAddress() {
        assertTrue("is not proper ip", cfg.isIpAddress(IP, MANDATORY));
        assertTrue("is not proper ip", cfg.isIpAddress(IP, OPTIONAL));
        assertTrue("is not proper ip", cfg.isIpAddress("none", OPTIONAL));
        assertTrue("did not detect missing ip",
                expectInvalidField(() -> cfg.isIpAddress("none", MANDATORY)));
        assertTrue("did not detect bad ip",
                expectInvalidField(() -> cfg.isIpAddress(BAD_IP, MANDATORY)));
    }

    @Test
    public void isIpPrefix() {
        assertTrue("is not proper prefix", cfg.isIpPrefix(PREFIX, MANDATORY));
        assertTrue("is not proper prefix", cfg.isIpPrefix(PREFIX, OPTIONAL));
        assertTrue("is not proper prefix", cfg.isIpPrefix("none", OPTIONAL));
        assertTrue("did not detect missing prefix",
                expectInvalidField(() -> cfg.isIpPrefix("none", MANDATORY)));
        assertTrue("did not detect bad prefix",
                expectInvalidField(() -> cfg.isIpPrefix(BAD_PREFIX, MANDATORY)));
    }

    @Test
    public void isConnectPoint() {
        assertTrue("is not proper connectPoint", cfg.isConnectPoint(CONNECT_POINT, MANDATORY));
        assertTrue("is not proper connectPoint", cfg.isConnectPoint(CONNECT_POINT, OPTIONAL));
        assertTrue("is not proper connectPoint", cfg.isConnectPoint("none", OPTIONAL));
        assertTrue("did not detect missing connectPoint",
                expectInvalidField(() -> cfg.isConnectPoint("none", MANDATORY)));
        assertTrue("did not detect bad connectPoint",
                expectInvalidField(() -> cfg.isConnectPoint(BAD_CONNECT_POINT, MANDATORY)));
    }

    @Test
    public void isTpPort() {
        assertTrue("is not proper transport port", cfg.isTpPort(TP_PORT, MANDATORY));
        assertTrue("is not proper transport port", cfg.isTpPort(TP_PORT, OPTIONAL));
        assertTrue("is not proper transport port", cfg.isTpPort("none", OPTIONAL));
        assertTrue("did not detect missing field",
                expectInvalidField(() -> cfg.isTpPort("none", MANDATORY)));
        assertTrue("is not proper transport port",
                expectInvalidField(() -> cfg.isTpPort(BAD_TP_PORT, MANDATORY)));
    }

    /**
     * Expects an InvalidFieldException to be thrown when the given runnable is
     * run.
     *
     * @param runnable runnable to run
     * @return true if an InvalidFieldException was thrown, otherwise false
     */
    private boolean expectInvalidField(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (InvalidFieldException e) {
            return true;
        }
    }

    private class TestConfig extends Config<String> {
    }

    private class TestDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}
