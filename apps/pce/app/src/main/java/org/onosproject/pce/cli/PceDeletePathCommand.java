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
package org.onosproject.pce.cli;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

/**
 * Supports deleting pce path.
 */
@Command(scope = "onos", name = "pce-delete-path", description = "Supports deleting pce path.")
public class PceDeletePathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());

    @Argument(index = 0, name = "id", description = "Path Id.", required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        log.info("executing pce-delete-path");

        PceService service = get(PceService.class);

        if (!service.releasePath(TunnelId.valueOf(id))) {
            error("Path deletion failed.");
            return;
        }
    }
}
