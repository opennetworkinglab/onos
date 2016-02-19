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

import com.btisystems.mibbler.mibs.netsnmp.netsnmp.I_Device;
import com.btisystems.mibbler.mibs.netsnmp.netsnmp._OidRegistry;
import com.btisystems.pronx.ems.core.model.ClassRegistry;
import com.btisystems.pronx.ems.core.model.IClassRegistry;
import com.btisystems.pronx.ems.core.model.NetworkDevice;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import org.apache.commons.lang.StringUtils;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.slf4j.Logger;
import org.snmp4j.smi.OID;

import java.io.IOException;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A agent-specific implementation supporting NET-SNMP agents.
 * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
 */
@Deprecated
public class NetSnmpDeviceDescriptionProvider implements SnmpDeviceDescriptionProvider {
    private final Logger log = getLogger(getClass());
    protected static final IClassRegistry CLASS_REGISTRY =
            new ClassRegistry(_OidRegistry.oidRegistry, I_Device.class);
    private static final String UNKNOWN = "unknown";

    @Override
    public DeviceDescription populateDescription(ISnmpSession session, DeviceDescription description) {
        NetworkDevice networkDevice = new NetworkDevice(CLASS_REGISTRY,
                session.getAddress().getHostAddress());
        try {
            session.walkDevice(networkDevice, Arrays.asList(new OID[]{
                CLASS_REGISTRY.getClassToOidMap().get(
                com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.System.class)}));

            com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.System systemTree =
                    (com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.System)
                    networkDevice.getRootObject().getEntity(CLASS_REGISTRY.getClassToOidMap().get(
                                    com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.System.class));
            if (systemTree != null) {
                // TODO SNMP sys-contacts may be verbose; ONOS-GUI doesn't abbreviate fields neatly;
                // so cut it here until supported in prop displayer
                String manufacturer = StringUtils.abbreviate(systemTree.getSysContact(), 20);
                return new DefaultDeviceDescription(description.deviceUri(), description.type(), manufacturer,
                        UNKNOWN, UNKNOWN, UNKNOWN, description.chassisId(), description.annotations());
            }
        } catch (IOException ex) {
            log.error("Error reading details for device {}.", session.getAddress(), ex);
        }
        return description;
    }

}
