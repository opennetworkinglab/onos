/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmConsumer;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Ciena Wave Server Alarm Consumer.
 */
public class CienaWaveServerAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {
    CienaRestDevice restCiena;

    private final Logger log = getLogger(getClass());

    @Override
    public List<Alarm> consumeAlarms() {
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice:\n", e);
            return null;
        }
        return restCiena.getAlarms();
    }
}
