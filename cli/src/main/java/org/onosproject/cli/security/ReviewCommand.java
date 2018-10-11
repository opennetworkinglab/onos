/*
 * Copyright 2015-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
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
@Service
@Command(scope = "onos", name = "review",
        description = "Application security policy review interface")
public class ReviewCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name", description = "Application name",
            required = true, multiValued = false)
    @Completion(ReviewApplicationNameCompleter.class)
    String name = null;

    @Argument(index = 1, name = "accept", description = "Option to accept policy",
            required = false, multiValued = false)
    String accept = null;

    @Override
    protected void doExecute() {
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
        } else if ("accept".equals(accept.trim())) {
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
    private void printMap(Map<Integer, List<Permission>> assortedMap) {
        for (Integer type : assortedMap.keySet()) {
            switch (type) {
                case 0:
                    for (Permission perm: assortedMap.get(0)) {
                        print("\t[APP PERMISSION] " + perm.getName());
                    }
                    break;
                case 1:
                    for (Permission perm: assortedMap.get(1)) {
                        print("\t[NB-ADMIN SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
                    }
                    break;
                case 2:
                    for (Permission perm: assortedMap.get(2)) {
                        print("\t[NB SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
                    }
                    break;
                case 3:
                    for (Permission perm: assortedMap.get(3)) {
                        print("\t[Other SERVICE] " + perm.getName() + "(" + perm.getActions() + ")");
                    }
                    break;
                case 4:
                    for (Permission perm: assortedMap.get(4)) {
                        print("\t[Other] " + perm.getClass().getSimpleName() +
                                " " + perm.getName() + " (" + perm.getActions() + ")");
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
