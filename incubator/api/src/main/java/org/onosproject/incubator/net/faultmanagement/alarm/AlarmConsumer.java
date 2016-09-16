/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;

/**
 * Abstraction of a device behaviour capable of retrieving/consuming list of
 * pending alarms from the device.
 */
public interface AlarmConsumer extends HandlerBehaviour {

    /**
     * Returns the list of active alarms consumed from the device.
     * This means that subsequent retrieval of alarms will not contain
     * any duplicates.
     *
     * @return list of alarms consumed from the device
     */
    List<Alarm> consumeAlarms();

}
