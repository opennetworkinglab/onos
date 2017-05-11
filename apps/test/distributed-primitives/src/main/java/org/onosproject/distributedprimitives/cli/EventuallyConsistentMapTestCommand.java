/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.distributedprimitives.DistributedPrimitivesTest;
import org.onosproject.store.service.EventuallyConsistentMap;

/**
 * CLI command to manipulate a distributed map.
 */
@Command(scope = "onos", name = "ec-map-test",
        description = "Manipulate an eventually consistent map")
public class EventuallyConsistentMapTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "map name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "operation",
            description = "operation name",
            required = true, multiValued = false)
    String operation = null;

    @Argument(index = 2, name = "key",
            description = "first arg",
            required = false, multiValued = false)
    String arg1 = null;

    @Argument(index = 3, name = "value1",
            description = "second arg",
            required = false, multiValued = false)
    String arg2 = null;

    @Argument(index = 4, name = "value2",
            description = "third arg",
            required = false, multiValued = false)
    String arg3 = null;

    EventuallyConsistentMap<String, String> map;

    @Override
    protected void execute() {
        DistributedPrimitivesTest test = get(DistributedPrimitivesTest.class);
        map = test.getEcMap(name);

        if ("get".equals(operation)) {
            print(map.get(arg1));
        } else if ("put".equals(operation)) {
            map.put(arg1, arg2);
        } else if ("size".equals(operation)) {
            print("%d", map.size());
        } else if ("isEmpty".equals(operation)) {
            print("%b", map.isEmpty());
        } else if ("clear".equals(operation)) {
            map.clear();
        } else if ("remove".equals(operation)) {
            if (arg2 == null) {
                print(map.remove(arg1));
            } else {
                map.remove(arg1, arg2);
            }
        } else if ("containsKey".equals(operation)) {
            print("%b", map.containsKey(arg1));
        } else if ("containsValue".equals(operation)) {
            print("%b", map.containsValue(arg1));
        }
    }

    void print(String value) {
        if (value == null) {
            print("null");
        } else {
            print("%s", value);
        }
    }
}
