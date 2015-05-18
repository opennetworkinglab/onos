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

package org.onosproject.cli.security;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.Permission;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages application permissions.
 */
@Command(scope = "onos", name = "perm",
        description = "Manages application permissions")
public class PermissionCommand extends AbstractShellCommand {

    static final String ADD = "add";
    static final String REMOVE = "remove";
    static final String LIST = "list";
    static final String CLEAR = "clear";


    @Argument(index = 0, name = "command",
            description = "Command name (add|remove)",
            required = true, multiValued = false)
    String command = null;

    @Argument(index = 1, name = "name", description = "Application name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 2, name = "permissions", description = "List of permissions",
            required = false, multiValued = true)
    String[] permissions = null;

    @Override
    protected void execute() {
        ApplicationAdminService applicationAdminService = get(ApplicationAdminService.class);
        Set<Permission> newPermSet = Sets.newHashSet();
        if (command.equals(ADD)) {
            ApplicationId appId = applicationAdminService.getId(name);
            if (appId == null) {
                print("No such application: %s", name);
                return;
            }
            Application app = applicationAdminService.getApplication(appId);

            for (String perm : permissions) {
                try {
                    Permission permission = Permission.valueOf(perm);
                    newPermSet.add(permission);
                } catch (IllegalArgumentException e) {
                    print("%s is not a valid permission.", perm);
                    return;
                }

            }
            Set<Permission> oldPermSet = applicationAdminService.getPermissions(appId);
            if (oldPermSet != null) {
                newPermSet.addAll(oldPermSet);
            } else {
                newPermSet.addAll(app.permissions());
            }
            applicationAdminService.setPermissions(appId, ImmutableSet.copyOf(newPermSet));

        } else if (command.equals(REMOVE)) {
            ApplicationId appId = applicationAdminService.getId(name);
            Application app = applicationAdminService.getApplication(appId);
            if (appId == null) {
                print("No such application: %s", name);
                return;
            }
            Set<Permission> oldPermSet = applicationAdminService.getPermissions(appId);
            if (oldPermSet == null) {
                oldPermSet = app.permissions();
            }
            Set<String> clearPermSet = Sets.newHashSet(permissions);
            newPermSet.addAll(oldPermSet.stream().filter(
                    perm -> !clearPermSet.contains(perm.name().toUpperCase())).collect(Collectors.toList()));
            applicationAdminService.setPermissions(appId, ImmutableSet.copyOf(newPermSet));
        } else if (command.equals(CLEAR)) {
            ApplicationId appId = applicationAdminService.getId(name);
            if (appId == null) {
                print("No such application: %s", name);
                return;
            }
            applicationAdminService.setPermissions(appId, ImmutableSet.of());
            print("Cleared the permission list of %s.", appId.name());
        } else if (command.equals(LIST)) {
            ApplicationId appId = applicationAdminService.getId(name);
            if (appId == null) {
                print("No such application: %s", name);
                return;
            }
            Application app = applicationAdminService.getApplication(appId);
            Set<Permission> userPermissions = applicationAdminService.getPermissions(appId);
            Set<Permission> defaultPermissions = app.permissions();
            print("Application Role");
            print("\trole=%s", app.role().name());

            if (defaultPermissions != null) {
                if (!defaultPermissions.isEmpty()) {
                    print("Default permissions (specified in app.xml)");
                    for (Permission perm : defaultPermissions) {
                        print("\tpermission=%s", perm.name());
                    }
                } else {
                    print("(No default permissions specified in app.xml)");
                }
            }
            if (userPermissions != null) {
                if (!userPermissions.isEmpty()) {
                    print("User permissions");
                    for (Permission perm : userPermissions) {
                        print("\tpermission=%s", perm.name());
                    }
                } else {
                    print("(User has removed all the permissions");
                }
            }

        }
    }
}
