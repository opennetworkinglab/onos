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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.util.IntentFilter;
import org.onosproject.net.intent.util.IntentMiniSummary;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query, submit and withdraw network intents.
 */
@Path("intents")
public class IntentsWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(IntentsWebResource.class);
    private static final int WITHDRAW_EVENT_TIMEOUT_SECONDS = 5;

    private static final String APP_ID_NOT_FOUND = "Application Id not found";
    private static final String INTENTS = "intents";
    private static final String APP_ID = "appId";
    private static final String ID = "id";
    private static final String INTENT_PATHS = "paths";
    private static final String INTENT_TYPE = "type";
    private static final String INTENT_NOT_FOUND = "Intent is not found";

    @Context
    private UriInfo uriInfo;

    /**
     * Returns the JSON codec for the specified intent.
     *
     * @param intent the intent instance
     * @return JSON codec
     */
    public JsonCodec<Intent> codec(Intent intent) {
        return Optional.ofNullable((JsonCodec<Intent>) codec(intent.getClass()))
                .orElse(codec(Intent.class));
    }

    /**
     * Gets all intents.
     * Returns array containing all the intents in the system.
     * @param detail flag to return full details of intents in list.
     *
     * @return 200 OK with array of all the intents in the system
     * @onos.rsModel Intents
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntents(@QueryParam("detail") boolean detail) {
        ObjectNode root = null;
        final Iterable<Intent> intents = get(IntentService.class).getIntents();
        if (detail) {
            root = mapper().createObjectNode();
            ArrayNode intentsNode = root.putArray(INTENTS);
            intents.forEach(intent -> intentsNode.add(codec(intent).encode(intent, this)));
        } else {
            root = encodeArray(Intent.class, INTENTS, intents);
        }

        return ok(root).build();
    }


    /**
     * Gets Summary of all intents.
     * Returns Summary of the intents in the system.
     *
     * @return 200 OK with Summary of all the intents in the system
     * @onos.rsModel Minisummary
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("minisummary")
    public Response getIntentSummary() {
        final Iterable<Intent> intents = get(IntentService.class).getIntents();
        ObjectNode root = mapper().createObjectNode();
        IntentMiniSummary intentminisummary = new IntentMiniSummary();
        Map<String, IntentMiniSummary> map = intentminisummary.summarize(intents, get(IntentService.class));
        map.values().stream().forEach(intentsummary -> {
            root.put(intentsummary.getIntentType(), codec(IntentMiniSummary.class).encode(intentsummary, this));
        });
        return ok(root).build();
    }


    /**
     * Gets intent installables by application ID and key.
     * @param appId application identifier
     * @param key   intent key
     *
     * @return 200 OK with array of the intent installables
     * @onos.rsModel Intents
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("installables/{appId}/{key}")
    public Response getIntentWithInstallable(@PathParam("appId") String appId,
                                             @PathParam("key") String key) {
        final IntentService intentService = get(IntentService.class);
        final ApplicationId app = get(CoreService.class).getAppId(appId);
        nullIsNotFound(app, APP_ID_NOT_FOUND);

        Intent intent = intentService.getIntent(Key.of(key, app));
        if (intent == null) {
            long numericalKey = Long.decode(key);
            intent = intentService.getIntent(Key.of(numericalKey, app));
        }
        nullIsNotFound(intent, INTENT_NOT_FOUND);

        final Iterable<Intent> installables = intentService.getInstallableIntents(intent.key());
        final ObjectNode root = encodeArray(Intent.class, "installables", installables);
        return ok(root).build();
    }

    /**
     * Gets intent by application and key.
     * Returns details of the specified intent.
     *
     * @param appId application identifier
     * @param key   intent key
     * @return 200 OK with intent data
     * @onos.rsModel Intents
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{appId}/{key}")
    public Response getIntentById(@PathParam("appId") String appId,
                                  @PathParam("key") String key) {
        final ApplicationId app = get(CoreService.class).getAppId(appId);
        nullIsNotFound(app, APP_ID_NOT_FOUND);
        Intent intent = get(IntentService.class).getIntent(Key.of(key, app));
        if (intent == null) {
            long numericalKey = Long.decode(key);
            intent = get(IntentService.class).getIntent(Key.of(numericalKey, app));
        }
        nullIsNotFound(intent, INTENT_NOT_FOUND);

        final ObjectNode root = codec(intent).encode(intent, this);

        return ok(root).build();
    }

    /**
     * Gets intents by application.
     * Returns the intents specified by the application id.
     * @param detail flag to return full details of intents in list.
     *
     * @param appId application identifier
     * @return 200 OK with a collection of intents of given application id
     * @onos.rsModel Intents
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("application/{appId}")
    public Response getIntentsByAppId(@PathParam("appId") String appId, @QueryParam("detail") boolean detail) {
        final ApplicationId app = get(CoreService.class).getAppId(appId);
        nullIsNotFound(app, APP_ID_NOT_FOUND);

        final Iterable<Intent> intents = get(IntentService.class).getIntentsByAppId(app);
        ObjectNode root = null;
        if (detail) {
            root = mapper().createObjectNode();
            ArrayNode intentsNode = root.putArray(INTENTS);
            intents.forEach(intent -> intentsNode.add(codec(intent).encode(intent, this)));
        } else {
            root = encodeArray(Intent.class, INTENTS, intents);
        }

        return ok(root).build();
    }

    /**
     * Gets all related flow entries created by a particular intent.
     * Returns all flow entries of the specified intent.
     *
     * @param appId application identifier
     * @param key   intent key
     * @return 200 OK with intent data
     * @onos.rsModel Relatedflows
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("relatedflows/{appId}/{key}")
    public Response getIntentFlowsById(@PathParam("appId") String appId,
                                       @PathParam("key") String key) {
        ApplicationId applicationId = get(CoreService.class).getAppId(appId);
        nullIsNotFound(applicationId, APP_ID_NOT_FOUND);
        IntentService intentService = get(IntentService.class);
        FlowRuleService flowService = get(FlowRuleService.class);

        Intent intent = intentService.getIntent(Key.of(key, applicationId));
        if (intent == null) {
            long numericalKey = Long.decode(key);
            intent = intentService.getIntent(
                    Key.of(numericalKey, applicationId));
        }
        nullIsNotFound(intent, INTENT_NOT_FOUND);

        ObjectNode root = mapper().createObjectNode();
        root.put(APP_ID, appId);
        root.put(ID, key);

        IntentFilter intentFilter = new IntentFilter(intentService, flowService);

        List<Intent> installables =
                intentService.getInstallableIntents(intent.key());

        root.put(INTENT_TYPE, intent.getClass().getSimpleName());

        ArrayNode pathsNode = root.putArray(INTENT_PATHS);

        for (List<FlowEntry> flowEntries :
                intentFilter.readIntentFlows(installables)) {
            ArrayNode flowNode = pathsNode.addArray();

            for (FlowEntry entry : flowEntries) {
                flowNode.add(codec(FlowEntry.class).encode(entry, this));
            }
        }
        return ok(root).build();
    }

    /**
     * Internal listener for tracking the intent deletion events.
     */
    private class DeleteListener implements IntentListener {
        final Key key;
        final CountDownLatch latch;

        /**
         * Default constructor.
         *
         * @param key   key
         * @param latch count down latch
         */
        DeleteListener(Key key, CountDownLatch latch) {
            this.key = key;
            this.latch = latch;
        }

        @Override
        public void event(IntentEvent event) {
            if (Objects.equals(event.subject().key(), key) &&
                    (event.type() == IntentEvent.Type.WITHDRAWN ||
                            event.type() == IntentEvent.Type.FAILED)) {
                latch.countDown();
            }
        }
    }

    /**
     * Submits a new intent.
     * Creates and submits intent from the JSON request.
     *
     * @param stream input JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel IntentHost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        try {
            IntentService service = get(IntentService.class);
            ObjectNode root = readTreeFromStream(mapper(), stream);
            Intent intent = codec(Intent.class).decode(root, this);
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

    /**
     * Withdraws intent.
     * Withdraws the specified intent from the system.
     *
     * @param appId application identifier
     * @param key   intent key
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{appId}/{key}")
    public Response deleteIntentById(@PathParam("appId") String appId,
                                     @PathParam("key") String key) {
        final ApplicationId app = get(CoreService.class).getAppId(appId);
        nullIsNotFound(app, APP_ID_NOT_FOUND);
        Intent intent = get(IntentService.class).getIntent(Key.of(key, app));
        IntentService service = get(IntentService.class);

        if (intent == null) {
            intent = service
                    .getIntent(Key.of(Long.decode(key), app));
        }
        if (intent == null) {
            // No such intent.  REST standards recommend a positive status code
            // in this case.
            return Response.noContent().build();
        }

        Key k = intent.key();

        // set up latch and listener to track uninstall progress
        CountDownLatch latch = new CountDownLatch(1);

        IntentListener listener = new DeleteListener(k, latch);
        service.addListener(listener);

        try {
            // request the withdraw
            service.withdraw(intent);

            try {
                latch.await(WITHDRAW_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.info("REST Delete operation timed out waiting for intent {}", k);
                Thread.currentThread().interrupt();
            }
            // double check the state
            IntentState state = service.getIntentState(k);
            if (state == WITHDRAWN || state == FAILED) {
                service.purge(intent);
            }

        } finally {
            // clean up the listener
            service.removeListener(listener);
        }
        return Response.noContent().build();
    }
}
