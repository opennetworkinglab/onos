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
package org.onosproject.drivers.ciena.waveserver.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.driver.optical.flowrule.CrossConnectCache;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmEntityId;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class CienaRestDevice {
    private static final Frequency CENTER_FREQUENCY = Frequency.ofGHz(195_950);
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private static final String VALUE = "value";
    private static final String STATE = "state";
    private static final String ADMIN_STATE = "admin-state";
    private static final String WAVELENGTH = "wavelength";
    private static final String DATA = "data";
    private static final String ACTIVE = "active";
    private static final String ACKNOWLEDGE = "acknowledged";
    private static final String SEVERITY = "severity";
    private static final String DESCRIPTION = "description";
    private static final String INSTANCE = "instance";
    private static final String PORT = "port";
    private static final String PTP = "ptp";
    private static final String UTC = "UTC";
    private static final String OTHER = "other";
    private static final String DATE_TIME_FORMAT = "EEE MMM [ ]d HH:mm:ss yyyy";
    //keys
    private static final String ALARM_KEY = "ws-alarms";
    private static final String ALARM_INSTANCE_ID = "alarm-instance-id";
    private static final String ALARM_TABLE_ID = "alarm-table-id";
    private static final String ALARM_LOCAL_DATE_TIME = "local-date-time";
    private static final String LINE_SYSTEM_CHANNEL_NUMBER = "ciena-ws-ptp-modem:line-system-channel-number";
    private static final String FREQUENCY_KEY = "ciena-ws-ptp-modem:frequency";
    //URIs
    private static final String PORT_URI = "ws-ptps/ptps/%s";
    private static final String TRANSMITTER_URI = PORT_URI + "/properties/transmitter";
    private static final String PORT_STATE_URI = PORT_URI + "/" + STATE;
    private static final String WAVELENGTH_URI = TRANSMITTER_URI + "/" + WAVELENGTH;
    private static final String FREQUENCY_URI = TRANSMITTER_URI + "/" + FREQUENCY_KEY;
    private static final String CHANNEL_URI = TRANSMITTER_URI + "/" + LINE_SYSTEM_CHANNEL_NUMBER;
    private static final String ACTIVE_ALARMS_URL = ALARM_KEY + "/" + ACTIVE;
    private static final List<String> LINESIDE_PORT_ID = ImmutableList.of("4", "48");
    private static final ChannelSpacing CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;

    private final Logger log = getLogger(getClass());

    private DeviceId deviceId;
    private RestSBController controller;
    private CrossConnectCache crossConnectCache;
    private DeviceService deviceService;


    public CienaRestDevice(DriverHandler handler) throws NullPointerException {
        deviceId = handler.data().deviceId();
        controller = checkNotNull(handler.get(RestSBController.class));
        crossConnectCache = checkNotNull(handler.get(CrossConnectCache.class));
        deviceService = checkNotNull(handler.get(DeviceService.class));
    }

    /**
     * return the Line side ports.
     *
     * @return List of Line side ports.
     */
    public static List<String> getLinesidePortId() {
        return LINESIDE_PORT_ID;
    }

    /**
     * add the given flow rules to cross connect-cache.
     *
     * @param flowRules flow rules that needs to be cached.
     */
    public void setCrossConnectCache(Collection<FlowRule> flowRules) {
        flowRules.forEach(xc -> crossConnectCache.set(
                Objects.hash(deviceId, xc.selector(), xc.treatment()),
                xc.id(),
                xc.priority()));
    }

    /**
     * remove the given flow rules form the cross-connect cache.
     *
     * @param flowRules flow rules that needs to be removed from cache.
     */
    public void removeCrossConnectCache(Collection<FlowRule> flowRules) {
        flowRules.forEach(xc -> crossConnectCache.remove(Objects.hash(deviceId, xc.selector(), xc.treatment())));
    }

    private final String genPortStateRequest(String state) {
        String request = "{\n" +
                "\"" + STATE + "\": {\n" +
                "\"" + ADMIN_STATE + "\": \"" + state + "\"\n}\n}";
        log.debug("generated request: \n{}", request);
        return request;
    }

    private String genWavelengthChangeRequest(String wavelength) {
        String request = "{\n" +
                "\"" + WAVELENGTH + "\": {\n" +
                "\"" + VALUE + "\": " + wavelength + "\n" +
                "}\n" +
                "}";
        log.debug("request:\n{}", request);
        return request;

    }

    private String genFrequencyChangeRequest(double frequency) {
        String request = "{\n" +
                "\"" + FREQUENCY_KEY + "\": {\n" +
                "\"" + VALUE + "\": " + Double.toString(frequency) + "\n" +
                "}\n" +
                "}";
        log.debug("request:\n{}", request);
        return request;

    }

    private String genChannelChangeRequest(int channel) {
        String request = "{\n" +
                "\"" + LINE_SYSTEM_CHANNEL_NUMBER + "\": " +
                Integer.toString(channel) + "\n}";
        log.debug("request:\n{}", request);
        return request;

    }


    private final String genUri(String uriFormat, PortNumber port) {
        return String.format(uriFormat, port.name());
    }

    private boolean isPortState(PortNumber number, String state) {
        log.debug("checking port {} state is {} or not on device {}", number, state, deviceId);
        String uri = genUri(PORT_STATE_URI, number);
        JsonNode jsonNode;
        try {
            jsonNode = get(uri);
        } catch (IOException e) {
            log.error("unable to get port state on device {}", deviceId);
            return false;
        }
        return jsonNode.get(STATE).get(ADMIN_STATE).asText().equals(state);

    }

    private boolean confirmPortState(long timePeriodInMillis, int iterations, PortNumber number, String state) {
        for (int i = 0; i < iterations; i++) {
            log.debug("looping for port state with time period {}ms on device {}. try number {}/{}",
                    timePeriodInMillis, deviceId, i + 1, iterations);
            if (isPortState(number, state)) {
                return true;
            }
            try {
                Thread.sleep(timePeriodInMillis);
            } catch (InterruptedException e) {
                log.error("unable to sleep thread for device {}\n", deviceId, e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean changePortState(PortNumber number, String state) {
        log.debug("changing the port {} on device {} state to {}", number, deviceId, state);
        String uri = genUri(PORT_STATE_URI, number);
        String request = genPortStateRequest(state);

        boolean response = putNoReply(uri, request);
        if (!response) {
            log.error("unable to change port {} on device {} state to {}", number, deviceId, state);
        }

        // 5 tries with 2 sec delay
        long timePeriod = 2000;
        int iterations = 5;
        return confirmPortState(timePeriod, iterations, number, state);
    }

    public boolean disablePort(PortNumber number) {
        return changePortState(number, DISABLED);
    }

    public boolean enablePort(PortNumber number) {
        return changePortState(number, ENABLED);
    }

    public final boolean changeFrequency(OchSignal signal, PortNumber outPort) {
        String uri = genUri(FREQUENCY_URI, outPort);
        double frequency = signal.centralFrequency().asGHz();
        String request = genFrequencyChangeRequest(frequency);
        boolean response = putNoReply(uri, request);
        if (!response) {
            log.error("unable to change frequency of port {} on device {}", outPort, deviceId);
        }
        return response;
    }

    public final boolean changeChannel(OchSignal signal, PortNumber outPort) {
        String uri = genUri(CHANNEL_URI, outPort);
        int channel = signal.spacingMultiplier();
        log.debug("channel is {} for port {} on device {}", channel, outPort.name(), deviceId);
        String request = genChannelChangeRequest(channel);
        boolean response = putNoReply(uri, request);
        if (!response) {
            log.error("unable to change channel to {} for port {} on device {}",
                    channel, outPort.name(), deviceId);
        }
        return response;
    }

    private int getChannel(PortNumber port) {
        try {
            String uri = genUri(CHANNEL_URI, port);
            JsonNode response = get(uri);
            return response.get(LINE_SYSTEM_CHANNEL_NUMBER).asInt();
        } catch (IOException e) {
            // this is expected for client side ports as they don't contain channel data
            log.error("unable to get channel for port {} on device {}:\n{}", port, deviceId, e);
            return -1;
        }

    }

    private int getChannelFromFrequency(Frequency frequency) {
        return (int) frequency.subtract(Spectrum.CENTER_FREQUENCY)
                .floorDivision(CHANNEL_SPACING.frequency().asHz()).asHz();

    }

    private Frequency getFrequency(PortNumber port) {
        try {
            String uri = genUri(FREQUENCY_URI, port);
            JsonNode response = get(uri);
            return Frequency.ofGHz(response.get(FREQUENCY_KEY).get(VALUE).asDouble());
        } catch (IOException e) {
            // this is expected for client side ports as they don't contain frequency data
            log.error("unable to get frequency for port {} on device {}:\n{}", port, deviceId, e);
            return null;
        }

    }

    private AlarmEntityId getAlarmSource(String instance) {
        AlarmEntityId source;
        if (instance.contains(PORT)) {
            source = AlarmEntityId.alarmEntityId(instance.replace("-", ":"));
        } else if (instance.contains(PTP)) {
            source = AlarmEntityId.alarmEntityId(instance.replace(PTP + "-", PORT + ":"));
        } else {
            source = AlarmEntityId.alarmEntityId(OTHER + ":" + instance);
        }
        return source;
    }

    private long parseAlarmTime(String time) {
        /*
         * expecting WaveServer time to be set to UTC.
         */
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            LocalDateTime localDateTime = LocalDateTime.parse(time, formatter);
            return localDateTime.atZone(ZoneId.of(UTC)).toInstant().toEpochMilli();
        } catch (DateTimeParseException e2) {
            log.error("unable to parse time {}, using system time", time);
            return System.currentTimeMillis();
        }
    }

    private Alarm newAlarmFromJsonNode(JsonNode jsonNode) {
        try {
            AlarmId alarmId = AlarmId.alarmId(checkNotNull(jsonNode.get(ALARM_INSTANCE_ID)).asText());
            String time = checkNotNull(jsonNode.get(ALARM_LOCAL_DATE_TIME)).asText();
            String instance = checkNotNull(jsonNode.get(INSTANCE).asText()).toLowerCase();
            String description = checkNotNull(jsonNode.get(DESCRIPTION)).asText() + " - " + instance + " - " + time;
            AlarmEntityId source = getAlarmSource(instance);
            Alarm.SeverityLevel severity = Alarm.SeverityLevel.valueOf(checkNotNull(
                    jsonNode.get(SEVERITY)).asText().toUpperCase());

            long timeRaised = parseAlarmTime(time);
            boolean isAcknowledged = checkNotNull(jsonNode.get(ACKNOWLEDGE)).asBoolean();

            return new DefaultAlarm.Builder(alarmId, deviceId, description, severity, timeRaised)
                    .withAcknowledged(isAcknowledged)
                    .forSource(source)
                    .build();

        } catch (NullPointerException e) {
            log.error("got exception while parsing alarm json node {} for device {}:\n", jsonNode, deviceId, e);
            return null;
        }

    }

    private List<Alarm> getActiveAlarms() {
        log.debug("getting active alarms for device {}", deviceId);
        try {
            List<JsonNode> alarms = Lists.newArrayList(get(ACTIVE_ALARMS_URL).get(ACTIVE).elements());
            return alarms.stream()
                    .map(this::newAlarmFromJsonNode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("unable to get active alarms for device {}:\n", deviceId, e);
            return null;
        }
    }

    public Collection<FlowEntry> getFlowEntries() {
        List<Port> ports = deviceService.getPorts(deviceId);
        //driver only handles lineSide ports
        //TODO: handle client ports as well
        return ports.stream()
                .filter(p -> LINESIDE_PORT_ID.contains(p.number().name()))
                .map(p -> fetchRule(p.number()))
                .filter(Objects::nonNull)
                .map(fr -> new DefaultFlowEntry(fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                .collect(Collectors.toList());
    }

    private FlowRule fetchRule(PortNumber port) {
        Frequency frequency = getFrequency(port);
        if (frequency == null) {
            return null;
        }
        int channel = getChannelFromFrequency(frequency);
        /*
         * both inPort and outPort will be same as WaveServer only deal with same port ptp-indexes
         * channel and spaceMultiplier are same.
         * TODO: find a way to get both inPort and outPort for future when inPort may not be equal to outPort
         */

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(port)
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .add(Criteria.matchLambda(OchSignal.newDwdmSlot(CHANNEL_SPACING, channel)))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(port)
                .build();

        int hash = Objects.hash(deviceId, selector, treatment);
        Pair<FlowId, Integer> lookup = crossConnectCache.get(hash);
        if (lookup == null) {
            return null;
        }

        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(lookup.getRight())
                .withCookie(lookup.getLeft().value())
                .build();
    }

    public List<Alarm> getAlarms() {
        log.debug("getting alarms for device {}", deviceId);
        return getActiveAlarms();
    }

    private JsonNode get(String uri) throws IOException {
        InputStream response = controller.get(deviceId, uri, MediaType.valueOf(MediaType.APPLICATION_JSON));
        ObjectMapper om = new ObjectMapper();
        final ObjectReader reader = om.reader();
        // all waveserver responses contain data node, which contains the requested data
        return reader.readTree(response).get(DATA);
    }

    private int put(String uri, String request) {
        InputStream payload = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        int response = controller.put(deviceId, uri, payload, MediaType.valueOf(MediaType.APPLICATION_JSON));
        log.debug("response: {}", response);
        return response;
    }

    private boolean putNoReply(String uri, String request) {
        return put(uri, request) == Response.Status.NO_CONTENT.getStatusCode();
    }

}
