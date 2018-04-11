/*
 * Copyright 2017-present Open Networking Foundation
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


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;

import static org.onosproject.segmentrouting.pwaas.PwaasUtil.parsePwId;


/**
 * Command to remove a pseudowire.
 */
@Command(scope = "onos", name = "sr-pw-remove",
        description = "Remove a pseudowire")
public class PseudowireRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "pwId",
            description = "pseudowire ID",
            required = true, multiValued = false)
    String pwId;

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        // remove the pseudowire
        SegmentRoutingManager mngr = (SegmentRoutingManager) srService;
        int pwIntId;
        try {
            pwIntId = parsePwId(pwId);
        } catch (IllegalArgumentException e) {
            log.error("Exception while parsing pseudowire id : \n\t %s", e.getMessage());
            print("Exception while parsing pseudowire id : \n\t %s", e.getMessage());
            return;
        }

        log.info("Removing pseudowire {} from the command line.", pwIntId);
        L2TunnelHandler.Result res = mngr.removePseudowire(pwIntId);
        switch (res) {
            case WRONG_PARAMETERS:
                error("Pseudowire could not be removed , wrong parameters: \n\t %s\n",
                      res.getSpecificError());
                break;
            case INTERNAL_ERROR:
                error("Pseudowire could not be removed, internal error : \n\t %s\n",
                      res.getSpecificError());
                break;
            case SUCCESS:
                break;
            default:
                break;
            }
    }
}