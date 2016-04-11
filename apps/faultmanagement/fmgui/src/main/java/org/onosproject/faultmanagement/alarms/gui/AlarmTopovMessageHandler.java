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
package org.onosproject.faultmanagement.alarms.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.NodeBadge.Status;
import org.onosproject.ui.topo.TopoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;

/**
 * FaultManagement UI Topology-Overlay message handler.
 */
public class AlarmTopovMessageHandler extends UiMessageHandler {

    private static final String ALARM_TOPOV_DISPLAY_START = "alarmTopovDisplayStart";
    private static final String ALARM_TOPOV_DISPLAY_UPDATE = "alarmTopovDisplayUpdate";
    private static final String ALARM_TOPOV_DISPLAY_STOP = "alarmTopovDisplayStop";

    private static final String ID = "id";
    private static final String MODE = "mode";

    private enum Mode {

        IDLE, MOUSE
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;
    private HostService hostService;
    private AlarmService alarmService;

    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;

    // ===============-=-=-=-=-=-======================-=-=-=-=-=-=-================================
    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
        hostService = directory.get(HostService.class);
        alarmService = directory.get(AlarmService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
                new DisplayUpdateHandler(),
                new DisplayStopHandler()
        );
    }

    // === -------------------------
    // === Handler classes
    private final class DisplayStartHandler extends RequestHandler {

        public DisplayStartHandler() {
            super(ALARM_TOPOV_DISPLAY_START);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String mode = string(payload, MODE);

            log.debug("Start Display: mode [{}]", mode);
            clearState();
            clearForMode();

            switch (mode) {
                case "mouse":
                    currentMode = Mode.MOUSE;

                    sendMouseData();
                    break;

                default:
                    currentMode = Mode.IDLE;

                    break;
            }
        }
    }

    private final class DisplayUpdateHandler extends RequestHandler {

        public DisplayUpdateHandler() {
            super(ALARM_TOPOV_DISPLAY_UPDATE);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String id = string(payload, ID);
            log.debug("Update Display: id [{}]", id);
            if (!Strings.isNullOrEmpty(id)) {
                updateForMode(id);
            } else {
                clearForMode();
            }
        }
    }

    private final class DisplayStopHandler extends RequestHandler {

        public DisplayStopHandler() {
            super(ALARM_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            log.debug("Stop Display");
            clearState();
            clearForMode();
        }
    }

    // === ------------
    private void clearState() {
        currentMode = Mode.IDLE;
        elementOfNote = null;
    }

    private void updateForMode(String id) {
        log.debug("host service: {}", hostService);
        log.debug("device service: {}", deviceService);

        try {
            HostId hid = HostId.hostId(id);
            log.debug("host id {}", hid);
            elementOfNote = hostService.getHost(hid);
            log.debug("host element {}", elementOfNote);

        } catch (RuntimeException e) {
            try {
                DeviceId did = DeviceId.deviceId(id);
                log.debug("device id {}", did);
                elementOfNote = deviceService.getDevice(did);
                log.debug("device element {}", elementOfNote);

            } catch (RuntimeException e2) {
                log.debug("Unable to process ID [{}]", id);
                elementOfNote = null;
            }
        }

        switch (currentMode) {
            case MOUSE:
                sendMouseData();
                break;

            default:
                break;
        }

    }

    private void clearForMode() {
        sendHighlights(new Highlights());
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }

    private void sendMouseData() {
        if (elementOfNote != null && elementOfNote instanceof Device) {
            DeviceId devId = (DeviceId) elementOfNote.id();
            Set<Alarm> alarmsOnDevice = alarmService.getAlarms(devId);
            Highlights highlights = new Highlights();

            addDeviceBadge(highlights, devId, alarmsOnDevice.size());
            sendHighlights(highlights);
        }
        // Note: could also process Host, if available
    }

    private void addDeviceBadge(Highlights h, DeviceId devId, int n) {
        DeviceHighlight dh = new DeviceHighlight(devId.toString());
        dh.setBadge(createBadge(n));
        h.add(dh);
    }

    private NodeBadge createBadge(int n) {
        Status status = n > 0 ? Status.ERROR : Status.INFO;
        String noun = n > 0 ? "(Alarmed)" : "(Normal)";
        String msg = "Alarms: " + n + " " + noun;
        return NodeBadge.number(status, n, msg);
    }

}
