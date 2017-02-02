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

import java.util.Map;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;
import static org.onosproject.ui.topo.TopoConstants.Properties.*;

/**
 * Our topology overlay.
 */
public class AlarmTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in alarmTopov.js
    private static final String OVERLAY_ID = "alarmsTopo-overlay";

    private static final ButtonId ALARM1_BUTTON = new ButtonId("alarm1button");

    public AlarmTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void modifySummary(PropertyPanel pp) {
        pp.title("Alarms Overview");
        // We could just remove some properties here but lets keep it uncluttered, unless
        //    there is feedback other properties are essential.
        pp.removeAllProps();
        Map<Alarm.SeverityLevel, Long> countsForAll = AlarmServiceUtil.lookUpAlarmCounts();
        addAlarmCountsProperties(pp, countsForAll);

    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.title("Alarm Details");
        pp.removeProps(LATITUDE, LONGITUDE, PORTS, FLOWS, TUNNELS, SERIAL_NUMBER, PROTOCOL);

        Map<Alarm.SeverityLevel, Long> countsForDevice = AlarmServiceUtil.lookUpAlarmCounts(deviceId);
        addAlarmCountsProperties(pp, countsForDevice);

        pp.addButton(ALARM1_BUTTON);

        pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);
    }

    private void addAlarmCountsProperties(PropertyPanel pp, Map<Alarm.SeverityLevel, Long> countsForDevice) {

        // TODO we could show these as color-coded squares with a count inside, to save space on the screen.

        long cr = countsForDevice.getOrDefault(Alarm.SeverityLevel.CRITICAL, 0L);
        long ma = countsForDevice.getOrDefault(Alarm.SeverityLevel.MAJOR, 0L);
        long mi = countsForDevice.getOrDefault(Alarm.SeverityLevel.MINOR, 0L);
        long wa = countsForDevice.getOrDefault(Alarm.SeverityLevel.WARNING, 0L);
        long in = countsForDevice.getOrDefault(Alarm.SeverityLevel.INDETERMINATE, 0L);
        long cl = countsForDevice.getOrDefault(Alarm.SeverityLevel.CLEARED, 0L);

        // Unfortunately the PropertyPanel does not righ justify numbers even when using longs,
        // but that not in scope of fault management work
        pp.addProp("Critical", cr);
        pp.addProp("Major", ma);
        pp.addProp("Minor", mi);
        pp.addProp("Warning", wa);
        pp.addProp("Indeter.", in);
        pp.addProp("Cleared", cl);
        pp.addSeparator();
        pp.addProp("Total", cr + ma + mi + wa + in + cl);

    }

}
