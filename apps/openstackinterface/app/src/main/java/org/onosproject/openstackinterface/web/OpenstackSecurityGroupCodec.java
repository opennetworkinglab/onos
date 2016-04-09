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
package org.onosproject.openstackinterface.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackinterface.OpenstackSecurityGroup;
import org.onosproject.openstackinterface.OpenstackSecurityGroupRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Encodes and decodes the Openstack Security Group.
 */
public class OpenstackSecurityGroupCodec extends JsonCodec<OpenstackSecurityGroup> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String SECURITY_GROUP = "security_group";
    private static final String DESCRIPTION = "description";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String SECURITY_GROUP_RULES = "security_group_rules";
    private static final String DIRECTION = "direction";
    private static final String EHTERTYPE = "ethertype";
    private static final String PORT_RANGE_MAX = "port_range_max";
    private static final String PORT_RANGE_MIN = "port_range_min";
    private static final String PROTOCOL = "protocol";
    private static final String REMOTE_GROUP_ID = "remote_group_id";
    private static final String REMOTE_IP_PREFIX = "remote_ip_prefix";
    private static final String SECURITY_GROUP_ID = "security_group_id";
    private static final String TENANT_ID = "tenant_id";

    @Override
    public OpenstackSecurityGroup decode(ObjectNode json, CodecContext context) {
        JsonNode securityGroupNode = json.get(SECURITY_GROUP);
        if (securityGroupNode == null) {
            log.warn("SecurityGroup Json data is null");
            return null;
        }

        String description = securityGroupNode.path(DESCRIPTION).asText();
        String id = securityGroupNode.path(ID).asText();
        String name = securityGroupNode.path(NAME).asText();
        ArrayNode ruleInfoList = (ArrayNode) securityGroupNode.path(SECURITY_GROUP_RULES);
        Collection<OpenstackSecurityGroupRule> rules = Lists.newArrayList();
        for (JsonNode ruleInfo: ruleInfoList) {
            OpenstackSecurityGroupRule openstackSecurityGroupRule =
                    new OpenstackSecurityGroupRule.Builder()
                        .direction(ruleInfo.path(DIRECTION).asText())
                        .etherType(ruleInfo.path(EHTERTYPE).asText())
                        .id(ruleInfo.path(ID).asText())
                        .portRangeMax(ruleInfo.path(PORT_RANGE_MAX).asText())
                        .portRangeMin(ruleInfo.path(PORT_RANGE_MIN).asText())
                        .protocol(ruleInfo.path(PROTOCOL).asText())
                        .remoteGroupId(ruleInfo.path(REMOTE_GROUP_ID).asText())
                        .remoteIpPrefix(ruleInfo.path(REMOTE_IP_PREFIX).asText())
                        .securityGroupId(ruleInfo.path(SECURITY_GROUP_ID).asText())
                        .tenantId(ruleInfo.path(TENANT_ID).asText())
                        .build();

            rules.add(openstackSecurityGroupRule);
        }
        String tenantId = securityGroupNode.path(TENANT_ID).asText();

        OpenstackSecurityGroup openstackSecurityGroup = OpenstackSecurityGroup.builder()
                .description(description)
                .id(id)
                .name(name)
                .rules(rules)
                .tenantId(tenantId)
                .build();

        return openstackSecurityGroup;
    }
}
