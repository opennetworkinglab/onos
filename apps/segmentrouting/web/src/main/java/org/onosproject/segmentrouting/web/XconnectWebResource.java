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
package org.onosproject.segmentrouting.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.segmentrouting.xconnect.api.XconnectDesc;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;
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
import java.util.Set;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Query, create and remove Xconnects.
 */
@Path("xconnect")
public class XconnectWebResource extends AbstractWebResource {
    private static final String XCONNECTS = "xconnects";
    private static Logger log = LoggerFactory.getLogger(XconnectWebResource.class);

    /**
     * Gets all Xconnects.
     *
     * @return an array of xconnects
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXconnects() {
        XconnectService xconnectService = get(XconnectService.class);
        Set<XconnectDesc> xconnects = xconnectService.getXconnects();

        ObjectNode result = encodeArray(XconnectDesc.class, XCONNECTS, xconnects);
        return ok(result).build();
    }

    /**
     * Create a new Xconnect.
     *
     * @param input JSON stream for xconnect to create
     * @return 200 OK
     * @throws IOException Throws IO exception
     * @onos.rsModel XconnectCreate
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addOrUpdateXconnect(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = readTreeFromStream(mapper, input);
        XconnectDesc desc = codec(XconnectDesc.class).decode(json, this);

        if (desc.ports().size() != 2) {
            throw new IllegalArgumentException("Ports should have only two items.");
        }

        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.addOrUpdateXconnect(desc.key().deviceId(), desc.key().vlanId(), desc.ports());

        return Response.ok().build();
    }


    /**
     * Delete an existing Xconnect.
     *
     * @param input JSON stream for xconnect to remove
     * @return 204 NO CONTENT
     * @throws IOException Throws IO exception
     * @onos.rsModel XconnectDelete
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeXconnect(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = readTreeFromStream(mapper, input);
        XconnectDesc desc = codec(XconnectDesc.class).decode(json, this);

        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.removeXonnect(desc.key().deviceId(), desc.key().vlanId());

        return Response.noContent().build();
    }
}
