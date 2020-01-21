/*
 * Copyright 2020-present Open Networking Foundation
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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.Range;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.drivers.odtn.openconfig.TerminalDevicePowerConfig;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.netconf.DatastoreId;

import java.util.Optional;

/**
 * Driver Implementation of the PowerConfig for OpenConfig terminal devices.
 */
public class GrooveOpenConfigDevicePowerConfig<T>
        extends TerminalDevicePowerConfig<T> implements PowerConfig<T> {

    /**
     * Construct a rpc target power message.
     *
     * @param filter to build rpc
     * @return RPC payload
     */
    @Override
    public StringBuilder getTargetPowerRequestRpc(String filter) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<get>")
                .append("<filter type='subtree'>")
                .append(filter)
                .append("</filter>")
                .append("</get>");
        return rpc;
    }

    /**
     * Construct a rpc target power message.
     *
     * @return RPC payload
     */
    @Override
    public DatastoreId getDataStoreId() {
        return DatastoreId.RUNNING;
    }

    /**
     * Construct a rpc target power message.
     *
     * @param name for optical channel name
     * @param power to build rpc for target output power configuration
     * @return RPC payload
     */
    @Override
    public StringBuilder parsePortRequestRpc(Double power, String name) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<component>").append("<name>").append(name).append("</name>");
        if (power != null) {
            // This is an edit-config operation.
            rpc.append("<optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">")
                    .append("<config>")
                    .append("<target-output-power>")
                    .append(power)
                    .append("</target-output-power>")
                    .append("</config>")
                    .append("</optical-channel>");
        }
        return rpc;
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, Object component) {
        // Only line ports have power
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        DeviceId deviceId = did();
        if (deviceService.getPort(deviceId, port).type() == Port.Type.OCH) {
            return Optional.of(Range.open(-20., 6.));
        }
        return Optional.empty();
    }
}
