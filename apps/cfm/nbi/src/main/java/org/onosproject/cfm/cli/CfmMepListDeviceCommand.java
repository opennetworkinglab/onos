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
package org.onosproject.cfm.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cfm.cli.completer.CfmDeviceIdCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.net.DeviceId;

/**
 * Lists all the MEPs on a particular device.
 */
@Service
@Command(scope = "onos", name = "cfm-mep-device-list",
        description = "Lists a set of MEPs filtered by device.")
public class CfmMepListDeviceCommand extends AbstractShellCommand {
    @Argument(name = "device",
            description = "Device Id",
            required = true)
    @Completion(CfmDeviceIdCompleter.class)
    private String deviceStr = null;

    @Override
    protected void doExecute() {
        CfmMepService mepService = get(CfmMepService.class);
        if (deviceStr != null) {
            DeviceId deviceId = DeviceId.deviceId(deviceStr);
            try {
                mepService.getAllMepsByDevice(deviceId)
                        .forEach(mep -> print(printMep(mep)));
            } catch (CfmConfigException e) {
                log.error("Error retrieving Meps for Device {}",
                        deviceId, e);
            }
        }
    }

    /**
     * Print only the config part of the MEP.
     * @param mep The MEP to print
     * @return A string with MD name, MA name and Mep details
     */
    private static String printMep(Mep mep) {
         return "MEP: " + mep.mdId().mdName() + "/" + mep.maId().maName() + "/" +
                 mep.mepId() + " Device:" + mep.deviceId() + ", Port: " +
                 mep.port() + ", Vlan: " + mep.primaryVid() + ", AdminSt: " +
                 mep.administrativeState() + ", CciEnabled: " + mep.cciEnabled() +
                 ", Priority: " + mep.ccmLtmPriority();
    }

}
