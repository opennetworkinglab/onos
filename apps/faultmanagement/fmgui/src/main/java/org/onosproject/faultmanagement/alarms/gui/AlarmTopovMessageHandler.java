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
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
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

/**
 * FaultManagement UI Topology-Overlay message handler.
 */
public class AlarmTopovMessageHandler extends UiMessageHandler {

    private static final String ALARM_TOPOV_DISPLAY_START = "alarmTopovDisplayStart";
    private static final String ALARM_TOPOV_DISPLAY_STOP = "alarmTopovDisplayStop";
    private static final int DELAY_MS = 500;


    private final Logger log = LoggerFactory.getLogger(getClass());

    protected AlarmService alarmService;
    private DeviceService deviceService;
    private AlarmMonitor alarmMonitor;

    // =======================================================================
    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
        alarmService = directory.get(AlarmService.class);
        alarmMonitor = new AlarmMonitor(this);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
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
        public void process(ObjectNode payload) {
            log.debug("Start Display");
            sendDelayedAlarmHighlights();
            alarmMonitor.startMonitorig();
        }
    }

    private final class DisplayStopHandler extends RequestHandler {

        public DisplayStopHandler() {
            super(ALARM_TOPOV_DISPLAY_STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("Stop Display");
            alarmMonitor.stopMonitoring();
            clearHighlights();
        }
    }

    /**
     * Sends the highlights with a delay to the client side on the browser.
     */
    protected void sendDelayedAlarmHighlights() {
        createAndSendHighlights(true);
    }

    /**
     * Sends the highlights to the client side on the browser.
     */
    protected void sendAlarmHighlights() {
        createAndSendHighlights(false);
    }

    private void createAndSendHighlights(boolean toDelay) {
        Highlights highlights = new Highlights();
        createBadges(highlights);
        if (toDelay) {
            highlights.delay(DELAY_MS);
        }
        sendHighlights(highlights);
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }

    private void createBadges(Highlights highlights) {
        deviceService.getAvailableDevices().forEach(d -> {
            Set<Alarm> alarmsOnDevice = alarmService.getAlarms(d.id());
            int alarmSize = alarmsOnDevice.size();
            log.debug("{} Alarms on device {}", alarmSize, d.id());
            if (alarmSize > 0) {
                addDeviceBadge(highlights, d.id(), alarmSize);
            }
        });
    }

    private void clearHighlights() {
        sendHighlights(new Highlights());
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
