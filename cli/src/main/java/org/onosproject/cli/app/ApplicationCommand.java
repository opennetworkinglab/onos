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
package org.onosproject.cli.app;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages application inventory.
 */
@Command(scope = "onos", name = "app",
        description = "Manages application inventory")
public class ApplicationCommand extends AbstractShellCommand {

    static final String INSTALL = "install";
    static final String UNINSTALL = "uninstall";
    static final String ACTIVATE = "activate";
    static final String DEACTIVATE = "deactivate";

    @Argument(index = 0, name = "command",
            description = "Command name (install|activate|deactivate|uninstall)",
            required = true, multiValued = false)
    String command = null;

    @Argument(index = 1, name = "names", description = "Application name(s) or URL(s)",
            required = true, multiValued = true)
    String[] names = null;

    @Override
    protected void execute() {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        if (command.equals(INSTALL)) {
            for (String name : names) {
                if (!installApp(service, name)) {
                    return;
                }
            }

        } else {
            for (String name : names) {
                if (!manageApp(service, name)) {
                    return;
                }
            }
        }
    }

    // Installs the application from input of the specified URL
    private boolean installApp(ApplicationAdminService service, String url) {
        try {
            if ("-".equals(url)) {
                service.install(System.in);
            } else {
                service.install(new URL(url).openStream());
            }
        } catch (IOException e) {
            error("Unable to get URL: %s", url);
            return false;
        }
        return true;
    }

    // Manages the specified application.
    private boolean manageApp(ApplicationAdminService service, String name) {
        ApplicationId appId = service.getId(name);
        if (appId == null) {
            List<Application> matches = service.getApplications().stream()
                .filter(app -> app.id().name().matches(".*\\." + name + "$"))
                .collect(Collectors.toList());

            if (matches.size() == 1) {
                // Found match
                appId = matches.iterator().next().id();
            } else if (!matches.isEmpty()) {
                print("Did you mean one of: %s",
                      matches.stream()
                          .map(Application::id)
                          .map(ApplicationId::name)
                          .collect(Collectors.toList()));
                return false;
            }
        }
        if (appId == null) {
            print("No such application: %s", name);
            return false;
        }

        String action;
        if (command.equals(UNINSTALL)) {
            service.uninstall(appId);
            action = "Uninstalled";
        } else if (command.equals(ACTIVATE)) {
            service.activate(appId);
            action = "Activated";
        } else if (command.equals(DEACTIVATE)) {
            service.deactivate(appId);
            action = "Deactivated";
        } else {
            print("Unsupported command: %s", command);
            return false;
        }
        print("%s %s", action, appId.name());
        return true;
    }

}
