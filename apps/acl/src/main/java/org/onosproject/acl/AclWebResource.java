/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li, Heng Qi and Haisheng Yu
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */
package org.onosproject.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Manage ACL rules.
 */
@Path("rules")
public class AclWebResource extends AbstractWebResource {

    /**
     * Get all ACL rules.
     * Returns array of all ACL rules.
     *
     * @return 200 OK
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
        root.set("aclRules", arrayNode);
        return Response.ok(root.toString(), MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Add a new ACL rule.
     *
     * @param stream JSON data describing the rule
     * @return 200 OK
     * @throws URISyntaxException uri syntax exception
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAclRule(InputStream stream) throws URISyntaxException {
        AclRule newRule = jsonToRule(stream);
        return get(AclService.class).addAclRule(newRule) ?
                Response.created(new URI(newRule.id().toString())).build() :
                Response.serverError().build();
    }

    /**
     * Remove ACL rule.
     *
     * @param id ACL rule id (in hex string format)
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{id}")
    public Response removeAclRule(@PathParam("id") String id) {
        RuleId ruleId = new RuleId(Long.parseLong(id.substring(2), 16));
        get(AclService.class).removeAclRule(ruleId);
        return Response.noContent().build();
    }

    /**
     * Remove all ACL rules.
     *
     * @return 204 NO CONTENT
     */
    @DELETE
    public Response clearAcl() {
        get(AclService.class).clearAcl();
        return Response.noContent().build();
    }

    /**
     * Turns a JSON string into an ACL rule instance.
     */
    private AclRule jsonToRule(InputStream stream) {
        JsonNode node;
        try {
            node = mapper().readTree(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse ACL request", e);
        }

        AclRule.Builder rule = AclRule.builder();

        String s = node.path("srcIp").asText(null);
        if (s != null) {
            rule.srcIp(Ip4Prefix.valueOf(s));
        }

        s = node.path("dstIp").asText(null);
        if (s != null) {
            rule.dstIp(Ip4Prefix.valueOf(s));
        }

        s = node.path("ipProto").asText(null);
        if (s != null) {
            if ("TCP".equalsIgnoreCase(s)) {
                rule.ipProto(IPv4.PROTOCOL_TCP);
            } else if ("UDP".equalsIgnoreCase(s)) {
                rule.ipProto(IPv4.PROTOCOL_UDP);
            } else if ("ICMP".equalsIgnoreCase(s)) {
                rule.ipProto(IPv4.PROTOCOL_ICMP);
            } else {
                throw new IllegalArgumentException("ipProto must be assigned to TCP, UDP, or ICMP");
            }
        }

        int port = node.path("dstTpPort").asInt(0);
        if (port > 0) {
            rule.dstTpPort((short) port);
        }

        s = node.path("action").asText(null);
        if (s != null) {
            if ("allow".equalsIgnoreCase(s)) {
                rule.action(AclRule.Action.ALLOW);
            } else if ("deny".equalsIgnoreCase(s)) {
                rule.action(AclRule.Action.DENY);
            } else {
                throw new IllegalArgumentException("action must be ALLOW or DENY");
            }
        }

        return rule.build();
    }

}
