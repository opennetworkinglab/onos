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
package org.onosproject.cfm.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.InternetDomainName;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * Test that the MaintenanceDomainCodec can successfully parse Json in to a MaintenanceDomain.
 */
public class MaintenanceDomainCodecTest {
    private static final MdId MDID1_CHAR = MdIdCharStr.asMdId("test-1");
    private static final MdId MDID2_DOMAIN = MdIdDomainName.asMdId(
                        InternetDomainName.from("test.opennetworking.org"));
    private static final MdId MDID3_MACUINT =
            MdIdMacUint.asMdId(MacAddress.valueOf("aa:bb:cc:dd:ee:ff"), 181);
    private static final MdId MDID4_NONE = MdIdNone.asMdId();

    private ObjectMapper mapper;
    private CfmCodecContext context;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testDecodeMd1() throws IOException {
        String mdString = "{\"md\": {    \"mdName\": \"test-1\"," +
                "\"mdNameType\": \"CHARACTERSTRING\"," +
                "\"mdLevel\": \"LEVEL1\", \"mdNumericId\": 1}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceDomain mdDecode1 = context
                .codec(MaintenanceDomain.class).decode((ObjectNode) cfg, context);
        assertEquals(MDID1_CHAR, mdDecode1.mdId());
        assertEquals(MaintenanceDomain.MdLevel.LEVEL1, mdDecode1.mdLevel());
        assertEquals(1, mdDecode1.mdNumericId());
    }

    @Test
    public void testDecodeMd1NoTypeGiven() throws IOException {
        String mdString = "{\"md\": {    \"mdName\": \"test-1\"," +
                "\"mdLevel\": \"LEVEL1\", \"mdNumericId\": 1}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceDomain mdDecode1 = context
                .codec(MaintenanceDomain.class).decode((ObjectNode) cfg, context);
        assertEquals(MDID1_CHAR, mdDecode1.mdId());
        assertEquals(MaintenanceDomain.MdLevel.LEVEL1, mdDecode1.mdLevel());
        assertEquals(1, mdDecode1.mdNumericId());
    }


    @Test
    public void testDecodeMd2() throws IOException {
        String mdString = "{\"md\": {    \"mdName\": \"test.opennetworking.org\"," +
                "\"mdNameType\": \"DOMAINNAME\"}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceDomain mdDecode1 = context
                .codec(MaintenanceDomain.class).decode((ObjectNode) cfg, context);
        assertEquals(MDID2_DOMAIN, mdDecode1.mdId());
        assertEquals(MaintenanceDomain.MdLevel.LEVEL0, mdDecode1.mdLevel());
        assertEquals(0, mdDecode1.mdNumericId());
    }

    @Test
    public void testDecodeMd3() throws IOException {
        String mdString = "{\"md\": {    \"mdName\": \"aa:bb:cc:dd:ee:ff:181\"," +
                "\"mdNameType\": \"MACANDUINT\"}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceDomain mdDecode1 = context
                .codec(MaintenanceDomain.class).decode((ObjectNode) cfg, context);
        assertEquals(MDID3_MACUINT, mdDecode1.mdId());
    }

    @Test
    public void testDecodeMd4() throws IOException {
        String mdString = "{\"md\": {    \"mdName\": \"\"," +
                "\"mdNameType\": \"NONE\"}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceDomain mdDecode1 = context
                .codec(MaintenanceDomain.class).decode((ObjectNode) cfg, context);
        assertEquals(MDID4_NONE, mdDecode1.mdId());
    }

    @Test
    public void testEncodeMd1() throws CfmConfigException {
        MaintenanceDomain md1 = DefaultMaintenanceDomain.builder(MDID1_CHAR)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL1)
                .mdNumericId((short) 1)
                .build();

        ObjectNode node = mapper.createObjectNode();
        node.set("md", context.codec(MaintenanceDomain.class).encode(md1, context));

        assertEquals("{\"md\":{" +
                "\"mdName\":\"test-1\"," +
                "\"mdNameType\":\"CHARACTERSTRING\"," +
                "\"mdLevel\":\"LEVEL1\"," +
                "\"mdNumericId\":1," +
                "\"maList\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMd2() throws CfmConfigException {
        MaintenanceDomain md2 = DefaultMaintenanceDomain.builder(MDID2_DOMAIN)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL2).build();

        ObjectNode node = mapper.createObjectNode();
        node.set("md", context.codec(MaintenanceDomain.class).encode(md2, context));

        assertEquals("{\"md\":{" +
                "\"mdName\":\"test.opennetworking.org\"," +
                "\"mdNameType\":\"DOMAINNAME\"," +
                "\"mdLevel\":\"LEVEL2\"," +
                "\"maList\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMd3() throws CfmConfigException {
        MaintenanceDomain md3 = DefaultMaintenanceDomain.builder(MDID3_MACUINT)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL3).build();

        ObjectNode node = mapper.createObjectNode();
        node.set("md", context.codec(MaintenanceDomain.class).encode(md3, context));

        assertEquals("{\"md\":{" +
                "\"mdName\":\"AA:BB:CC:DD:EE:FF:181\"," +
                "\"mdNameType\":\"MACANDUINT\"," +
                "\"mdLevel\":\"LEVEL3\"," +
                "\"maList\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMd4() throws CfmConfigException {
        MaintenanceDomain md4 = DefaultMaintenanceDomain.builder(MDID4_NONE)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL4).build();

        ObjectNode node = mapper.createObjectNode();
        node.set("md", context.codec(MaintenanceDomain.class).encode(md4, context));

        assertEquals("{\"md\":{" +
                "\"mdName\":\"\"," +
                "\"mdNameType\":\"NONE\"," +
                "\"mdLevel\":\"LEVEL4\"," +
                "\"maList\":[]}}", node.toString());
    }
}
