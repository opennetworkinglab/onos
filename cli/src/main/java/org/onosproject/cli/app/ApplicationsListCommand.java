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
package org.onosproject.cli.app;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.app.ApplicationService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.Application;

import static org.onosproject.app.ApplicationState.ACTIVE;

/**
 * Lists application information.
 */
@Command(scope = "onos", name = "apps",
        description = "Lists application information")
public class ApplicationsListCommand extends AbstractShellCommand {

    private static final String FMT =
            "%s id=%d, name=%s, version=%s, origin=%s, description=%s, " +
                    "features=%s, featuresRepo=%s, permissions=%s";

    @Override
    protected void execute() {
        ApplicationService service = get(ApplicationService.class);
        for (Application app : service.getApplications()) {
            print(FMT, service.getState(app.id()) == ACTIVE ? "*" : " ",
                  app.id().id(), app.id().name(), app.version(), app.origin(),
                  app.description(), app.features(), app.featuresRepo(), app.permissions());
        }
    }

}
