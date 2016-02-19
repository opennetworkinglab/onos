/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.provider.snmp.device.impl;

import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import org.onosproject.net.Device;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device description provider for Lumentum SDN ROADMs.
 * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
 */
@Deprecated
public class LumentumDeviceDescriptionProvider implements SnmpDeviceDescriptionProvider {

    private static final Logger log = LoggerFactory.getLogger(LumentumDeviceDescriptionProvider.class);

    @Override
    public DeviceDescription populateDescription(ISnmpSession session, DeviceDescription description) {
        return new DefaultDeviceDescription(description.deviceUri(), Device.Type.ROADM,
                "Lumentum", "SDN ROADM", "1.0", "v1", description.chassisId(), description.annotations());
    }
}
