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
 */
package org.onosproject.mfwd.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onlab.packet.IpPrefix;
import org.onlab.rest.BaseResource;
import org.onosproject.mfwd.impl.McastRouteGroup;
import org.onosproject.mfwd.impl.McastRouteTable;

/**
 * Rest API for Multicast Forwarding.
 */
@Path("mcast")
public class McastResource extends BaseResource {

    /**
     * Retrieve the multicast route table.
     * @return the multicast route table.
     * @throws IOException if an error occurs
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response showAll() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        McastRouteTable mcastRouteTable = McastRouteTable.getInstance();
        Map<IpPrefix, McastRouteGroup> map = mcastRouteTable.getMrib4();
        return Response.ok(mapper.createObjectNode().toString()).build();
    }

    /**
     * Static join of a multicast flow.
     * @param input source, group, ingress connectPoint egress connectPoints
     * @return status of static join
     * @throws IOException if an error occurs
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response join(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode cfg = mapper.readTree(input);
        return null;
    }
}
