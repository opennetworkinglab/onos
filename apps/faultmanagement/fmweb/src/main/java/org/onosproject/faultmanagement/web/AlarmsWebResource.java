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
package org.onosproject.faultmanagement.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.core.Response;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Alarms on devices or ONOS.
 */
@Path("alarms")
public class AlarmsWebResource extends AbstractWebResource {

    public static final String ALARM_NOT_FOUND = "Alarm is not found";

    private final Logger log = getLogger(getClass());

    public AlarmsWebResource() {
    }

    /**
     * Get all alarms. Returns a list of all alarms across all devices.
     *
     * @param includeCleared include recently cleared alarms in response
     * @return JSON encoded set of alarms
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAlarms(@DefaultValue("false") @QueryParam("includeCleared") boolean includeCleared
    ) {

        log.info("Requesting all alarms, includeCleared={}", includeCleared);
        final AlarmService service = get(AlarmService.class);

        final Iterable<Alarm> alarms = includeCleared
                ? service.getAlarms()
                : service.getActiveAlarms();

        final ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("alarms",
                codec(Alarm.class).
                encode(alarms, this));
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
        log.info("HTTP GET alarm for id={}", id);

        final AlarmId alarmId = toAlarmId(id);
        final Alarm alarm = get(AlarmService.class).getAlarm(alarmId);

        final ObjectNode result = mapper().createObjectNode();
        result.set("alarm", codec(Alarm.class).encode(alarm, this));
        return ok(result.toString()).build();
    }

    /**
     * Update book-keeping fields on the alarm. Returns an up-to-date version of the alarm. Some of its fields may have
     * been updated since the REST client last retrieved the alarm being updated.
     *
     * @param alarmIdPath alarm id path
     * @param stream input JSON
     * @return updated JSON encoded alarm
     */
    @PUT
    @Path("{alarm_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("alarm_id") String alarmIdPath, InputStream stream) {
        log.info("PUT NEW ALARM at /{}", alarmIdPath);

        try {
            final ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            log.info("jsonTree={}", jsonTree);

            final Alarm alarm = codec(Alarm.class).decode(jsonTree, this);

            final AlarmService service = get(AlarmService.class);

            if (Long.parseLong(alarmIdPath) != alarm.id().fingerprint()) {
                throw new IllegalArgumentException("id in path is " + Long.parseLong(alarmIdPath)
                        + " but payload uses id=" + alarm.id().fingerprint());

            }
            final Alarm updated = service.update(alarm);
            final ObjectNode encoded = new AlarmCodec().encode(updated, this);
            return ok(encoded.toString()).build();

        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    private static AlarmId toAlarmId(String id) {
        try {
            return AlarmId.valueOf(Long.parseLong(id));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Alarm id should be numeric", ex);
        }

    }

}
