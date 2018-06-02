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

package org.onosproject.odtn.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.odtn.utils.tapi.TapiNepRef;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TerminalDeviceConfig extends BaseConfig<ConnectPoint> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * {@value #CONFIG_KEY} : a netcfg ConfigKey for {@link TerminalDeviceConfig}.
     */
    public static final String CONFIG_KEY = "odtn-terminal-device";

    private static final String CLIENT_PORT = "client";
    private static final String ENABLE = "enable";

    /**
     * Create a TerminalDeviceConfig for ODTN.
     */
    public TerminalDeviceConfig() {
        super();
    }

    /**
     * Create a TerminalDeviceConfig for ODTN.
     *
     * @param cp ConnectPoint
     */
    public TerminalDeviceConfig(ConnectPoint cp) {
        ObjectMapper mapper = new ObjectMapper();
        init(cp, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }

    @Override
    public boolean isValid() {
        return isConnectPoint(CLIENT_PORT, FieldPresence.MANDATORY) &&
                isBoolean(ENABLE, FieldPresence.MANDATORY);
    }

    public ConnectPoint clientCp() {
        String cp = get(CLIENT_PORT, "");
        return ConnectPoint.deviceConnectPoint(cp);
    }

    public TerminalDeviceConfig clientCp(ConnectPoint cp) {
        String val = String.format("%s/%d", cp.deviceId(), cp.port().toLong());
        setOrClear(CLIENT_PORT, val);
        return this;
    }

    public Boolean isEnabled() {
        return get(ENABLE, false);
    }

    public TerminalDeviceConfig enable() {
        setOrClear(ENABLE, true);
        return this;
    }

    public TerminalDeviceConfig disable() {
        setOrClear(ENABLE, false);
        return this;
    }

    /**
     * Factory method in order to emit NetCfg event from onos inner call.
     *
     * @param line   side NodeEdgePoint of connection
     * @param client side NodeEdgePoint of connection
     * @return Config object for NetCfg
     */
    public static TerminalDeviceConfig create(TapiNepRef line, TapiNepRef client) {

        if (line.getPortType() != LINE) {
            throw new IllegalArgumentException("Argument line must be a LINE type.");
        }
        if (client.getPortType() != CLIENT) {
            throw new IllegalArgumentException("Argument client must be a CLIENT type.");
        }

        TerminalDeviceConfig self = new TerminalDeviceConfig(line.getConnectPoint());
        self.clientCp(client.getConnectPoint());
        self.enable();
        return self;
    }
}
