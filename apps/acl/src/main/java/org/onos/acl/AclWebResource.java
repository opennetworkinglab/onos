/*
 * Copyright 2015 Open Networking Laboratory
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li and Heng Qi
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
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
package org.onos.acl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IPv4;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * REST resource for interacting with ACL application.
 */
@Path("")
public class AclWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Processes user's GET HTTP request for querying ACL rules.
     * @return response to the request
     */
    @GET
    public Response queryAclRule() {
        List<AclRule> rules = get(AclService.class).getAclRules();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (AclRule rule : rules) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", rule.id().toString());
            if (rule.srcIp() != null) {
                node.put("srcIp", rule.srcIp().toString());
            }
            if (rule.dstIp() != null) {
                node.put("dstIp", rule.dstIp().toString());
            }
            if (rule.ipProto() != 0) {
                switch (rule.ipProto()) {
                    case IPv4.PROTOCOL_ICMP:
                        node.put("ipProto", "ICMP");
                        break;
                    case IPv4.PROTOCOL_TCP:
                        node.put("ipProto", "TCP");
                        break;
                    case IPv4.PROTOCOL_UDP:
                        node.put("ipProto", "UDP");
                        break;
                    default:
                        break;
                }
            }
            if (rule.dstTpPort() != 0) {
                node.put("dstTpPort", rule.dstTpPort());
            }
            node.put("action", rule.action().toString());
            arrayNode.add(node);
        }
        root.set("ACL rules", arrayNode);
        return Response.ok(root.toString(), MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Processes user's POST HTTP request for add ACL rules.
     * @param stream input stream
     * @return response to the request
     */
    @POST
    @Path("add")
    public Response addAclRule(InputStream stream) {
        AclRule newRule;
        try {
            newRule = jsonToRule(stream);
        } catch (Exception e) {
            return Response.ok("{\"status\" : \"Failed! " + e.getMessage() + "\"}").build();
        }

        String status;
        if (get(AclService.class).addAclRule(newRule)) {
            status = "Success! New ACL rule is added.";
        } else {
            status = "Failed! New ACL rule matches an existing rule.";
        }
        return Response.ok("{\"status\" : \"" + status + "\"}").build();
    }

    /**
     * Processes user's GET HTTP request for removing ACL rule.
     * @param id ACL rule id (in hex string format)
     * @return response to the request
     */
    @GET
    @Path("remove/{id}")
    public Response removeAclRule(@PathParam("id") String id) {
        String status;
        RuleId ruleId = new RuleId(Long.parseLong(id.substring(2), 16));
        if (get(AclStore.class).getAclRule(ruleId) == null) {
            status = "Failed! There is no ACL rule with this id.";
        } else {
            get(AclService.class).removeAclRule(ruleId);
            status = "Success! ACL rule(id:" + id + ") is removed.";
        }
        return Response.ok("{\"status\" : \"" + status + "\"}").build();
    }

    /**
     * Processes user's GET HTTP request for clearing ACL.
     * @return response to the request
     */
    @GET
    @Path("clear")
    public Response clearACL() {
        get(AclService.class).clearAcl();
        return Response.ok("{\"status\" : \"ACL is cleared.\"}").build();
    }

    /**
     * Exception class for parsing a invalid ACL rule.
     */
    private class AclRuleParseException extends Exception {
        public AclRuleParseException(String message) {
            super(message);
        }
    }

    /**
     * Turns a JSON string into an ACL rule instance.
     */
    private AclRule jsonToRule(InputStream stream) throws AclRuleParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(stream);
        JsonParser jp = jsonNode.traverse();
        AclRule.Builder rule = AclRule.builder();
        jp.nextToken();
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new AclRuleParseException("Expected START_OBJECT");
        }

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                throw new AclRuleParseException("Expected FIELD_NAME");
            }

            String key = jp.getCurrentName();
            jp.nextToken();
            String value = jp.getText();
            if ("".equals(value)) {
                continue;
            }

            if ("srcIp".equals(key)) {
                rule.srcIp(value);
            } else if ("dstIp".equals(key)) {
                rule.dstIp(value);
            } else if ("ipProto".equals(key)) {
                if ("TCP".equalsIgnoreCase(value)) {
                    rule.ipProto(IPv4.PROTOCOL_TCP);
                } else if ("UDP".equalsIgnoreCase(value)) {
                    rule.ipProto(IPv4.PROTOCOL_UDP);
                } else if ("ICMP".equalsIgnoreCase(value)) {
                    rule.ipProto(IPv4.PROTOCOL_ICMP);
                } else {
                    throw new AclRuleParseException("ipProto must be assigned to TCP, UDP, or ICMP.");
                }
            } else if ("dstTpPort".equals(key)) {
                try {
                    rule.dstTpPort(Short.parseShort(value));
                } catch (NumberFormatException e) {
                    throw new AclRuleParseException("dstTpPort must be assigned to a numerical value.");
                }
            } else if ("action".equals(key)) {
                if (!"allow".equalsIgnoreCase(value) && !"deny".equalsIgnoreCase(value)) {
                    throw new AclRuleParseException("action must be assigned to ALLOW or DENY.");
                }
                if ("allow".equalsIgnoreCase(value)) {
                    rule.action(AclRule.Action.ALLOW);
                }
            }
        }
        return rule.build();
    }

}