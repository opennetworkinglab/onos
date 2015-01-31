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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;

/**
 * Activates an installed application.
 */
@Command(scope = "onos", name = "app-activate",
        description = "Activates an installed application")
public class ApplicationActivateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name", description = "Application name",
            required = true, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        if (appId != null) {
            service.activate(appId);
        } else {
            print("No such application: %s", name);
        }
    }

}
