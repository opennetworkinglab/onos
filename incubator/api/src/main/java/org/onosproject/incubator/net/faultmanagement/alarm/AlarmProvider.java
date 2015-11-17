/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.incubator.net.faultmanagement.alarm;

import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.Provider;

/**
 * Abstraction of a Alarm provider.
 */
public interface AlarmProvider extends Provider {

    /**
     * Triggers an asynchronous discovery of the alarms on the specified device,
     * intended to refresh internal alarm model for the device. An indirect
     * result of this should be invocation of
     * {@link org.onosproject.incubator.net.faultmanagement.alarm.AlarmProviderService#updateAlarmList} )}
     * at some later point in time.
     *
     * @param deviceId ID of device to be probed
     */
    void triggerProbe(DeviceId deviceId);

}
