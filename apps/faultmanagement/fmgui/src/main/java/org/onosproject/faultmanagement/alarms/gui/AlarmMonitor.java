/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEvent;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmListener;

/**
 * Encapsulates the behavior of monitoring when new alarms are stored.
 */
public class AlarmMonitor {

    private final AlarmTopovMessageHandler messageHandler;
    private final AlarmListener alarmListener = new TopoAlarmListener();
    private volatile boolean isMonitoring;


    /**
     * Constructs a traffic monitor.
     *
     * @param msgHandler the alarm message handler
     */
    public AlarmMonitor(AlarmTopovMessageHandler msgHandler) {
        messageHandler = msgHandler;
        messageHandler.alarmService.addListener(alarmListener);
    }

    /**
     * Activates updating the alarms onf the GUI client.
     */
    protected void startMonitorig() {
        isMonitoring = true;
    }

    /**
     * De-activates updating the alarms onf the GUI client.
     */
    protected void stopMonitoring() {
        isMonitoring = false;
    }

    //internal alarm listener
    private class TopoAlarmListener implements AlarmListener {

        @Override
        public void event(AlarmEvent event) {
            if (isMonitoring) {
                messageHandler.sendAlarmHighlights();
            }
        }
    }
}
