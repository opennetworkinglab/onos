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
package org.onosproject.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.upgrade.UpgradeAdminService;
import org.onosproject.upgrade.UpgradeService;

/**
 * Commands for managing upgrades.
 */
@Service
@Command(scope = "onos", name = "issu",
        description = "Manages upgrades")
public class IssuCommand extends AbstractShellCommand {

    static final String INIT = "init";
    static final String UPGRADE = "upgrade";
    static final String COMMIT = "commit";
    static final String ROLLBACK = "rollback";
    static final String RESET = "reset";
    static final String STATUS = "status";
    static final String VERSION = "version";

    @Argument(index = 0, name = "command",
            description = "Command name (init|upgrade|commit|rollback|reset|status|version)",
            required = false, multiValued = false)
    String command = null;

    @Override
    protected void doExecute() {
        UpgradeService upgradeService = get(UpgradeService.class);
        UpgradeAdminService upgradeAdminService = get(UpgradeAdminService.class);
        if (command == null) {
            print("source=%s, target=%s, status=%s, upgraded=%b, active=%b",
                    upgradeService.getState().source(),
                    upgradeService.getState().target(),
                    upgradeService.getState().status(),
                    upgradeService.isLocalUpgraded(),
                    upgradeService.isLocalActive());
        } else if (command.equals(INIT)) {
            upgradeAdminService.initialize();
            print("Initialized");
        } else if (command.equals(UPGRADE)) {
            upgradeAdminService.upgrade();
            print("Upgraded");
        } else if (command.equals(COMMIT)) {
            upgradeAdminService.commit();
            print("Committed version %s", upgradeService.getVersion());
        } else if (command.equals(ROLLBACK)) {
            upgradeAdminService.rollback();
            print("Rolled back to version %s", upgradeService.getVersion());
        } else if (command.equals(RESET)) {
            upgradeAdminService.reset();
            print("Reset version %s", upgradeService.getVersion());
        } else if (command.equals(STATUS)) {
            print("%s", upgradeService.getState().status());
        } else if (command.equals(VERSION)) {
            print("%s", upgradeService.getVersion());
        } else {
            print("Unsupported command: %s", command);
        }
    }

}
