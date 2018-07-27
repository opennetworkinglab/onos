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
package org.onosproject.openstacktroubleshoot.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Handles REST API call of openstack troubleshoot.
 */
@Path("troubleshoot")

public class OpenstackTroubleshootWebResource extends AbstractWebResource {

    private final ObjectNode root = mapper().createObjectNode();

    /**
     * OpenstackTroubleshootServiceImpl method.
     *
     * @return 200 OK
     *
     * @onos.rsModel dummy
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response dummy() {
        return ok(root).build();
    }
}
