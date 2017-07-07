/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.artemis.cli;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.artemis.impl.ArtemisService;
import org.onosproject.cli.AbstractShellCommand;

/**
 * CLI to enable or disable BGP Update message logging.
 */
@Command(scope = "artemis", name = "log-messages",
    description = "Show RIS messages in logger.")
public class LogOptionsCommand extends AbstractShellCommand {

    @Option(name = "--enable", aliases = "-e", description = "Enable RIS message logging",
            required = false, multiValued = false)
    private boolean enable = false;

    @Option(name = "--disable", aliases = "-d", description = "Disable RIS message logging",
            required = false, multiValued = false)
    private boolean disable = false;

    @Override
    protected void execute() {
        ArtemisService artemisService = get(ArtemisService.class);
        if (enable) {
            artemisService.setLogger(true);
        } else if (disable) {
            artemisService.setLogger(false);
        }
    }
}