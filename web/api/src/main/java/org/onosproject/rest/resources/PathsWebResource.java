/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.topology.PathService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * Compute paths in the network graph.
 */
@Path("paths")
public class PathsWebResource extends AbstractWebResource {

    /**
     * Determines if the id appears to be the id of a host.
     * Host id format is 00:00:00:00:00:01/-1
     *
     * @param id id string
     * @return HostId if the id is valid, null otherwise
     */
    private HostId isHostId(String id) {
        return id.matches("..:..:..:..:..:../.*") ? HostId.hostId(id) : null;
    }

    /**
     * Returns either host id or device id, depending on the ID format.
     *
     * @param id host or device id string
     * @return element id
     */
    private ElementId elementId(String id) {
        ElementId elementId = isHostId(id);
        return elementId != null ? elementId : DeviceId.deviceId(id);
    }

    /**
     * Gets all shortest paths between any two hosts or devices.
     * Returns array of all shortest paths between any two elements.
     * @onos.rsModel Paths
     * @param src source identifier
     * @param dst destination identifier
     * @return 200 OK with array of all shortest paths between any two elements
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{src}/{dst}")
    public Response getPath(@PathParam("src") String src,
                            @PathParam("dst") String dst) {
        PathService pathService = get(PathService.class);
        Set<org.onosproject.net.Path> paths =
                pathService.getPaths(elementId(src), elementId(dst));
        return ok(encodeArray(org.onosproject.net.Path.class, "paths", paths)).build();
    }

    /**
     * Gets all shortest disjoint path pairs between any two hosts or devices.
     * Returns array of all shortest disjoint path pairs between any two elements.
     * @onos.rsModel Paths
     * @param src source identifier
     * @param dst destination identifier
     * @return 200 OK with array of all shortest disjoint path pairs between any two elements
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{src}/{dst}/disjoint")
    public Response getDisjointPath(@PathParam("src") String src,
                                    @PathParam("dst") String dst) {
        PathService pathService = get(PathService.class);
        Set<org.onosproject.net.DisjointPath> paths =
                pathService.getDisjointPaths(elementId(src), elementId(dst));
        return ok(encodeArray(org.onosproject.net.DisjointPath.class, "paths", paths)).build();
    }
}
