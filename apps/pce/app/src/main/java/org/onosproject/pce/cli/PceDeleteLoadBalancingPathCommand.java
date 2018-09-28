/*
 * Copyright 2016-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

/**
 * Supports deleting pce load balancing path.
 */
@Service
@Command(scope = "onos", name = "pce-delete-load-balancing-path",
        description = "Supports deleting pce load balancing path.")
public class PceDeleteLoadBalancingPathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());

    @Argument(index = 0, name = "name", description = "load balancing path name", required = true,
            multiValued = false)
    String name = null;

    @Override
    protected void doExecute() {
        log.info("executing pce-delete-load-balancing-path");

        PceService service = get(PceService.class);

        if (!service.releasePath(name)) {
            error("Path deletion failed.");
            return;
        }
    }
}
