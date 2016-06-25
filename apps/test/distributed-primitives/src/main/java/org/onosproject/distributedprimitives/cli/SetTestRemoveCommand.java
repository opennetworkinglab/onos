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
package org.onosproject.distributedprimitives.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.util.KryoNamespace;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

import java.util.Arrays;
import java.util.Set;

/**
 * CLI command to remove elements from a distributed set.
 */
@Command(scope = "onos", name = "set-test-remove",
        description = "Remove from a distributed set")
public class SetTestRemoveCommand extends AbstractShellCommand {

    @Option(name = "-r", aliases = "--retain",
            description = "Only keep the given values in the set (if they already exist in the set)",
            required = false, multiValued = false)
    private boolean retain = false;

    @Option(name = "-c", aliases = "--clear", description = "Clear the set of all values",
            required = false, multiValued = false)
    private boolean clear = false;

    @Argument(index = 0, name = "setName",
            description = "set name",
            required = true, multiValued = false)
    String setName = null;

    @Argument(index = 1, name = "values",
            description = "Value(s) to remove from the set",
            required = false, multiValued = true)
    String[] values = null;

    Set<String> set;
    Serializer serializer = Serializer.using(
            new KryoNamespace.Builder().register(KryoNamespaces.BASIC).build());


    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        set = storageService.<String>setBuilder()
                .withName(setName)
                .withSerializer(serializer)
                .build()
                .asDistributedSet();

        if (clear) {
            set.clear();
            print("Set %s cleared", setName);
            return;
        }

        if (values == null) {
            print("Error executing command: No value given");
            return;
        }

        if (retain) { // Keep only the given values
            if (set.retainAll(Arrays.asList(values))) {
                print("%s was pruned to contain only elements of set %s", setName, Arrays.asList(values));
            } else {
                print("%s was not changed by retaining only elements of the set %s", setName, Arrays.asList(values));
            }
        } else if (values.length == 1) {
            // Remove a single element from the set
            if (set.remove(values[0])) {
                print("[%s] was removed from the set %s", values[0], setName);
            } else {
                print("[%s] was not in set %s", values[0], setName);
            }
        } else if (values.length > 1) {
            // Remove multiple elements from a set
            if (set.removeAll(Arrays.asList(values))) {
                print("%s was removed from the set %s", Arrays.asList(values), setName);
            } else {
                print("No element of %s was in set %s", Arrays.asList(values), setName);
            }
        }
    }
}
