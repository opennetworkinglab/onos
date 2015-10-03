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
package org.onosproject.distributedprimitives.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.util.KryoNamespace;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

import java.util.HashSet;
import java.util.Set;

/**
 * CLI command to add elements to a distributed set.
 */
@Command(scope = "onos", name = "set-test-add",
        description = "Add to a distributed set")
public class SetTestAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "setName",
            description = "set name",
            required = true, multiValued = false)
    String setName = null;

    @Argument(index = 1, name = "values",
            description = "Value(s) to add to the set",
            required = true, multiValued = true)
    String[] values = null;

    Set<String> set;
    Set<String> toAdd = new HashSet<>();


    Serializer serializer = Serializer.using(
            new KryoNamespace.Builder().register(KryoNamespaces.BASIC).build());


    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        set = storageService.<String>setBuilder()
                .withName(setName)
                .withSerializer(serializer)
                .build();

        // Add a single element to the set
        if (values.length == 1) {
            if (set.add(values[0])) {
                print("[%s] was added to the set %s", values[0], setName);
            } else {
                print("[%s] was already in set %s", values[0], setName);
            }
        } else if (values.length >= 1) {
            // Add multiple elements to a set
            for (String value : values) {
                toAdd.add(value);
            }
            if (set.addAll(toAdd)) {
                print("%s was added to the set %s", toAdd, setName);
            } else {
                print("%s was already in set %s", toAdd, setName);
            }
        }
    }
}
