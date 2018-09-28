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
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;

/**
 * CLI command to increment a distributed counter.
 */
@Service
@Command(scope = "onos", name = "counter-test",
        description = "Manipulate a distributed counter")
public class CounterTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "counter",
            description = "Counter name",
            required = true, multiValued = false)
    String counter = null;

    @Argument(index = 1, name = "operation",
            description = "operation name",
            required = true, multiValued = false)
    String operation = null;

    @Argument(index = 2, name = "value1",
            description = "first arg",
            required = false, multiValued = false)
    Long value1 = null;

    @Argument(index = 3, name = "value2",
            description = "second arg",
            required = false, multiValued = false)
    Long value2 = null;

    AtomicCounter atomicCounter;

    @Override
    protected void doExecute() {
        StorageService storageService = get(StorageService.class);
        atomicCounter = storageService.getAsyncAtomicCounter(counter).asAtomicCounter();
        if ("get".equals(operation)) {
            print("%d", atomicCounter.get());
        } else if ("set".equals(operation)) {
            atomicCounter.set(value1);
        } else if ("incrementAndGet".equals(operation)) {
            print("%d", atomicCounter.incrementAndGet());
        } else if ("getAndIncrement".equals(operation)) {
            print("%d", atomicCounter.getAndIncrement());
        } else if ("getAndAdd".equals(operation)) {
            print("%d", atomicCounter.getAndAdd(value1));
        } else if ("addAndGet".equals(operation)) {
            print("%d", atomicCounter.addAndGet(value1));
        } else if ("compareAndSet".equals(operation)) {
            print("%b", atomicCounter.compareAndSet(value1, value2));
        } else if ("destroy".equals(operation)) {
            atomicCounter.destroy();
        }
    }
}
