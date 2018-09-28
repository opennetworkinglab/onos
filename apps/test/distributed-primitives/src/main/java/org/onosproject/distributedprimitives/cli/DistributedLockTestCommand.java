/*
 * Copyright 2018-present Open Networking Foundation
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

import java.time.Duration;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.distributedprimitives.DistributedPrimitivesTest;
import org.onosproject.store.service.DistributedLock;

@Service
@Command(scope = "onos", name = "lock-test",
    description = "DistributedLock test cli fixture")
public class DistributedLockTestCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "name",
        description = "lock name",
        required = true,
        multiValued = false)
    String name = null;

    @Argument(index = 1, name = "operation",
        description = "operation",
        required = true,
        multiValued = false)
    String operation = null;

    @Argument(index = 2, name = "durationMillis",
        description = "lock attempt duration in milliseconds",
        required = false,
        multiValued = false)
    Long durationMillis = null;

    DistributedLock lock;

    @Override
    protected void doExecute() {
        DistributedPrimitivesTest test = get(DistributedPrimitivesTest.class);
        lock = test.getLock(name);
        if ("lock".equals(operation)) {
            lock.lock();
        } else if ("tryLock".equals(operation)) {
            if (durationMillis == null) {
                print("%b", lock.tryLock().isPresent());
            } else {
                print("%b", lock.tryLock(Duration.ofMillis(durationMillis)).isPresent());
            }
        } else if ("unlock".equals(operation)) {
            lock.unlock();
        }
    }
}
