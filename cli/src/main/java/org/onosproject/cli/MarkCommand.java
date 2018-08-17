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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * CLI command which just log message to ONOS log for ease of debugging, etc.
 */
@Service
@Command(scope = "onos", name = "mark",
         description = "Mark message in the log")
public class MarkCommand extends AbstractShellCommand {

    private static final String MARK = "--MARK--";

    @Option(name = "--impersonate",
            description = "Logger to use when logging this message",
            required = false, multiValued = false)
    String loggerName = "org.onosproject.cli.MarkCommand";

    // TODO Option to specify log level


    @Argument(index = 0, name = "message", description = "Message to log",
            required = false, multiValued = true, valueToShowInHelp = MARK)
    List<String> message = ImmutableList.of(MARK);

    @Override
    protected void doExecute() {

        Logger log = getLogger(loggerName);

        log.info("{}", message.stream().collect(Collectors.joining(" ")));

    }

}
