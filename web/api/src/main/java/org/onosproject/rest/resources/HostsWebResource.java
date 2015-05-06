/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.HostId.hostId;

/**
 * REST resource for interacting with the inventory of hosts.
 */
@Path("hosts")
public class HostsWebResource extends AbstractWebResource {

    public static final String HOST_NOT_FOUND = "Host is not found";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHosts() {
        final Iterable<Host> hosts = get(HostService.class).getHosts();
        final ObjectNode root = encodeArray(Host.class, "hosts", hosts);
        return ok(root).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response getHostById(@PathParam("id") String id) {
        final Host host = nullIsNotFound(get(HostService.class).getHost(hostId(id)),
                HOST_NOT_FOUND);
        final ObjectNode root = codec(Host.class).encode(host, this);
        return ok(root).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{mac}/{vlan}")
    public Response getHostByMacAndVlan(@PathParam("mac") String mac,
                                        @PathParam("vlan") String vlan) {
        final Host host = nullIsNotFound(get(HostService.class).getHost(hostId(mac + "/" + vlan)),
                HOST_NOT_FOUND);
        final ObjectNode root = codec(Host.class).encode(host, this);
        return ok(root).build();
    }
}

