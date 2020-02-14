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
 */
package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.phasedrecovery.api.Phase;
import org.onosproject.segmentrouting.phasedrecovery.api.PhasedRecoveryService;

@Service
@Command(scope = "onos", name = "sr-pr-set", description = "Set recovery phase of given device")

public class PhasedRecoverySetCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String deviceIdStr;

    @Argument(index = 1, name = "phase",
            description = "Recovery phase",
            required = true, multiValued = false)
    @Completion(PhaseCompleter.class)
    private String phaseStr;

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        Phase newPhase = Phase.valueOf(phaseStr);

        PhasedRecoveryService prService = get(PhasedRecoveryService.class);
        prService.setPhase(deviceId, newPhase);
    }
}
