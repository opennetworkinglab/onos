/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.bti;

import com.btisystems.mibbler.mibs.netsnmp.netsnmp.I_Device;
import com.btisystems.mibbler.mibs.netsnmp.netsnmp._OidRegistry;
import com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.interfaces.IfTable;
import com.btisystems.pronx.ems.core.model.ClassRegistry;
import com.btisystems.pronx.ems.core.model.IClassRegistry;
import com.btisystems.pronx.ems.core.model.NetworkDevice;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.google.common.collect.ImmutableList;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmConsumer;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.snmp.SnmpController;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Net SNMP specific implementation to provide a list of current alarms.
 */
public class NetSnmpAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {
    private final Logger log = getLogger(getClass());
    protected static final IClassRegistry CLASS_REGISTRY =
            new ClassRegistry(_OidRegistry.oidRegistry, I_Device.class);

    @Override
    public List<Alarm> consumeAlarms() {
        SnmpController controller = checkNotNull(handler().get(SnmpController.class));
        List<Alarm> alarms = new ArrayList<>();
        ISnmpSession session;
        DeviceId deviceId = handler().data().deviceId();
        try {
            session = controller.getSession(deviceId);

            NetworkDevice networkDevice = new NetworkDevice(CLASS_REGISTRY,
                                                            session.getAddress()
                                                                    .getHostAddress());
            session.walkDevice(networkDevice, Collections.singletonList(
                    CLASS_REGISTRY.getClassToOidMap().get(IfTable.class)));

            IfTable interfaceTable = (IfTable) networkDevice.getRootObject()
                    .getEntity(CLASS_REGISTRY.getClassToOidMap().get(IfTable.class));
            if (interfaceTable != null) {
                interfaceTable.getEntries().values().forEach((ifEntry) -> {
                    if (ifEntry.getIfAdminStatus() == 1 && ifEntry.getIfOperStatus() == 2) {
                        alarms.add(new DefaultAlarm.Builder(deviceId, "Link Down.",
                                                            Alarm.SeverityLevel.CRITICAL,
                                                            System.currentTimeMillis())
                                           .forSource(AlarmEntityId
                                                              .alarmEntityId("port:" + ifEntry.
                                                                      getIfDescr())).build());
                    }
                    log.debug("Interface: " + ifEntry);
                });
            }
        } catch (IOException ex) {
            log.error("Error reading alarms for device {}.", deviceId, ex);
            alarms.add(controller.buildWalkFailedAlarm(deviceId));
        }
        return ImmutableList.copyOf(alarms);
    }
}
