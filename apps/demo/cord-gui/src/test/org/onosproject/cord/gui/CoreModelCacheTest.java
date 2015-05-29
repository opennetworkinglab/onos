/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.cord.gui.model.BundleFactory;
import org.onosproject.cord.gui.model.SubscriberUser;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CordModelCache}.
 */
@Ignore("How to test against a live XOS system??")
public class CoreModelCacheTest {

    private CordModelCache cache;

    @Before
    public void setUp() {
        cache = new CordModelCache();
    }

    @Test
    public void basic() {
        assertEquals("wrong bundle", BundleFactory.BASIC_BUNDLE,
                     cache.getCurrentBundle().descriptor());
    }

    @Test
    public void basicBundleJson() {
        ObjectNode node = BundleFactory.toObjectNode(cache.getCurrentBundle());
        String json = node.toString();
        System.out.println(json);
        assertTrue("bad basic json", sameJson(BASIC_BUNDLE_JSON, json));
    }

    @Test
    public void chooseFamilyBundle() {
        cache.setCurrentBundle("family");
        assertEquals("wrong bundle", BundleFactory.FAMILY_BUNDLE,
                     cache.getCurrentBundle().descriptor());
    }

    @Test
    public void familyBundleJson() {
        cache.setCurrentBundle("family");
        ObjectNode node = BundleFactory.toObjectNode(cache.getCurrentBundle());
        String json = node.toString();
        System.out.println(json);
        assertTrue("bad family json", sameJson(FAMILY_BUNDLE_JSON, json));
    }

    @Test
    public void checkUsers() {
        List<SubscriberUser> users = cache.getUsers();
        assertEquals("wrong # users", 4, users.size());
    }

    @Test
    public void usersBasicJson() {
        String json = cache.jsonUsers();
        System.out.println(json);
        assertTrue("bad users basic json", sameJson(USERS_BASIC, json));
    }

    @Test
    public void usersFamilyJson() {
        cache.setCurrentBundle("family");
        String json = cache.jsonUsers();
        System.out.println(json);
        assertTrue("bad users family json", sameJson(USERS_FAMILY, json));
    }

    @Test
    public void setNewLevel() {
        cache.setCurrentBundle("family");
        JsonNode node = fromString(cache.jsonUsers());
        assertEquals("wrong level", "G", getMomsLevel(node));

        cache.applyPerUserParam("1", "url_filter", "level", "R");

        node = fromString(cache.jsonUsers());
        assertEquals("wrong level", "R", getMomsLevel(node));
    }

    private String getMomsLevel(JsonNode node) {
        JsonNode mom = node.get("users").elements().next();
        assertEquals("wrong ID", 1, mom.get("id").asInt());
        return mom.get("profile").get("url_filter").get("level").asText();
    }


    // =============

    private JsonNode fromString(String s) {
        try {
            return MAPPER.readTree(s);
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }
        return null;
    }

