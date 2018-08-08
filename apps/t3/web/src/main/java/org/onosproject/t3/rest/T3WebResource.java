/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.t3.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.group.Group;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.t3.api.GroupsInDevice;
import org.onosproject.t3.api.StaticPacketTrace;
import org.onosproject.t3.api.TroubleshootService;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Trellis Troubleshooting Tool REST API.
 */
@Path("t3")
public class T3WebResource extends AbstractWebResource {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = getLogger(T3WebResource.class);

    /**
     * Returns the trace non verbose result for the given source and destination ips.
     *
     * @param srcHost source ip identifier
     * @param dstHost destination ip identifier
     * @param ethType ethernet type identifier
     * @return 200 OK with component properties of given component and variable
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("simple/{srcHost}/{dstHost}/{ethType}")
    public Response getT3Simple(@PathParam("srcHost") String srcHost, @PathParam("dstHost") String dstHost,
                                @PathParam("ethType") String ethType) {

        ObjectNode node;
        try {
            node = getT3JsonOutput(srcHost, dstHost, ethType, false);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
        return Response.status(200).entity(node).build();

    }

    /**
     * Returns the trace verbose result for the given source and destination ips.
     *
     * @param srcHost source ip identifier
     * @param dstHost destination ip identifier
     * @param ethType ethernet type identifier
     * @return 200 OK with component properties of given component and variable
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("simple/{srcHost}/{dstHost}/{ethType}/verbose")
    public Response getT3SimpleVerbose(@PathParam("srcHost") String srcHost, @PathParam("dstHost") String dstHost,
                                       @PathParam("ethType") String ethType) {

        ObjectNode node;
        try {
            node = getT3JsonOutput(srcHost, dstHost, ethType, true);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
        return Response.status(200).entity(node).build();
    }

    /**
     * Returns the mcast trace non verbose result.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("mcast")
    public Response getT3Mcast() {
        ObjectNode node = null;
        try {
            node = getT3McastJsonOutput(false);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
        return Response.status(200).entity(node).build();
    }

    /**
     * Returns the mcast trace verbose result.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("mcast/verbose")
    public Response getT3McastVerbose() {
        ObjectNode node;
        try {
            node = getT3McastJsonOutput(true);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
        return Response.status(200).entity(node).build();
    }

    /**
     * Returns trace verbose or non verbose json output for the given trace.
     *
     * @param verbose based on verbosity level
     * @return a json representing the trace.
     */
    private ObjectNode getT3McastJsonOutput(boolean verbose) {
        TroubleshootService troubleshootService = get(TroubleshootService.class);
        final ObjectNode nodeOutput = mapper.createObjectNode();
        nodeOutput.put("title", "Tracing all Multicast routes in the System");

        //Create the generator for the list of traces.
        List<Set<StaticPacketTrace>> generator = troubleshootService.getMulitcastTrace(VlanId.vlanId("None"));
        int totalTraces = 0;
        List<StaticPacketTrace> failedTraces = new ArrayList<>();
        StaticPacketTrace previousTrace = null;
        ArrayNode traceArray = mapper.createArrayNode();
        for (Set<StaticPacketTrace> traces : generator) {
            ObjectNode genNode = mapper.createObjectNode();
            totalTraces++;
            //Print also Route if possible or packet
            ArrayNode traceOutput = mapper.createArrayNode();
            if (verbose) {
                traces.forEach(trace -> {
                    ObjectNode traceObj = mapper.createObjectNode();
                    ArrayNode node = mapper.createArrayNode();
                    for (Criterion packet : trace.getInitialPacket().criteria()) {
                        node.add(packet.toString());
                    }
                    traceObj.set("input packet", node);
                    traceObj.set("trace", getTraceJson(trace, verbose));
                    traceOutput.add(traceObj);
                });
            } else {
                for (StaticPacketTrace trace : traces) {
                    ObjectNode traceObject = mapper.createObjectNode();
                    traceObject.set("trace", traceNode(previousTrace, trace));
                    if (previousTrace == null || !previousTrace.equals(trace)) {
                        previousTrace = trace;
                    }
                    traceObject.put("result", trace.isSuccess());
                    if (!trace.isSuccess()) {
                        traceObject.put("reason", trace.resultMessage());
                        failedTraces.add(trace);
                    }
                    traceOutput.add(traceObject);
                }
            }
            genNode.set("traces", traceOutput);
            traceArray.add(genNode);
        }
        nodeOutput.set("tracing packet", traceArray);

        if (!verbose) {
            if (failedTraces.size() != 0) {
                nodeOutput.put("failed traces", failedTraces.size());
            }
            previousTrace = null;
            for (StaticPacketTrace trace : failedTraces) {
                if (previousTrace == null || !previousTrace.equals(trace)) {
                    previousTrace = trace;
                }
                nodeOutput.set("trace", traceNode(previousTrace, trace));
                if (trace != null) {
                    nodeOutput.put("failure", trace.resultMessage());
                }
            }
            nodeOutput.put("total traces", totalTraces);
            nodeOutput.put("errors", failedTraces.size());
        }
        return nodeOutput;
    }

