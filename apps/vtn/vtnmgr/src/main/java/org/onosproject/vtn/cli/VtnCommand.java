/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtn.cli;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtn.manager.impl.VtnManager;

/**
 * Supports for updating the external gateway virtualPort.
 */
@Command(scope = "onos", name = "externalportname-set",
        description = "Supports for setting the external port name.")
public class VtnCommand extends AbstractShellCommand {

    @Option(name = "-n", aliases = "--name", description = "external port name.", required = true,
            multiValued = false)
    String exPortName = "";

    @Override
    protected void execute() {
        VtnManager.setExPortName(exPortName);
    }
}
