/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * REST resource for interacting with the inventory of intents.
 */

@Path("intents")
public class IntentsWebResource extends AbstractWebResource {
    public static final String INTENT_NOT_FOUND = "Intent is not found";

    /**
     * Gets an array containing all the intents in the system.
     *
     * @return array of all the intents in the system
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntents() {
        final Iterable<Intent> intents = get(IntentService.class).getIntents();
        final ObjectNode root = encodeArray(Intent.class, "intents", intents);
        return ok(root).build();
    }

    /**
     * Gets a single intent by Id.
     *
     * @param appId the Application ID
     * @param key the Intent key value to look up
     * @return intent data
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{appId}/{key}")
    public Response getIntentById(@PathParam("appId") Short appId,
                                  @PathParam("key") String key) {
        final ApplicationId app = get(CoreService.class).getAppId(appId);

        Intent intent = get(IntentService.class).getIntent(Key.of(key, app));
        if (intent == null) {
            intent = get(IntentService.class)
                    .getIntent(Key.of(Long.parseLong(key), app));
        }
        nullIsNotFound(intent, INTENT_NOT_FOUND);

        final ObjectNode root;
        if (intent instanceof HostToHostIntent) {
            root = codec(HostToHostIntent.class).encode((HostToHostIntent) intent, this);
        } else if (intent instanceof PointToPointIntent) {
            root = codec(PointToPointIntent.class).encode((PointToPointIntent) intent, this);
        } else {
            root = codec(Intent.class).encode(intent, this);
        }
        return ok(root).build();
    }
}
