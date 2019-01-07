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
package org.onosproject.openstacktelemetry.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;

/**
 * Handles REST API call of openstack telemetry configuration.
 */
@Path("config")
public class OpenstackTelemetryConfigWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_CONFIG = "Received config %s request";
    private static final String CONFIG = "config";
    private static final String ADDRESS = "address";
    private static final String QUERY = "QUERY";
    private static final String UPDATE = "UPDATE";
    private static final String DELETE = "DELETE";
    private static final String CONFIG_NAME = "config name";
    private static final String NOT_NULL_MESSAGE = " cannot be null";
    private static final String CONFIG_NOT_FOUND = "Config is not found";

    private final TelemetryConfigAdminService configService =
                                        get(TelemetryConfigAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Updates the telemetry configuration address from the JSON input stream.
     *
     * @param configName telemetry config name
     * @param address telemetry config address
     * @return 200 OK with the updated telemetry config, 400 BAD_REQUEST
     * if the JSON is malformed, and 304 NOT_MODIFIED without the updated config
     * due to incorrect configuration name so that we cannot find the existing config
     */
    @PUT
    @Path("address/{name}/{address}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfigAddress(@PathParam("name") String configName,
                                        @PathParam("address") String address) {
        log.trace(String.format(MESSAGE_CONFIG, UPDATE));

        try {
            TelemetryConfig config = configService.getConfig(
                    nullIsIllegal(configName, CONFIG_NAME + NOT_NULL_MESSAGE));

            if (config == null) {
                log.warn("There is no config found to modify for {}", configName);
                return Response.notModified().build();
            } else {
                Map<String, String> updatedProperties =
                        Maps.newHashMap(config.properties());
                updatedProperties.put(ADDRESS,
                        nullIsIllegal(address, ADDRESS + NOT_NULL_MESSAGE));
                TelemetryConfig updatedConfig =
                        config.updateProperties(updatedProperties);

                configService.updateTelemetryConfig(updatedConfig);
                return Response.ok().build();
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Deletes the telemetry configuration by referring to configuration name.
     *
     * @param configName telemetry configuration name
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed,
     * and 304 NOT_MODIFIED without removing config, due to incorrect
     * configuration name so that we cannot find the existing config
     */
    @DELETE
    @Path("{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTelemetryConfig(@PathParam("name") String configName) {
        log.trace(String.format(MESSAGE_CONFIG, DELETE));

        TelemetryConfig config = configService.getConfig(
                nullIsIllegal(configName, CONFIG_NAME + NOT_NULL_MESSAGE));

        if (config == null) {
            log.warn("There is no config found to delete for {}", configName);
            return Response.notModified().build();
        } else {
            configService.removeTelemetryConfig(configName);
            return Response.noContent().build();
        }
    }

    /**
     * Get details of telemetry config.
     * Returns detailed properties of the specified telemetry config.
     *
     * @param configName telemetry configName
     * @return 200 OK with detailed properties of the specific telemetry config
     * @onos.rsModel TelemetryConfig
     */
    @GET
    @Path("{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("name") String configName) {
        log.trace(String.format(MESSAGE_CONFIG, QUERY));

        final TelemetryConfig config =
                nullIsNotFound(configService.getConfig(configName), CONFIG_NOT_FOUND);
        final ObjectNode root = codec(TelemetryConfig.class).encode(config, this);
        return ok(root).build();
    }

    /**
     * Enables the telemetry configuration with the given config name.
     *
     * @param configName telemetry configuration name
     * @return 200 OK with the enabled telemetry config,
     * 400 BAD_REQUEST if the JSON is malformed,
     * and 304 NOT_MODIFIED without removing config, due to incorrect
     * configuration name so that we cannot find the existing config
     */
    @PUT
    @Path("enable/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enableConfig(@PathParam("name") String configName) {
        log.trace(String.format(MESSAGE_CONFIG, UPDATE));

        TelemetryConfig config = configService.getConfig(
                nullIsIllegal(configName, CONFIG_NAME + NOT_NULL_MESSAGE));

        if (config == null) {
            log.warn("There is no config found to enable for {}", configName);
            return Response.notModified().build();
        } else {
            TelemetryConfig updatedConfig = config.updateStatus(ENABLED);
            configService.updateTelemetryConfig(updatedConfig);
            return Response.ok().build();
        }
    }

    /**
     * Disables the telemetry configuration with the given config name.
     *
     * @param configName telemetry configuration name
     * @return 200 OK with the disabled telemetry config
     * 400 BAD_REQUEST if the JSON is malformed,
     * and 304 NOT_MODIFIED without removing config, due to incorrect
     * configuration name so that we cannot find the existing config
     */
    @PUT
    @Path("disable/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response disableConfig(@PathParam("name") String configName) {
        log.trace(String.format(MESSAGE_CONFIG, UPDATE));

        TelemetryConfig config = configService.getConfig(
                nullIsIllegal(configName, CONFIG_NAME + NOT_NULL_MESSAGE));

        if (config == null) {
            log.warn("There is no config found to disable for {}", configName);
            return Response.notModified().build();
        } else {
            TelemetryConfig updatedConfig = config.updateStatus(DISABLED);
            configService.updateTelemetryConfig(updatedConfig);
            return Response.ok().build();
        }
    }
}
