/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This Work is contributed by Sterlite Technologies
 */
package org.onosproject.drivers.odtn;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Driver Implementation of the BitErrorRateConfig for Cassini terminal devices.
 */
public class CassiniBitErrorRateState extends OpenconfigBitErrorRateState {

    private static final Logger log = LoggerFactory.getLogger(CassiniBitErrorRateState.class);

    private static final String OC_NAME = "oc-name";
    private static final String PORT_NOT_PRESENT = "Port is not present";

    /*
     * This method is used for getting Cassini Component name.
     * from DeviceService port annotations by key ("oc-name")
     *
     * @param deviceId the device identifier
     * @param port the port identifier
     * @return String value representing cassini component name
     */
    @Override
    protected String ocName(DeviceId deviceId, PortNumber port) {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        if (deviceService.getPort(deviceId, port) == null) {
            throw new IllegalArgumentException(PORT_NOT_PRESENT);
        }
        return deviceService.getPort(deviceId, port).annotations().value(OC_NAME);
    }
}
