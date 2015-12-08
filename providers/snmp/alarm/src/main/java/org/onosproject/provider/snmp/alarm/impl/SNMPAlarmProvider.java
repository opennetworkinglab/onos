/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.provider.snmp.alarm.impl;

import org.apache.felix.scr.annotations.Component;
import org.onosproject.net.DeviceId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmProvider;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an SNMP controller to detect network device alarms. The class leverages functionality from
 *
 * @see <a href="https://github.com/btisystems/snmp-core">https://github.com/btisystems/snmp-core</a>
 * @see <a href="https://github.com/btisystems/mibbler">https://github.com/btisystems/mibbler</a>
 */
@Component(immediate = true)
public class SNMPAlarmProvider extends AbstractProvider implements AlarmProvider {

    private static final Logger LOG = getLogger(SNMPAlarmProvider.class);

    /**
     * Creates a SNMP alarm provider, dummy class provided as template, tbd later.
     */
    public SNMPAlarmProvider() {
        super(new ProviderId("snmp", "org.onosproject.provider.alarm"));
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {

        // TODO in shout term should this just be synchronous and return result?
        LOG.info("Run a SNMP discovery for device at {} when done invoke on AlarmProviderService", deviceId);

        // TODO Look up AlarmProviderService
        // TODO Decide threading
        // TODO Decide shouldn't it be generic not alarm-specific ? Its user responsible for passing in OID list ?
        // Same for its callack AlarmProviderService ?
    }

}
