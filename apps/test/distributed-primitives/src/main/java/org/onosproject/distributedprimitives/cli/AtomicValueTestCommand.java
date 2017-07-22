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
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

/**
 * Atomic value test command.
 */
@Command(scope = "onos", name = "value-test",
        description = "Manipulate an atomic value")
public class AtomicValueTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "value name",
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

    AtomicValue<String> value;

    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        value = storageService.<String>atomicValueBuilder()
                .withName(name)
                .withSerializer(Serializer.using(KryoNamespaces.BASIC))
                .build()
                .asAtomicValue();

        if ("get".equals(operation)) {
            print(value.get());
        } else if ("set".equals(operation)) {
            value.set("null".equals(arg1) ? null : arg1);
        } else if ("getAndSet".equals(operation)) {
            print(value.getAndSet(arg1));
        } else if ("compareAndSet".equals(operation)) {
            print(value.compareAndSet("null".equals(arg1) ? null : arg1, "null".equals(arg2) ? null : arg2));
        }
    }

    void print(Object value) {
        if (value == null) {
            print("null");
        } else {
            print("%s", value);
        }
    }
}
