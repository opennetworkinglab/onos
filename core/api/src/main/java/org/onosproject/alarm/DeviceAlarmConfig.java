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

package org.onosproject.alarm;

import org.onlab.packet.IpAddress;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;
import java.util.Set;

/**
 * Abstraction of a device behaviour capable of translating a list of
 * alarms from a device. Also provides a method to configure the address where to
 * send the alarms/traps/notifications.
 */
public interface DeviceAlarmConfig extends HandlerBehaviour {

    /**
     * Configures the device to send alarms to a particular Ip and port combination.
     *
     * @param address  address to wich the device should send alarms
     * @param port     port on which the controller is listening
     * @param protocol tcp or udp
     * @return boolean true if the device was properly configured
     */
    boolean configureDevice(IpAddress address, int port, String protocol);

    /**
     * Returns the list of translated alarms from device-specific representation
     * to ONOS alarms.
     *
     * @param unparsedAlarms alarms arrived from the device depending on protocol
     * @param <T> type of object given from the device
     * @return list of alarms consumed from the device
     */
    <T> Set<Alarm> translateAlarms(List<T> unparsedAlarms);

}
