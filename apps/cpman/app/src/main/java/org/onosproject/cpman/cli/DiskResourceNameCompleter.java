/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cpman.cli;

import org.apache.karaf.shell.console.completer.ArgumentCompleter;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cpman.ControlPlaneMonitorService;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static org.onosproject.cpman.ControlResource.Type;
/**
 * Disk resource name completer.
 */
public class DiskResourceNameCompleter extends AbstractCompleter {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Resource type is the second argument.
        ArgumentCompleter.ArgumentList list = getArgumentList();
        String type = list.getArguments()[1];

        if (Type.DISK.toString().toLowerCase().equals(type)) {
            ControlPlaneMonitorService monitorService =
                    AbstractShellCommand.get(ControlPlaneMonitorService.class);

            Set<String> set = monitorService.availableResources(Type.DISK);
            SortedSet<String> strings = delegate.getStrings();

            if (set != null) {
                set.forEach(s -> strings.add(s));
            }
        }
        return delegate.complete(buffer, cursor, candidates);
    }
}
