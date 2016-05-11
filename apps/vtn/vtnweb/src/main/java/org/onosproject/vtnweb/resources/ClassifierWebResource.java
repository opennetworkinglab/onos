/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnweb.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.classifier.ClassifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program classifiers.
 */
@Path("classifiers")
public class ClassifierWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(ClassifierWebResource.class);

    /**
     * Get the list of classifier devices.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getClassifiers() {

        ObjectNode result = mapper().createObjectNode();

        Iterable<DeviceId> classifierDevices = get(ClassifierService.class).getClassifiers();
        ArrayNode classifier = result.putArray("classifiers");
        if (classifierDevices != null) {
            for (final DeviceId deviceId : classifierDevices) {
                ObjectNode dev = mapper().createObjectNode()
                        .put("DeviceId", deviceId.toString());
                classifier.add(dev);
            }
        }
        return ok(result.toString()).build();
    }
}