    /**
     * Returns verbose or non verbose json output for the given trace.      *
     *
     * @param previousTrace the trace
     * @param trace         based on verbosity level
     * @return a json representing the trace.
     */
    private ObjectNode traceNode(StaticPacketTrace previousTrace, StaticPacketTrace trace) {
        ObjectNode obj = mapper.createObjectNode();
        if (previousTrace == null || !previousTrace.equals(trace)) {
            previousTrace = trace;
            ConnectPoint initialConnectPoint = trace.getInitialConnectPoint();
            TrafficSelector initialPacket = trace.getInitialPacket();
            boolean isIPv4 = ((EthTypeCriterion) initialPacket.getCriterion(Criterion.Type.ETH_TYPE))
                    .ethType().equals(EthType.EtherType.IPV4.ethType()
                    );
            IpPrefix group = ((IPCriterion) (isIPv4 ? trace.getInitialPacket()
                    .getCriterion(Criterion.Type.IPV4_DST) : trace.getInitialPacket()
                    .getCriterion(Criterion.Type.IPV6_DST))).ip();
            obj.put("source", initialConnectPoint.toString());
            obj.put("group", group.toString());
        }
        ArrayNode nodePath = mapper.createArrayNode();
        for (List<ConnectPoint> listPaths : trace.getCompletePaths()) {
            nodePath.add(listPaths.get(listPaths.size() - 1).toString());
        }
        if (trace.getCompletePaths().size() > 1) {
            obj.set("sinks", nodePath);
        } else {
            obj.set("sink", nodePath);
        }
        return obj;
    }

    /**
     * Returns trace verbose or non verbose json output for the given trace.
     *
     * @param srcHost source ip identifier
     * @param dstHost destination ip identifier
     * @param ethType the ethernet Type
     * @param verbose based on verbosity level
     * @return a json representing the trace.
     */
    private ObjectNode getT3JsonOutput(String srcHost, String dstHost, String ethType, Boolean verbose) {
        TroubleshootService troubleshootService = get(TroubleshootService.class);
        final ObjectNode nodeOutput = mapper.createObjectNode();
        //Tracing between host ips
        ArrayNode ipList = mapper.createArrayNode();
        ipList.add(srcHost);
        ipList.add(dstHost);
        nodeOutput.set("hostIps ", ipList);
        EthType.EtherType type = EthType.EtherType.valueOf(ethType.toUpperCase());

        //Build the traces
        Set<StaticPacketTrace> traces = troubleshootService.trace(HostId.hostId(srcHost), HostId.hostId(dstHost), type);
        traces.forEach(trace -> {

            if (trace.getInitialPacket() != null) {
                ArrayNode node = mapper.createArrayNode();
                for (Criterion packet : trace.getInitialPacket().criteria()) {
                    node.add(packet.toString());
                }
                nodeOutput.set("input packet", node);
                nodeOutput.set("trace", getTraceJson(trace, verbose));
            } else {
                LOG.debug("cannot obtain trace between: ", srcHost, dstHost);
                nodeOutput.set("failed trace", ipList);
                nodeOutput.put("reason", trace.resultMessage());
            }
        });
        return nodeOutput;
    }

    /**
     * Returns verbose or non verbose json output for the given trace.      *
     *
     * @param trace     the trace
     * @param verbosity based on verbosity level
     * @return a json representing the trace.
     */
    public ObjectNode getTraceJson(StaticPacketTrace trace, boolean verbosity) {
        ObjectNode nodeOutput = mapper.createObjectNode();
        if (verbosity) {
            nodeOutput.set("trace", getTrace(trace, verbosity));
        } else {
            ArrayNode nodePath = mapper.createArrayNode();
            for (List<ConnectPoint> listPaths : trace.getCompletePaths()) {
                ArrayNode node = mapper.createArrayNode();
                for (ConnectPoint path : listPaths) {
                    node.add(path.toString());
                }
                nodePath.add(node);
            }
            nodeOutput.set("paths", nodePath);
        }

        nodeOutput.put("result", trace.resultMessage());
        return nodeOutput;
    }

