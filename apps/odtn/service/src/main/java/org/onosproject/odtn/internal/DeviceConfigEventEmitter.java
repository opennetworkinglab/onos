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

package org.onosproject.odtn.internal;

import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.odtn.config.TerminalDeviceConfig;
import org.onosproject.odtn.utils.tapi.TapiNepRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

public final class DeviceConfigEventEmitter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private NetworkConfigService netcfgService;

    private DeviceConfigEventEmitter() {
    }

    public static DeviceConfigEventEmitter create() {
        DeviceConfigEventEmitter self = new DeviceConfigEventEmitter();
        self.netcfgService = getService(NetworkConfigService.class);
        return self;
    }

    /**
     * Emit NetworkConfig event with parameters for device config.
     *
     * @param line   side NodeEdgePoint of connection
     * @param client side NodeEdgePoint of connection
     * @param enable or disable
     */
    public void emit(TapiNepRef line, TapiNepRef client, boolean enable) {

        // FIXME Config class should be implemented as behaviour to support
        //       multi device types.
        TerminalDeviceConfig cfg = TerminalDeviceConfig.create(line, client);
        if (enable) {
            cfg.enable();
        } else {
            cfg.disable();
        }
        netcfgService.applyConfig(line.getConnectPoint(), TerminalDeviceConfig.class, cfg.node());
    }

}
