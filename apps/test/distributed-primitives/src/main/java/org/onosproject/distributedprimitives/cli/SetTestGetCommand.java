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
 * CLI command to get the elements in a distributed set.
 */
@Command(scope = "onos", name = "set-test-get",
        description = "Get the elements in a distributed set")
public class SetTestGetCommand extends AbstractShellCommand {

    @Option(name = "-s", aliases = "--size", description = "Also show the size of the set?",
            required = false, multiValued = false)
    private boolean size = false;

    @Argument(index = 0, name = "setName",
            description = "set name",
            required = true, multiValued = false)
    String setName = null;

    @Argument(index = 1, name = "values",
            description = "Check if the set contains these value(s)",
            required = false, multiValued = true)
    String[] values = null;

    Set<String> set;
    String output = "";

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

        // Print the set size
        if (size) {
            print("There are %d items in set %s:", set.size(), setName);
        } else {
            print("Items in set %s:", setName);
        }
        // Print the set
        if (set.isEmpty()) {
            print("[]");
        } else {
            for (String e : set.toArray(new String[set.size()])) {
                if (output.isEmpty()) {
                    output += e;
                } else {
                    output += ", " + e;
                }
            }
            print("[%s]", output);
        }
        // Check if given values are in the set
        if (values == null) {
            return;
        } else if (values.length == 1) {
            // contains
            if (set.contains(values[0])) {
                print("Set %s contains the value %s", setName, values[0]);
            } else {
                print("Set %s did not contain the value %s", setName, values[0]);
            }
        } else if (values.length > 1) {
            //containsAll
            if (set.containsAll(Arrays.asList(values))) {
                print("Set %s contains the the subset %s", setName, Arrays.asList(values));
            } else {
                print("Set %s did not contain the the subset %s", setName, Arrays.asList(values));
            }
        }
    }
}
