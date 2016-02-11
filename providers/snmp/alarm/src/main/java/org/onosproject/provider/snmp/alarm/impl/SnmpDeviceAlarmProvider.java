/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.provider.snmp.alarm.impl;

import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import java.util.Collection;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.net.DeviceId;

/**
 * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
 */
@Deprecated
public interface SnmpDeviceAlarmProvider {
    /**
     * Implemented by device specific implementations which query the current
     * alarms from a device.
     * @deprecated 1.5.0 Falcon
     * @param snmpSession SNMP Session
     * @param deviceId device identifier
     * @return device alarms
     */
    @Deprecated
    Collection<Alarm> getAlarms(ISnmpSession snmpSession, DeviceId deviceId);
}
