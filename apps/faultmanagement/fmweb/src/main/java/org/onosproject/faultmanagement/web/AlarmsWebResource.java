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
package org.onosproject.faultmanagement.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;

import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.core.Response;

import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.onosproject.alarm.AlarmService;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Alarms on devices or ONOS.
 */
@Path("alarms")
public class AlarmsWebResource extends AbstractWebResource {

    private static final String ALARM_NOT_FOUND = "Alarm is not found";

    private final Logger log = getLogger(getClass());

    /**
     * Get alarms. Returns a list of alarms
     *
     * @param includeCleared (optional) include recently cleared alarms in response
     * @param devId          (optional) include only for specified device
     * @return JSON encoded set of alarms
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAlarms(@DefaultValue("false") @QueryParam("includeCleared") boolean includeCleared,
                              @DefaultValue("") @QueryParam("devId") String devId
    ) {

        log.debug("Requesting all alarms, includeCleared={}", includeCleared);
        AlarmService service = get(AlarmService.class);

        Iterable<Alarm> alarms;
        if (StringUtils.isBlank(devId)) {
            alarms = includeCleared
                    ? service.getAlarms()
                    : service.getActiveAlarms();
        } else {
            alarms = service.getAlarms(DeviceId.deviceId(devId));
        }
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("alarms", new AlarmCodec().encode(alarms, this));
        return ok(result.toString()).build();

    }

    /**
     * Get specified alarm. Returns details of the specified alarm.
     *
     * @param id ONOS allocated identifier
     * @return JSON encoded alarm
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAlarm(@PathParam("id") String id) {
        log.debug("HTTP GET alarm for id={}", id);

        AlarmId alarmId = AlarmId.alarmId(id);
        Alarm alarm = get(AlarmService.class).getAlarm(alarmId);

        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("alarm", new AlarmCodec().encode(alarm, this));
        return ok(result.toString()).build();
    }

    /**
     * Update book-keeping fields on the alarm. Returns an up-to-date version of the alarm. Some of its fields may have
     * been updated since the REST client last retrieved the alarm being updated.
     *
     * @param alarmIdPath alarm id path
     * @param stream      input JSON
     * @return updated JSON encoded alarm
     */
    @PUT
    @Path("{alarm_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("alarm_id") String alarmIdPath, InputStream stream) {
        log.debug("PUT NEW ALARM at /{}", alarmIdPath);

        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            log.debug("jsonTree={}", jsonTree);

            Alarm alarm = new AlarmCodec().decode(jsonTree, this);

            AlarmService service = get(AlarmService.class);

            if (!alarmIdPath.equals(alarm.id().toString())) {
                throw new IllegalArgumentException("id in path is " + alarmIdPath
                                                           + " but payload uses id=" + alarm.id().toString());

            }
            Alarm updated = service.updateBookkeepingFields(
                    alarm.id(), alarm.cleared(), alarm.acknowledged(), alarm.assignedUser()
            );
            ObjectNode encoded = new AlarmCodec().encode(updated, this);
            return ok(encoded.toString()).build();

        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

}
