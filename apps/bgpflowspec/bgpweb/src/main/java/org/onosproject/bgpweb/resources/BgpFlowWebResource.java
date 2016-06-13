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
package org.onosproject.bgpweb.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.flow.manager.BgpFlowService;
import org.onosproject.flowapi.DefaultExtWideCommunityInt;
import org.onosproject.flowapi.ExtFlowContainer;
import org.onosproject.flowapi.ExtFlowTypes;
import org.onosproject.flowapi.ExtTrafficAction;
import org.onosproject.flowapi.ExtWideCommunityInt;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Bgp flow web resource.
 */

@Path("flow")
public class BgpFlowWebResource extends AbstractWebResource {

    private BgpFlowService flowService = get(BgpFlowService.class);
    protected static final String BGPFLOW = "bgp_flow";
    protected static final String DEVICEID = "deviceId";
    protected static final int LOCAL_PERF = 20;
    private final Logger log = LoggerFactory.getLogger(BgpFlowWebResource.class);

    /**
     * Get the Bgp flow.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBgpFlow() {
        Boolean result = true;
        //TODO
        return ok(result.toString()).build();
    }

    /**
     * Push the the Bgp flow spec.
     *
     * @param stream bgp flow spec in JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBgpFlow(InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode flow = jsonTree.get(BGPFLOW);
            String devId = jsonTree.get(DEVICEID).asText();
            boolean status;

            List<ExtFlowTypes> list;
            ExtFlowContainer container;

            list = codec(ExtFlowTypes.class).decode((ArrayNode) flow, this);

            if (!validateRpd(list)) {
                return Response.status(NOT_ACCEPTABLE).entity(Boolean.FALSE.toString()).build();
            }

            container = new ExtFlowContainer(list);
            container.setDeviceId(devId);


            status = flowService.onBgpFlowCreated(container);
            Boolean isSuccess = Boolean.valueOf(status);
            if (!status) {
                return Response.status(NOT_ACCEPTABLE).entity(isSuccess.toString()).build();
            }

            return Response.status(OK).entity(isSuccess.toString()).build();
        } catch (IOException e) {
            log.error("Exception while parsing bgp flow.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Validates the rpd and flow spec classes.
     *
     * @param list list of types
     * @return a true if the validation succeeds else false
     */
    boolean validateRpd(List<ExtFlowTypes> list) {
        ListIterator<ExtFlowTypes> iterator = list.listIterator();
        ExtFlowTypes flow;
        boolean key = false;
        boolean rpd = false;
        boolean nonRpd = false;
        ExtTrafficAction flowAction = null;
        boolean dstPfx = false;
        boolean wcFlg = false;
        boolean wcHop = false;
        boolean wcWc = false;
        boolean wcCAs = false;
        boolean wcLAs = false;
        boolean wcTarget = false;
        boolean wcETarget = false;
        boolean wcParm = false;

        while (iterator.hasNext()) {
            flow = iterator.next();
            switch (flow.type()) {
                case EXT_FLOW_RULE_KEY:
                    key = true;
                    break;
                case IPV4_DST_PFX:
                    dstPfx = true;
                    break;
                case IPV4_SRC_PFX:
                    nonRpd = true;
                    break;
                case IP_PROTO_LIST:
                    nonRpd = true;
                    break;
                case IN_PORT_LIST:
                    nonRpd = true;
                    break;
                case DST_PORT_LIST:
                    nonRpd = true;
                    break;
                case SRC_PORT_LIST:
                    nonRpd = true;
                    break;
                case ICMP_TYPE_LIST:
                    nonRpd = true;
                    break;
                case ICMP_CODE_LIST:
                    nonRpd = true;
                    break;
                case TCP_FLAG_LIST:
                    nonRpd = true;
                    break;
                case PACKET_LENGTH_LIST:
                    nonRpd = true;
                    break;
                case DSCP_VALUE_LIST:
                    nonRpd = true;
                    break;
                case FRAGMENT_LIST:
                    nonRpd = true;
                    break;
                case TRAFFIC_ACTION:
                    flowAction = (ExtTrafficAction) flow;
                    if (flowAction.rpd()) {
                        rpd = true;
                    }
                    break;
                case TRAFFIC_RATE:
                    nonRpd = true;
                    break;
                case TRAFFIC_REDIRECT:
                    nonRpd = true;
                    break;
                case TRAFFIC_MARKING:
                    nonRpd = true;
                    break;
                case WIDE_COMM_FLAGS:
                    wcFlg = true;
                    break;
                case WIDE_COMM_HOP_COUNT:
                    wcHop = true;
                    break;
                case WIDE_COMM_COMMUNITY:
                    wcWc = true;
                    break;
                case WIDE_COMM_CONTEXT_AS:
                    wcCAs = true;
                    break;
                case WIDE_COMM_LOCAL_AS:
                    wcLAs = true;
                    break;
                case WIDE_COMM_TARGET:
                    wcTarget = true;
                    break;
                case WIDE_COMM_EXT_TARGET:
                    wcETarget = true;
                    break;
                case WIDE_COMM_PARAMETER:
                    wcParm = true;
                    break;
                default:
                    log.error("error: this type is not supported");
                    break;
            }
        }

        if (!key) {
            return false;
        }

        /** checking for non Rpd. */
        if (!rpd) {
            if (wcFlg || wcHop || wcWc
                    || wcCAs || wcLAs || wcParm) {
                return false;
            }
        }

        /** checking for Rpd. */
        if (nonRpd || !dstPfx || !wcFlg || !wcHop
                || !wcWc || !wcCAs || !wcLAs || !wcParm) {
            return false;
        }

        /** If it is rpd then either of these two or both should be present.*/
        if (!wcTarget && !wcETarget) {
            rpd = false;
        }

        if (!handleRpdLocalPerf(list)) {
            return false;
        }

        return rpd;
    }

