/*
 * Copyright 2016-present Open Networking Laboratory
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
 * CLI command to manipulate a distributed value.
 */
@Command(scope = "onos", name = "value-test",
        description = "Manipulate a distributed value")
public class ValueTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "value",
            description = "Value name",
            required = true, multiValued = false)
    String value = null;

    @Argument(index = 1, name = "operation",
            description = "operation name",
            required = true, multiValued = false)
    String operation = null;

    @Argument(index = 2, name = "value1",
            description = "first arg",
            required = false, multiValued = false)
    String value1 = null;

    @Argument(index = 3, name = "value2",
            description = "second arg",
            required = false, multiValued = false)
    String value2 = null;

    AtomicValue<String> atomicValue;

    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        atomicValue = storageService.<String>atomicValueBuilder()
                                    .withName(value)
                                    .withSerializer(Serializer.using(KryoNamespaces.BASIC))
                                    .build()
                                    .asAtomicValue();
        if ("get".equals(operation)) {
            print("%s", atomicValue.get());
        } else if ("set".equals(operation)) {
            atomicValue.set(value1);
        } else if ("compareAndSet".equals(operation)) {
            print("%b", atomicValue.compareAndSet(value1, value2));
        } else if ("getAndSet".equals(operation)) {
            print("%s", atomicValue.getAndSet(value1));
        } else if ("destroy".equals(operation)) {
            atomicValue.destroy();
        } else {
            print("Error, unknown operation %s", operation);
        }
    }
}
