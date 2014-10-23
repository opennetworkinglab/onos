/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.rest.BaseResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resource that acts as an ancillary provider for uploading pre-configured
 * devices, ports and links.
 */
@Path("config")
public class ConfigResource extends BaseResource {

    @POST
    @Path("topology")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response topology(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode cfg = mapper.readTree(input);
        new ConfigProvider(cfg, get(DeviceProviderRegistry.class),
                           get(LinkProviderRegistry.class),
                           get(HostProviderRegistry.class)).parse();
        return Response.ok(mapper.createObjectNode().toString()).build();
    }

}
