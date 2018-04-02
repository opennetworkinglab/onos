/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.kafkaintegration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.kafkaintegration.api.EventSubscriptionService;
import org.onosproject.kafkaintegration.api.dto.EventSubscriber;
import org.onosproject.kafkaintegration.api.dto.RegistrationResponse;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Rest Interfaces for subscribing/unsubscribing to event notifications.
 */
@Path("kafkaService")
public class EventExporterWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String JSON_NOT_NULL =
            "Registration Data cannot be empty";
    public static final String REGISTRATION_SUCCESSFUL =
            "Registered Listener successfully";
    public static final String DEREGISTRATION_SUCCESSFUL =
            "De-Registered Listener successfully";
    public static final String EVENT_SUBSCRIPTION_SUCCESSFUL =
            "Event Registration successful";
    public static final String EVENT_SUBSCRIPTION_UNSUCCESSFUL =
            "Event subscription unsuccessful";
    public static final String EVENT_SUBSCRIPTION_REMOVED =
            "Event De-Registration successful";

    /**
     * Registers a listener for ONOS Events.
     *
     * @param appName The application trying to register
     * @return 200 OK with UUID string which should be used as Kafka Consumer
     *         Group Id and Kafka Server, port information.
     * @onos.rsModel KafkaRegistration
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("register")
    public Response registerKafkaListener(String appName) {

        EventSubscriptionService service = get(EventSubscriptionService.class);

        RegistrationResponse response = service.registerListener(appName);

        ObjectNode result = mapper().createObjectNode();
        result.put("groupId", response.getGroupId().getId().toString());
        result.put("ipAddress", response.getIpAddress());
        result.put("port", response.getPort());

        log.info("Registered app {}", appName);

        return ok(result.toString()).build();
    }

    /**
     * Unregisters a listener for ONOS Events.
     *
     * @param appName The application trying to unregister
     * @return 200 OK
     * @onos.rsModel KafkaRegistration
     */
    @DELETE
    @Path("unregister")
    public Response removeKafkaListener(String appName) {
        EventSubscriptionService service = get(EventSubscriptionService.class);

        service.unregisterListener(appName);
        log.info("Unregistered app {}", appName);
        return ok(DEREGISTRATION_SUCCESSFUL).build();
    }

    /**
     * Creates subscription to a specific ONOS event.
     *
     * @param input Subscription Data in JSON format
     * @return 200 OK if successful or 400 BAD REQUEST
     * @onos.rsModel KafkaSubscription
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("subscribe")
    public Response subscribe(InputStream input) {

        EventSubscriptionService service = get(EventSubscriptionService.class);

        try {
            EventSubscriber sub = parseSubscriptionData(input);
            service.subscribe(sub);
            // It will subscribe to all the topics. Not only the one that is sent by the consumer.
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }

        return ok(EVENT_SUBSCRIPTION_SUCCESSFUL).build();
    }

    /**
     * Parses JSON Subscription Data from the external application.
     *
     * @param input Subscription Data in JSON format
     * @return parsed DTO object
     * @throws IOException
     */
    private EventSubscriber parseSubscriptionData(InputStream input)
            throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = readTreeFromStream(mapper, input);
        checkNotNull(node, JSON_NOT_NULL);
        EventSubscriber codec = codec(EventSubscriber.class).decode(node, this);
        checkNotNull(codec, JSON_NOT_NULL);
        return codec;
    }

    /**
     * Deletes subscription from a specific ONOS event.
     *
     * @param input data in JSON format
     * @return 200 OK if successful or 400 BAD REQUEST
     * @onos.rsModel KafkaSubscription
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("unsubscribe")
    public Response unsubscribe(InputStream input) {

        EventSubscriptionService service = get(EventSubscriptionService.class);

        try {
            EventSubscriber sub = parseSubscriptionData(input);
            service.unsubscribe(sub);
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }

        return ok(EVENT_SUBSCRIPTION_REMOVED).build();
    }
}