    private boolean sameJson(String s1, String s2) {
        try {
            JsonNode tree1 = MAPPER.readTree(s1);
            JsonNode tree2 = MAPPER.readTree(s2);
            return tree1.equals(tree2);
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }
        return false;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String BASIC_BUNDLE_JSON = "{\n" +
            "    \"bundle\": {\n" +
            "        \"id\": \"basic\",\n" +
            "        \"name\": \"Basic Bundle\",\n" +
            "        \"desc\": \"Provides basic internet and firewall functions.\",\n" +
            "        \"functions\": [\n" +
            "            {\n" +
            "                \"id\": \"internet\",\n" +
            "                \"name\": \"Internet\",\n" +
            "                \"desc\": \"Basic internet connectivity.\",\n" +
            "                \"params\": {}\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"firewall\",\n" +
            "                \"name\": \"Firewall\",\n" +
            "                \"desc\": \"Normal firewall protection.\",\n" +
            "                \"params\": {}\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    \"bundles\": [\n" +
            "        {\n" +
            "            \"id\": \"basic\",\n" +
            "            \"name\": \"Basic Bundle\",\n" +
            "            \"desc\": \"Provides basic internet and firewall functions.\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"family\",\n" +
            "            \"name\": \"Family Bundle\",\n" +
            "            \"desc\": \"Provides internet, firewall and parental control functions.\"\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";

    private static final String FAMILY_BUNDLE_JSON = "{\n" +
            "    \"bundle\": {\n" +
            "        \"id\": \"family\",\n" +
            "        \"name\": \"Family Bundle\",\n" +
            "        \"desc\": \"Provides internet, firewall and parental control functions.\",\n" +
            "        \"functions\": [\n" +
            "            {\n" +
            "                \"id\": \"internet\",\n" +
            "                \"name\": \"Internet\",\n" +
            "                \"desc\": \"Basic internet connectivity.\",\n" +
            "                \"params\": {}\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"firewall\",\n" +
            "                \"name\": \"Firewall\",\n" +
            "                \"desc\": \"Normal firewall protection.\",\n" +
            "                \"params\": {}\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"url_filter\",\n" +
            "                \"name\": \"Parental Control\",\n" +
            "                \"desc\": \"Variable levels of URL filtering.\",\n" +
            "                \"params\": {\n" +
            "                    \"level\": \"G\",\n" +
            "                    \"levels\": [\n" +
            "                        \"OFF\",\n" +
            "                        \"G\",\n" +
            "                        \"PG\",\n" +
            "                        \"PG_13\",\n" +
            "                        \"R\",\n" +
            "                        \"NONE\"\n" +
            "                    ]\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    \"bundles\": [\n" +
            "        {\n" +
            "            \"id\": \"basic\",\n" +
            "            \"name\": \"Basic Bundle\",\n" +
            "            \"desc\": \"Provides basic internet and firewall functions.\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"family\",\n" +
            "            \"name\": \"Family Bundle\",\n" +
            "            \"desc\": \"Provides internet, firewall and parental control functions.\"\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";

    private static final String USERS_BASIC = "{\n" +
            "  \"users\": [\n" +
            "    {\n" +
            "      \"id\": 1,\n" +
            "      \"name\": \"Mom's MacBook\",\n" +
            "      \"mac\": \"010203040506\",\n" +
            "      \"profile\": { }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 2,\n" +
            "      \"name\": \"Dad's iPad\",\n" +
            "      \"mac\": \"010203040507\",\n" +
            "      \"profile\": { }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 3,\n" +
            "      \"name\": \"Dick's laptop\",\n" +
            "      \"mac\": \"010203040508\",\n" +
            "      \"profile\": { }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 4,\n" +
            "      \"name\": \"Jane's laptop\",\n" +
            "      \"mac\": \"010203040509\",\n" +
            "      \"profile\": { }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private static final String USERS_FAMILY = "{\n" +
            "  \"users\": [\n" +
            "    {\n" +
            "      \"id\": 1,\n" +
            "      \"name\": \"Mom's MacBook\",\n" +
            "      \"mac\": \"010203040506\",\n" +
            "      \"profile\": {\n" +
            "        \"url_filter\": {\n" +
            "          \"level\": \"G\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 2,\n" +
            "      \"name\": \"Dad's iPad\",\n" +
            "      \"mac\": \"010203040507\",\n" +
            "      \"profile\": {\n" +
            "        \"url_filter\": {\n" +
            "          \"level\": \"G\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 3,\n" +
            "      \"name\": \"Dick's laptop\",\n" +
            "      \"mac\": \"010203040508\",\n" +
            "      \"profile\": {\n" +
            "        \"url_filter\": {\n" +
            "          \"level\": \"G\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 4,\n" +
            "      \"name\": \"Jane's laptop\",\n" +
            "      \"mac\": \"010203040509\",\n" +
            "      \"profile\": {\n" +
            "        \"url_filter\": {\n" +
            "          \"level\": \"G\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
}
