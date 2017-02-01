/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.intentsync.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.intentsync.IntentSynchronizationAdminService;

/**
 * Command to change whether this instance's intent synchronizer is primary.
 */
@Command(scope = "onos", name = "sdnip-set-primary",
         description = "Changes the primary status of this SDN-IP instance")
public class PrimaryChangeCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "isPrimary",
            description = "True if this instance should be primary, false if not",
            required = true, multiValued = false)
    boolean isPrimary = false;

    @Override
    protected void execute() {
        AbstractShellCommand.get(IntentSynchronizationAdminService.class).modifyPrimary(isPrimary);
    }

}
