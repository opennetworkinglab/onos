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
import org.junit.Before;
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
        String json = BundleFactory.toJson(cache.getCurrentBundle());
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
        String json = BundleFactory.toJson(cache.getCurrentBundle());
        System.out.println(json);
        assertTrue("bad family json", sameJson(FAMILY_BUNDLE_JSON, json));
    }

    @Test
    public void checkUsers() {
        List<SubscriberUser> users = cache.getUsers();
        assertEquals("wrong # users", 4, users.size());
    }

    // =============

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
            "  \"bundle\": {\n" +
            "    \"id\": \"basic\",\n" +
            "    \"name\": \"Basic Bundle\",\n" +
            "    \"functions\": [\n" +
            "      {\n" +
            "        \"id\": \"internet\",\n" +
            "        \"name\": \"Internet\",\n" +
            "        \"desc\": \"Basic internet connectivity.\",\n" +
            "        \"params\": {}\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": \"firewall\",\n" +
            "        \"name\": \"Firewall\",\n" +
            "        \"desc\": \"Normal firewall protection.\",\n" +
            "        \"params\": {}\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"bundles\": [\n" +
            "    { \"id\": \"basic\", \"name\": \"Basic Bundle\" },\n" +
            "    { \"id\": \"family\", \"name\": \"Family Bundle\" }\n" +
            "  ]\n" +
            "}\n";

    private static final String FAMILY_BUNDLE_JSON = "{\n" +
            "  \"bundle\": {\n" +
            "    \"id\": \"family\",\n" +
            "    \"name\": \"Family Bundle\",\n" +
            "    \"functions\": [\n" +
            "      {\n" +
            "        \"id\": \"internet\",\n" +
            "        \"name\": \"Internet\",\n" +
            "        \"desc\": \"Basic internet connectivity.\",\n" +
            "        \"params\": {}\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": \"firewall\",\n" +
            "        \"name\": \"Firewall\",\n" +
            "        \"desc\": \"Normal firewall protection.\",\n" +
            "        \"params\": {}\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": \"url_filter\",\n" +
            "        \"name\": \"Parental Control\",\n" +
            "        \"desc\": \"Variable levels of URL filtering.\",\n" +
            "        \"params\": {\n" +
            "          \"level\": \"PG\",\n" +
            "          \"levels\": [ \"PG\", \"PG-13\", \"R\" ]\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"bundles\": [\n" +
            "    { \"id\": \"basic\", \"name\": \"Basic Bundle\" },\n" +
            "    { \"id\": \"family\", \"name\": \"Family Bundle\" }\n" +
            "  ]\n" +
            "}\n";
}
