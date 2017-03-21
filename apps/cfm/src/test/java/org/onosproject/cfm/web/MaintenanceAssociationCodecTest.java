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
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * Test that the MaintenanceAssociationCodec can successfully parse Json in to a MaintenanceAssociation.
 */
public class MaintenanceAssociationCodecTest {
    private static final MaIdShort MAID1_CHAR = MaIdCharStr.asMaId("ma-1");
    private static final MaIdShort MAID2_VID = MaIdPrimaryVid.asMaId(1234);
    private static final MaIdShort MAID3_OCTET = MaId2Octet.asMaId(12467);
    private static final MaIdShort MAID4_RFC = MaIdRfc2685VpnId.asMaIdHex("aa:bb:cc:dd:ee:ff:99");
    private static final MaIdShort MAID5_Y1731 = MaIdIccY1731.asMaId("abc", "defghij");


    private ObjectMapper mapper;
    private CfmCodecContext context;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        mapper = new ObjectMapper();
        context = new CfmCodecContext();
    }

    @Test
    public void testDecodeMa1() throws IOException {
        String mdString = "{\"ma\": {    \"maName\": \"ma-1\"," +
                "\"maNameType\": \"CHARACTERSTRING\"," +
                "\"component-list\": [], " +
                "\"rmep-list\": [], " +
                "\"maNumericId\": 1}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceAssociation maDecode1 = ((MaintenanceAssociationCodec) context
                .codec(MaintenanceAssociation.class))
                .decode((ObjectNode) cfg, context, 10);
        assertEquals(MAID1_CHAR, maDecode1.maId());
        assertEquals(1, maDecode1.maNumericId());
    }

    @Test
    public void testDecodeMa1NoTypeGiven() throws IOException {
        String mdString = "{\"ma\": {    \"maName\": \"ma-1\"," +
                "\"component-list\": [], " +
                "\"rmep-list\": [], " +
                "\"maNumericId\": 1}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceAssociation maDecode1 = ((MaintenanceAssociationCodec) context
                .codec(MaintenanceAssociation.class))
                .decode((ObjectNode) cfg, context, 10);
        assertEquals(MAID1_CHAR, maDecode1.maId());
        assertEquals(1, maDecode1.maNumericId());
    }

    @Test
    public void testDecodeMa2() throws IOException {
        String mdString = "{\"ma\": {    \"maName\": 1234," +
                "\"maNameType\": \"PRIMARYVID\"," +
                "\"component-list\": [], " +
                "\"rmep-list\": [], " +
                "\"maNumericId\": 2}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceAssociation maDecode2 = ((MaintenanceAssociationCodec) context
                .codec(MaintenanceAssociation.class))
                .decode((ObjectNode) cfg, context, 10);
        assertEquals(MAID2_VID, maDecode2.maId());
    }

    @Test
    public void testDecodeMa3() throws IOException {
        String mdString = "{\"ma\": {    \"maName\": 12467," +
                "\"maNameType\": \"TWOOCTET\"," +
                "\"component-list\": [], " +
                "\"rmep-list\": [], " +
                "\"maNumericId\": 3}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceAssociation maDecode3 = ((MaintenanceAssociationCodec) context
                .codec(MaintenanceAssociation.class))
                .decode((ObjectNode) cfg, context, 10);
        assertEquals(MAID3_OCTET, maDecode3.maId());
    }

    @Test
    public void testDecodeMa4() throws IOException {
        String mdString = "{\"ma\": {    \"maName\": \"aa:bb:cc:dd:ee:ff:99\"," +
                "\"maNameType\": \"RFC2685VPNID\"," +
                "\"component-list\": [], " +
                "\"rmep-list\": [], " +
                "\"maNumericId\": 4}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceAssociation maDecode4 = ((MaintenanceAssociationCodec) context
                .codec(MaintenanceAssociation.class))
                .decode((ObjectNode) cfg, context, 10);
        assertEquals(MAID4_RFC, maDecode4.maId());
    }

    @Test
    public void testDecodeMa5() throws IOException {
        String mdString = "{\"ma\": {    \"maName\": \"abc:defghij\"," +
                "\"maNameType\": \"ICCY1731\"," +
                "\"component-list\": [], " +
                "\"rmep-list\": [], " +
                "\"maNumericId\": 5}}";

        InputStream input = new ByteArrayInputStream(
                mdString.getBytes(StandardCharsets.UTF_8));
        JsonNode cfg = mapper.readTree(input);
        MaintenanceAssociation maDecode5 = ((MaintenanceAssociationCodec) context
                .codec(MaintenanceAssociation.class))
                .decode((ObjectNode) cfg, context, 10);
        assertEquals(MAID5_Y1731, maDecode5.maId());
    }

    @Test
    public void testEncodeMa1() throws CfmConfigException {
        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation.builder(MAID1_CHAR, 10)
                .maNumericId((short) 1)
                .build();

        ObjectNode node = mapper.createObjectNode();
        node.set("ma", context.codec(MaintenanceAssociation.class).encode(ma1, context));

        assertEquals("{\"ma\":{" +
                "\"maName\":\"ma-1\"," +
                "\"maNameType\":\"CHARACTERSTRING\"," +
                "\"maNumericId\":1," +
                "\"component-list\":[]," +
                "\"rmep-list\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMa2() throws CfmConfigException {
        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation.builder(MAID2_VID, 10)
                .maNumericId((short) 2)
                .build();

        ObjectNode node = mapper.createObjectNode();
        node.set("ma", context.codec(MaintenanceAssociation.class).encode(ma1, context));

        assertEquals("{\"ma\":{" +
                "\"maName\":\"1234\"," +
                "\"maNameType\":\"PRIMARYVID\"," +
                "\"maNumericId\":2," +
                "\"component-list\":[]," +
                "\"rmep-list\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMa3() throws CfmConfigException {
        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation.builder(MAID3_OCTET, 10)
                .maNumericId((short) 3)
                .build();

        ObjectNode node = mapper.createObjectNode();
        node.set("ma", context.codec(MaintenanceAssociation.class).encode(ma1, context));

        assertEquals("{\"ma\":{" +
                "\"maName\":\"12467\"," +
                "\"maNameType\":\"TWOOCTET\"," +
                "\"maNumericId\":3," +
                "\"component-list\":[]," +
                "\"rmep-list\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMa4() throws CfmConfigException {
        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation.builder(MAID4_RFC, 10)
                .maNumericId((short) 4)
                .build();

        ObjectNode node = mapper.createObjectNode();
        node.set("ma", context.codec(MaintenanceAssociation.class).encode(ma1, context));

        assertEquals("{\"ma\":{" +
                "\"maName\":\"aa:bb:cc:dd:ee:ff:99\"," +
                "\"maNameType\":\"RFC2685VPNID\"," +
                "\"maNumericId\":4," +
                "\"component-list\":[]," +
                "\"rmep-list\":[]}}", node.toString());
    }

    @Test
    public void testEncodeMa5() throws CfmConfigException {
        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation.builder(MAID5_Y1731, 10)
                .maNumericId((short) 5)
                .build();

        ObjectNode node = mapper.createObjectNode();
        node.set("ma", context.codec(MaintenanceAssociation.class).encode(ma1, context));

        assertEquals("{\"ma\":{" +
                "\"maName\":\"abc:defghij\"," +
                "\"maNameType\":\"ICCY1731\"," +
                "\"maNumericId\":5," +
                "\"component-list\":[]," +
                "\"rmep-list\":[]}}", node.toString());
    }
}
