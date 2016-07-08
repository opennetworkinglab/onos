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

package org.onosproject.cli.security;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.security.SecurityAdminService;
import org.onosproject.security.SecurityUtil;

import java.security.Permission;
import java.util.List;
import java.util.Map;


/**
 * Application security policy review commands.
 */
@Command(scope = "onos", name = "review",
        description = "Application security policy review interface")
public class ReviewCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name", description = "Application name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "accept", description = "Option to accept policy",
            required = false, multiValued = false)
    String accept = null;


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    @Override
    protected void execute() {
        ApplicationAdminService applicationAdminService = get(ApplicationAdminService.class);
        ApplicationId appId = applicationAdminService.getId(name);
        if (appId == null) {
            print("No such application: %s", name);
            return;
        }
        Application app = applicationAdminService.getApplication(appId);
        SecurityAdminService smService = SecurityUtil.getSecurityService();
        if (smService == null) {
            print("Security Mode is disabled");
            return;
        }
        if (accept == null) {
            smService.review(appId);
            printPolicy(smService, app);
        } else if (accept.trim().equals("accept")) {
            smService.acceptPolicy(appId);
            printPolicy(smService, app);
        } else {
            print("Unknown command");
        }
    }

    private void printPolicy(SecurityAdminService smService, Application app) {
        print("\n*******************************");
        print("       SM-ONOS APP REVIEW      ");
        print("*******************************");

        print("Application name: %s ", app.id().name());
        print("Application role: " + app.role());
        print("\nDeveloper specified permissions: ");
        printMap(smService.getPrintableSpecifiedPermissions(app.id()));
        print("\nPermissions granted: ");
        printMap(smService.getPrintableGrantedPermissions(app.id()));
        print("\nAdditional permissions requested on runtime (POLICY VIOLATIONS): ");
        printMap(smService.getPrintableRequestedPermissions(app.id()));
        print("");

    }

    /**
     * TYPES.
     * 0 - APP_PERM
     * 1 - ADMIN SERVICE
     * 2 - NB_SERVICE
     * 3 - SB_SERVICE
     * 4 - CLI_SERVICE
     * 5 - ETC_SERVICE
     * 6 - CRITICAL PERMISSIONS
     * 7 - ETC
     **/
    private void printMap(Map<Integer, List<Permission>> assortedMap) {

        for (Permission perm: assortedMap.get(0)) { // APP PERM
            if (perm.getName().contains("WRITE")) {
                printYellow("\t[APP PERMISSION] " + perm.getName());
            } else {
                printGreen("\t[APP PERMISSION] " + perm.getName());
            }
        }

        for (Permission perm: assortedMap.get(4)) {
            printGreen("\t[CLI SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
        }

        for (Permission perm: assortedMap.get(5)) {
            printYellow("\t[Other SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
        }

        for (Permission perm: assortedMap.get(7)) {
            printYellow("\t[Other] " + perm.getClass().getSimpleName() +
                                " " + perm.getName() + " (" + perm.getActions() + ")");
        }

        for (Permission perm: assortedMap.get(1)) { // ADMIN SERVICES
            printRed("\t[NB-ADMIN SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
        }

        for (Permission perm: assortedMap.get(3)) { // ADMIN SERVICES
            printRed("\t[SB SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
        }

        for (Permission perm: assortedMap.get(6)) { // CRITICAL SERVICES
            printRed("\t[CRITICAL PERMISSION] " + perm.getClass().getSimpleName() +
                             " " + perm.getName() + " (" + perm.getActions() + ")");
        }
    }

    private void printRed(String format, Object... args) {
        print(ANSI_RED + String.format(format, args) + ANSI_RESET);
    }

    private void printYellow(String format, Object... args) {
        print(ANSI_YELLOW + String.format(format, args) + ANSI_RESET);
    }

    private void printGreen(String format, Object... args) {
        print(ANSI_GREEN + String.format(format, args) + ANSI_RESET);
    }
}
