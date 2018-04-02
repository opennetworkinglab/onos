/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.optical.rest;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.nullIsNotFound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.OchSignal;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.optical.json.OchSignalCodec;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.net.optical.util.OpticalIntentUtility.createOpticalIntent;


/**
 * Query, submit and withdraw optical network intents.
 */
@Path("intents")
public class OpticalIntentsWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(OpticalIntentsWebResource.class);

    private static final String JSON_INVALID = "Invalid json input";

    private static final String APP_ID = "appId";

    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";

    private static final String BIDIRECTIONAL = "bidirectional";

    private static final String SIGNAL = "signal";

    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required";
    private static final String E_APP_ID_NOT_FOUND =
            "Application ID is not found";

    @Context
    private UriInfo uriInfo;

    /**
     * Submits a new optical intent.
     * Creates and submits optical intents from the JSON request.
     *
     * @param stream input JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel CreateIntent
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        try {
            IntentService service = get(IntentService.class);
            ObjectNode root = readTreeFromStream(mapper(), stream);
            Intent intent = decode(root);
            service.submit(intent);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("intents")
                    .path(intent.appId().name())
                    .path(Long.toString(intent.id().fingerprint()));
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    private Intent decode(ObjectNode json) {
        JsonNode ingressJson = json.get(INGRESS_POINT);
        if (!ingressJson.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        ConnectPoint ingress = codec(ConnectPoint.class).decode((ObjectNode) ingressJson, this);

        JsonNode egressJson = json.get(EGRESS_POINT);
        if (!egressJson.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        ConnectPoint egress = codec(ConnectPoint.class).decode((ObjectNode) egressJson, this);

        JsonNode bidirectionalJson = json.get(BIDIRECTIONAL);
        boolean bidirectional = bidirectionalJson != null ? bidirectionalJson.asBoolean() : false;

        JsonNode signalJson = json.get(SIGNAL);
        OchSignal signal = null;
        if (signalJson != null) {
            if (!signalJson.isObject()) {
                throw new IllegalArgumentException(JSON_INVALID);
            } else {
                signal = OchSignalCodec.decode((ObjectNode) signalJson);
            }
        }

        String appIdString = nullIsIllegal(json.get(APP_ID), APP_ID + MISSING_MEMBER_MESSAGE).asText();
        CoreService service = getService(CoreService.class);
        ApplicationId appId = nullIsNotFound(service.getAppId(appIdString), E_APP_ID_NOT_FOUND);
        Key key = null;
        DeviceService deviceService = get(DeviceService.class);

        return createOpticalIntent(ingress, egress, deviceService, key, appId, bidirectional, signal);
    }
}
