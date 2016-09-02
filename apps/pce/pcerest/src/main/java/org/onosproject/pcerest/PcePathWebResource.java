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
package org.onosproject.pcerest;

import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.nullIsNotFound;

import java.util.Collection;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.LinkedList;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.api.PceService;
import org.onosproject.pce.pceservice.PcePath;
import org.onosproject.pce.pceservice.DefaultPcePath;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program pce path.
 */
@Path("path")
public class PcePathWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(PcePathWebResource.class);
    public static final String PCE_PATH_NOT_FOUND = "Path not found";
    public static final String PCE_PATH_ID_EXIST = "Path exists";
    public static final String PCE_PATH_ID_NOT_EXIST = "Path does not exist for the identifier";
    public static final String PCE_SETUP_PATH_FAILED = "PCE Setup path has failed.";

    /**
     * Retrieve details of all paths created.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response queryAllPath() {
        log.debug("Query all paths.");
        Iterable<Tunnel> tunnels = get(PceService.class).queryAllPath();
        ObjectNode result = mapper().createObjectNode();
        ArrayNode pathEntry = result.putArray("paths");
        if (tunnels != null) {
            for (final Tunnel tunnel : tunnels) {
                PcePath path = DefaultPcePath.builder().of(tunnel).build();
                pathEntry.add(codec(PcePath.class).encode(path, this));
            }
        }
        return ok(result.toString()).build();
    }

    /**
     * Retrieve details of a specified path id.
     *
     * @param id path id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @GET
    @Path("{path_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response queryPath(@PathParam("path_id") String id) {
        log.debug("Query path by identifier {}.", id);
        Tunnel tunnel = nullIsNotFound(get(PceService.class).queryPath(TunnelId.valueOf(id)),
                                       PCE_PATH_NOT_FOUND);
        PcePath path = DefaultPcePath.builder().of(tunnel).build();
        if (path == null) {
            return Response.status(OK).entity(PCE_SETUP_PATH_FAILED).build();
        }
        ObjectNode result = mapper().createObjectNode();
        result.set("path", codec(PcePath.class).encode(path, this));
        return ok(result.toString()).build();
    }

    /**
     * Creates a new path.
     *
     * @param stream pce path from json
     * @return status of the request
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setupPath(InputStream stream) {
        log.debug("Setup path.");
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode port = jsonTree.get("path");
            TunnelService tunnelService = get(TunnelService.class);
            PcePath path = codec(PcePath.class).decode((ObjectNode) port, this);
            if (path == null) {
                return Response.status(OK).entity(PCE_SETUP_PATH_FAILED).build();
            }

            //Validating tunnel name, duplicated tunnel names not allowed
            Collection<Tunnel> existingTunnels = tunnelService.queryTunnel(Tunnel.Type.MPLS);
            if (existingTunnels != null) {
                for (Tunnel t : existingTunnels) {
                    if (t.tunnelName().toString().equals(path.name())) {
                        return Response.status(OK).entity(PCE_SETUP_PATH_FAILED).build();
                    }
                }
            }

            DeviceId srcDevice = DeviceId.deviceId(path.source());
            DeviceId dstDevice = DeviceId.deviceId(path.destination());
            LspType lspType = path.lspType();
            List<Constraint> listConstrnt = new LinkedList<Constraint>();

            // Add bandwidth
            listConstrnt.add(path.bandwidthConstraint());

            // Add cost
            listConstrnt.add(path.costConstraint());

            List<ExplicitPathInfo> explicitPathInfoList = null;
            if (explicitPathInfoList != null) {
                explicitPathInfoList = ImmutableList.copyOf(path.explicitPathInfo());
            }

            Boolean issuccess = nullIsNotFound(get(PceService.class)
                                               .setupPath(srcDevice, dstDevice, path.name(), listConstrnt,
                                                       lspType, explicitPathInfoList),
                                               PCE_SETUP_PATH_FAILED);
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (IOException e) {
            log.error("Exception while creating path {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Update details of a specified path id.
     *
     * @param id path id
     * @param stream pce path from json
     * @return 200 OK, 404 if given identifier does not exist
     */
    @PUT
    @Path("{path_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePath(@PathParam("path_id") String id,
            final InputStream stream) {
        log.debug("Update path by identifier {}.", id);
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode pathNode = jsonTree.get("path");
            PcePath path = codec(PcePath.class).decode((ObjectNode) pathNode, this);
            if (path == null) {
                return Response.status(OK).entity(PCE_SETUP_PATH_FAILED).build();
            }
            List<Constraint> constrntList = new LinkedList<Constraint>();
            // Assign bandwidth
            if (path.bandwidthConstraint() != null) {
                constrntList.add(path.bandwidthConstraint());
            }

            // Assign cost
            if (path.costConstraint() != null) {
                constrntList.add(path.costConstraint());
            }

            Boolean result = nullIsNotFound(get(PceService.class).updatePath(TunnelId.valueOf(id), constrntList),
                                            PCE_PATH_NOT_FOUND);
            return Response.status(OK).entity(result.toString()).build();
        } catch (IOException e) {
            log.error("Update path failed because of exception {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Release a specified path.
     *
     * @param id path id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @DELETE
    @Path("{path_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response releasePath(@PathParam("path_id") String id) {
        log.debug("Deletes path by identifier {}.", id);

        Boolean isSuccess = nullIsNotFound(get(PceService.class).releasePath(TunnelId.valueOf(id)),
                                           PCE_PATH_NOT_FOUND);
        if (!isSuccess) {
            log.debug("Path identifier {} does not exist", id);
        }

        return Response.status(OK).entity(isSuccess.toString()).build();
    }
}
