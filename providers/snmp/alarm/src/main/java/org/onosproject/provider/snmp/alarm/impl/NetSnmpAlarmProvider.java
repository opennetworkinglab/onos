/*
 *
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
package org.onosproject.provider.snmp.alarm.impl;

import com.btisystems.mibbler.mibs.netsnmp.netsnmp.I_Device;
import com.btisystems.mibbler.mibs.netsnmp.netsnmp._OidRegistry;
import com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.interfaces.IfTable;
import com.btisystems.pronx.ems.core.model.ClassRegistry;
import com.btisystems.pronx.ems.core.model.IClassRegistry;
import com.btisystems.pronx.ems.core.model.NetworkDevice;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.snmp4j.smi.OID;

/**
 * Net SNMP specific implementation to provide a list of current alarms.
 * @deprecated 1.5.0 Falcon, not compliant with ONOS SB and driver architecture.
 */
@Deprecated
public class NetSnmpAlarmProvider implements SnmpDeviceAlarmProvider {
    private final Logger log = getLogger(getClass());
    protected static final IClassRegistry CLASS_REGISTRY =
            new ClassRegistry(_OidRegistry.oidRegistry, I_Device.class);
    @Override
    public Collection<Alarm> getAlarms(ISnmpSession session, DeviceId deviceId) {
        Set<Alarm> alarms = new HashSet<>();

        NetworkDevice networkDevice = new NetworkDevice(CLASS_REGISTRY,
                session.getAddress().getHostAddress());
        try {
            session.walkDevice(networkDevice, Arrays.asList(new OID[]{
                CLASS_REGISTRY.getClassToOidMap().get(IfTable.class)}));

            IfTable interfaceTable = (IfTable) networkDevice.getRootObject()
                    .getEntity(CLASS_REGISTRY.getClassToOidMap().get(IfTable.class));
            if (interfaceTable != null) {
                interfaceTable.getEntries().values().stream().forEach((ifEntry) -> {
                    //TODO will raise alarm for each interface as a demo.
    //                if (ifEntry.getIfAdminStatus() == 1 && ifEntry.getIfOperStatus() == 2){
                        alarms.add(new DefaultAlarm.Builder(deviceId, "Link Down.",
                                Alarm.SeverityLevel.CRITICAL, System.currentTimeMillis())
                                .forSource(AlarmEntityId.alarmEntityId("port:" + ifEntry.getIfDescr())).build());
    //                }
                    log.info("Interface: " + ifEntry);
                });
            }
        } catch (IOException ex) {
            log.error("Error reading alarms for device {}.", deviceId, ex);
        }
        return alarms;
    }
}
