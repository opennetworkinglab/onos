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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onlab.rest.BaseResource;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.link.LinkProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Inject devices, ports, links and end-station hosts.
 */
@Path("config")
public class ConfigWebResource extends BaseResource {

    private static Logger log = LoggerFactory.getLogger(ConfigWebResource.class);

    /**
     * Upload device, port, link and host data.
     *
     * @param input JSON blob
     * @return 200 OK
     */
    @POST
    @Path("topology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response topology(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = mapper.readTree(input);
            new ConfigProvider(cfg, get(DeviceService.class),
                               get(DeviceProviderRegistry.class),
                               get(LinkProviderRegistry.class),
                               get(HostProviderRegistry.class)).parse();
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Unable to parse topology configuration", e);
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        }
    }

}
