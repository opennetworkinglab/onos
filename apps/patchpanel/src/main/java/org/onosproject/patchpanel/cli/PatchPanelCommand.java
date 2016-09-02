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

/**
 * This class defines the cli command for the PatchPanel class. It creates
 * an instance of the PatchPanelService class to call it's method addPatch().
 * The command takes 2 parameters, 2 connectPoints.
 */
package org.onosproject.patchpanel.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.patchpanel.impl.PatchPanelService;

/**
 * Command for adding a new patch.
 */
@Command(scope = "onos", name = "patch",
         description = "Gets the 2 ports of one ConnectPoint that will be patched")
public class PatchPanelCommand extends AbstractShellCommand {
    //the 2 arguments, both connect points
    @Argument(index = 0, name = "switch/portNumber", description = "ConnectPoint and first port number",
            required = true, multiValued = false)
    String port1 = null;
    @Argument (index = 1, name = "switch/portNumber2", description = "ConnectPoint and second port number",
            required = true, multiValued = false)
    String port2 = null;

    private ConnectPoint cp1, cp2;
    private PatchPanelService patchPanelService;
    private DeviceId deviceId, deviceId2;

    /**
     * This method creates an instance of the Service class and then uses the user
     * input to call the addPatch method in PatchPanel. It also
     * @throws IllegalArgumentException if the 2 connectpoints are of different devices
     */
    @Override
    protected void execute() {
        patchPanelService = get(PatchPanelService.class);
        boolean done = false;
        cp1 = ConnectPoint.deviceConnectPoint(port1);
        cp2 = ConnectPoint.deviceConnectPoint(port2);
        deviceId = cp1.deviceId();
        deviceId2 = cp2.deviceId();
        if (!(deviceId.equals(deviceId2))) {
            throw new IllegalArgumentException("ERROR: Two Different Device Id's");
        } else {
            done = patchPanelService.addPatch(cp1, cp2);
        }
        if (done) {
            log.info("This patch has been created");
        } else {
            log.info("This patch was NOT created");
        }

    }

}