    /**
     * Validate and format the rpd local perf.
     *
     * @param list list of types
     * @return a true if the validation succeeds else false
     */
    boolean handleRpdLocalPerf(List<ExtFlowTypes> list) {
        ListIterator<ExtFlowTypes> iterator = list.listIterator();
        ExtFlowTypes flow;

        ExtWideCommunityInt wcComm = null;
        ExtWideCommunityInt wcParam = null;
        ListIterator<Integer> wcInt = null;
        int community = 0;
        int param = 0;

        while (iterator.hasNext()) {
            flow = iterator.next();
            switch (flow.type()) {
                case WIDE_COMM_COMMUNITY:
                    wcComm = (ExtWideCommunityInt) flow;
                    wcInt = wcComm.communityInt().listIterator();
                    community = wcInt.next().intValue();
                    break;

                case WIDE_COMM_PARAMETER:
                    wcParam = (ExtWideCommunityInt) flow;
                    wcInt = wcParam.communityInt().listIterator();
                    param = wcInt.next().intValue();
                    break;
                default:
                    log.error("error: this type is not supported");
                    break;
            }
        }

        if (community == LOCAL_PERF) {
            if (param > 127 || param < -127) {
                return false;
            }

            /** if -ve then make it 1 bye value and set it, if it is positive then no issue.*/
            if (param < 0) {
                param = ~param + 129;
                list.remove(wcParam);
                ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();
                resultBuilder.setwCommInt(Integer.valueOf(param));
                resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_PARAMETER);
                list.add(resultBuilder.build());
            }
        }

        return true;
    }

    /**
     * Delete the the Bgp flow spec.
     *
     * @param stream bgp flow spec in JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBgpFlow(InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode flow = jsonTree.get(BGPFLOW);
            String devId = jsonTree.get(DEVICEID).asText();
            boolean status;

            List<ExtFlowTypes> list;
            ExtFlowContainer container;

            list = codec(ExtFlowTypes.class).decode((ArrayNode) flow, this);

            if (!validateRpd(list)) {
                return Response.status(NOT_ACCEPTABLE).entity(Boolean.FALSE.toString()).build();
            }

            container = new ExtFlowContainer(list);
            container.setDeviceId(devId);


            status = flowService.onBgpFlowDeleted(container);
            Boolean isSuccess = Boolean.valueOf(status);
            if (!status) {
                return Response.status(NOT_ACCEPTABLE).entity(isSuccess.toString()).build();
            }
            return Response.status(OK).entity(isSuccess.toString()).build();
        } catch (IOException e) {
            log.error("Exception while parsing bgp flow.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }
}
