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

package org.onosproject.odtn.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Device driver interface for ODTN Phase1.0.
 */
@Beta
public interface OdtnTerminalDeviceDriver {

    enum Operation {
        CREATE("create"),
        MERGE("merge"),
        DELETE("delete");

        private final String value;

        Operation(String op) {
            this.value = op;
        }

        public String value() {
            return this.value;
        }

    }

    /**
     * Configure terminal device.
     *
     * @param did    Device ID
     * @param client side port of transceiver to enable/disable
     * @param line   side port of transceiver to enable/disable
     * @param enable or disable
     */
    void apply(DeviceId did, PortNumber client, PortNumber line, boolean enable);
}
