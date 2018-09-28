/*
 * Copyright 2016-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.Version;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;

/**
 * CLI command to manipulate a distributed value.
 */
@Service
@Command(scope = "onos", name = "map-test",
        description = "Manipulate a consistent map")
public class ConsistentMapTestCommand extends AbstractShellCommand {

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

    ConsistentMap<String, String> map;

    @Override
    protected void doExecute() {
        StorageService storageService = get(StorageService.class);
        map = storageService.<String, String>consistentMapBuilder()
            .withName(name)
            .withSerializer(Serializer.using(KryoNamespaces.BASIC))
            .withVersion(Version.version("1.0.0"))
            .withCompatibilityFunction((value, version) -> version + ":" + value)
            .build();
        if ("get".equals(operation)) {
            print(map.get(arg1));
        } else if ("put".equals(operation)) {
            print(map.put(arg1, arg2));
        } else if ("size".equals(operation)) {
            print("%d", map.size());
        } else if ("isEmpty".equals(operation)) {
            print("%b", map.isEmpty());
        } else if ("putIfAbsent".equals(operation)) {
            print(map.putIfAbsent(arg1, arg2));
        } else if ("putAndGet".equals(operation)) {
            print(map.putAndGet(arg1, arg2));
        } else if ("clear".equals(operation)) {
            map.clear();
        } else if ("remove".equals(operation)) {
            if (arg2 == null) {
                print(map.remove(arg1));
            } else {
                print("%b", map.remove(arg1, arg2));
            }
        } else if ("containsKey".equals(operation)) {
            print("%b", map.containsKey(arg1));
        } else if ("containsValue".equals(operation)) {
            print("%b", map.containsValue(arg1));
        } else if ("replace".equals(operation)) {
            if (arg3 == null) {
                print(map.replace(arg1, arg2));
            } else {
                print("%b", map.replace(arg1, arg2, arg3));
            }
        } else if ("compatiblePut".equals(operation)) {
            ConsistentMap<String, String> map = storageService.<String, String>consistentMapBuilder()
                .withName(name)
                .withSerializer(Serializer.using(KryoNamespaces.BASIC))
                .withCompatibilityFunction((value, version) -> version + ":" + value)
                .withVersion(Version.version("2.0.0"))
                .build();
            print(map.put(arg1, arg2));
        } else if ("compatibleGet".equals(operation)) {
            ConsistentMap<String, String> map = storageService.<String, String>consistentMapBuilder()
                .withName(name)
                .withSerializer(Serializer.using(KryoNamespaces.BASIC))
                .withCompatibilityFunction((value, version) -> version + ":" + value)
                .withVersion(Version.version("2.0.0"))
                .build();
            print(map.get(arg1));
        }
    }

    void print(Versioned<String> value) {
        if (value == null) {
            print("null");
        } else {
            print("%s", value.value());
        }
    }
}
