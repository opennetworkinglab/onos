/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.provider.snmp.device.impl;

import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import org.onosproject.net.device.DeviceDescription;

/**
 * Abstraction of an entity which updates a device description with information retrieved via SNMP.
 * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
 */
@Deprecated
public interface SnmpDeviceDescriptionProvider {

    /**
     * Generated an updated device description.
     * @deprecated 1.5.0 Falcon
     * @param session SNMP session
     * @param description old device description
     * @return new updated description
     */
    @Deprecated
    DeviceDescription populateDescription(ISnmpSession session, DeviceDescription description);
}
