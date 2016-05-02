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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.codec.JsonCodec;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for role info codec.
 */
public final class RoleInfoCodecTest {

    MockCodecContext context;
    JsonCodec<RoleInfo> roleInfoCodec;

    @Before
    public void setUp() {
        context = new MockCodecContext();
        roleInfoCodec = context.codec(RoleInfo.class);
        assertThat(roleInfoCodec, notNullValue());
    }

    /**
     * Tests encoding of a role info object.
     */
    @Test
    public void testRoleInfoEncode() {
        NodeId masterNodeId = NodeId.nodeId("1");
        NodeId backupNodeId1 = NodeId.nodeId("1");
        NodeId backupNodeId2 = NodeId.nodeId("2");
        NodeId backupNodeId3 = NodeId.nodeId("3");
        List<NodeId> backupNodeIds =
                ImmutableList.of(backupNodeId1, backupNodeId2, backupNodeId3);

        RoleInfo roleInfo = new RoleInfo(masterNodeId, backupNodeIds);
        ObjectNode roleInfoJson = roleInfoCodec.encode(roleInfo, context);
        assertThat(roleInfoJson, RoleInfoJsonMatcher.matchesRoleInfo(roleInfo));
    }

    /**
     * Tests decoding of a role info JSON object.
     */
    @Test
    public void testRoleInfoDecode() throws IOException {
        RoleInfo roleInfo = getRoleInfo("RoleInfo.json");

        assertThat(roleInfo.backups().size(), is(3));

        assertThat(roleInfo.master().id(), is("1"));

        List<NodeId> backups = roleInfo.backups();
        assertThat(backups.contains(NodeId.nodeId("2")), is(true));
        assertThat(backups.contains(NodeId.nodeId("3")), is(true));
        assertThat(backups.contains(NodeId.nodeId("4")), is(true));
    }

    /**
     * Hamcrest matcher for role info.
     */
    private static final class RoleInfoJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final RoleInfo roleInfo;

        private RoleInfoJsonMatcher(RoleInfo roleInfo) {
            this.roleInfo = roleInfo;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check master node identifier
            String jsonNodeId = jsonNode.get("master") != null ? jsonNode.get("master").asText() : null;
            String nodeId = roleInfo.master().id();
            if (!StringUtils.equals(jsonNodeId, nodeId)) {
                description.appendText("master's node id was " + jsonNodeId);
                return false;
            }

            // check backup nodes size
            JsonNode jsonBackupNodeIds = jsonNode.get("backups");
            if (jsonBackupNodeIds.size() != roleInfo.backups().size()) {
                description.appendText("backup nodes size was " + jsonBackupNodeIds.size());
                return false;
            }


            // check backup nodes' identifier
            for (NodeId backupNodeId : roleInfo.backups()) {
                boolean backupFound = false;
                for (int idx = 0; idx < jsonBackupNodeIds.size(); idx++) {
                    if (backupNodeId.id().equals(jsonBackupNodeIds.get(idx).asText())) {
                        backupFound = true;
                        break;
                    }
                }
                if (!backupFound) {
                    description.appendText("backup not found " + backupNodeId.id());
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(roleInfo.toString());
        }

        /**
         * Factory to allocate a role info.
         *
         * @param roleInfo role info object we are looking for
         * @return matcher
         */
        static RoleInfoJsonMatcher matchesRoleInfo(RoleInfo roleInfo) {
            return new RoleInfoJsonMatcher(roleInfo);
        }
    }

    /**
     * Reads in a role info from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded roleInfo
     * @throws IOException if processing the resource fails
     */
    private RoleInfo getRoleInfo(String resourceName) throws IOException {
        InputStream jsonStream = RoleInfoCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        RoleInfo roleInfo = roleInfoCodec.decode((ObjectNode) json, context);
        assertThat(roleInfo, notNullValue());
        return roleInfo;
    }
}