    /**
     * Returns verbose json output for the given trace.      *
     *
     * @param trace the trace
     * @return a json representing the trace.
     */
    private ObjectNode getTrace(StaticPacketTrace trace, boolean verbose) {

        ObjectNode nodeOutput = mapper.createObjectNode();

        List<List<ConnectPoint>> paths = trace.getCompletePaths();
        ArrayNode nodePath = mapper.createArrayNode();
        for (List<ConnectPoint> path : paths) {
            ArrayNode pathNode = mapper.createArrayNode();
            for (ConnectPoint pathItr : path) {
                pathNode.add(pathItr.toString());
            }
            nodePath.add(pathNode);

            ConnectPoint previous = null;

            if (path.size() == 1) {
                ConnectPoint connectPoint = path.get(0);
                nodeOutput.put("device", connectPoint.deviceId().toString());
                nodeOutput.put("input", connectPoint.toString());
                nodeOutput.put("flowCount", trace.getFlowsForDevice(connectPoint.deviceId()).size());
                nodeOutput.set("flows", getFlowArray(trace, connectPoint, verbose));

                List<GroupsInDevice> groupsInDevice = trace.getGroupOuputs(connectPoint.deviceId());

                if (groupsInDevice != null) {
                    groupsInDevice.forEach(output -> {
                        nodeOutput.set("groups", getGroupObj(connectPoint, output, verbose));
                    });
                }

            } else {
                for (ConnectPoint connectPoint : path) {
                    if (previous == null || !previous.deviceId().equals(connectPoint.deviceId())) {
                        nodeOutput.put("device", connectPoint.deviceId().toString());
                        nodeOutput.put("input", connectPoint.toString());
                        nodeOutput.put("flows", trace.getFlowsForDevice(connectPoint.deviceId()).size());
                        nodeOutput.put("verbose", verbose);
                        nodeOutput.set("flows", getFlowArray(trace, connectPoint, verbose));
                    } else {
                        List<GroupsInDevice> groupsInDevice = trace.getGroupOuputs(connectPoint.deviceId());
                        if (groupsInDevice != null) {
                            groupsInDevice.forEach(output -> {
                                nodeOutput.set("groups", getGroupObj(connectPoint, output, verbose));
                            });

                        }

                    }
                    previous = connectPoint;
                }
            }
        }
        nodeOutput.set("path", nodePath);
        return nodeOutput;
    }

    //Return groups Object for a given trace and a specified level of verbosity
    private ObjectNode getGroupObj(ConnectPoint connectPoint, GroupsInDevice output, boolean verbose) {
        ArrayNode groupArray = mapper.createArrayNode();
        ObjectNode groupsObj = mapper.createObjectNode();
        if (output.getOutput().equals(connectPoint)) {
            output.getGroups().forEach(group -> {
                ObjectNode groups = mapper.createObjectNode();
                if (verbose) {
                    groups = codec(Group.class).encode(group, this);
                } else {
                    groups.put("groupId", group.id().toString());
                }
                groupArray.add(groups);
            });
            ArrayNode node = mapper.createArrayNode();
            for (Criterion packet : output.getFinalPacket().criteria()) {
                node.add(packet.toString());
            }
            groupsObj.set("outgoing packet", node);
        }
        groupsObj.set("groups", groupArray);
        return groupsObj;
    }

    //Return flows Object for a given trace and a specified level of verbosity
    private ArrayNode getFlowArray(StaticPacketTrace trace, ConnectPoint connectPoint, boolean verbose) {
        ArrayNode flowArray = mapper.createArrayNode();
        trace.getFlowsForDevice(connectPoint.deviceId()).forEach(f -> {
            ObjectNode flows = mapper.createObjectNode();
            if (verbose) {
                flows = codec(FlowEntry.class).encode(f, this);
            } else {
                flows.put("flowId: ", f.id().toString());
                flows.put("table: ", f.table().toString());
                flows.put("selector: ", f.selector().criteria().toString());
            }
            flowArray.add(flows);
        });
        return flowArray;
    }

}
