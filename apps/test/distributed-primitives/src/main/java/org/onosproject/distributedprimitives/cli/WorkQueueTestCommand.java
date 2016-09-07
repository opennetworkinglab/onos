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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueStats;

import com.google.common.base.Throwables;

/**
 * CLI command to test a distributed work queue.
 */
@Command(scope = "onos", name = "work-queue-test",
        description = "Test a distributed work queue")
public class WorkQueueTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Work Queue name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "operation",
            description = "operation name. One of {add, addMutiple, "
                    + "takeAndComplete, totalPending, totalInProgress, totalCompleted, destroy}",
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

    WorkQueue<String> queue;

    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);
        queue = storageService.getWorkQueue(name, serializer);
        if (operation.equals("add")) {
            if (value1 == null) {
                print("Usage: add <value1>");
            } else {
                get(queue.addOne(value1));
                print("Done");
            }
        } else if (operation.equals("addMultiple")) {
            if (value1 == null || value2 == null) {
                print("Usage: addMultiple <value1> <value2>");
            } else {
                get(queue.addMultiple(Arrays.asList(value1, value2)));
                print("Done");
            }
        } else if (operation.equals("takeAndComplete")) {
            int maxItems = value1 != null ? Integer.parseInt(value1) : 1;
            Collection<Task<String>> tasks = get(queue.take(maxItems));
            tasks.forEach(task -> get(queue.complete(task.taskId())));
            print("Done");
        } else if (operation.equals("totalPending")) {
            WorkQueueStats stats = get(queue.stats());
            print("%d", stats.totalPending());
        } else if (operation.equals("totalInProgress")) {
            WorkQueueStats stats = get(queue.stats());
            print("%d", stats.totalInProgress());
        } else if (operation.equals("totalCompleted")) {
            WorkQueueStats stats = get(queue.stats());
            print("%d", stats.totalCompleted());
        } else if (operation.equals("destroy")) {
            get(queue.destroy());
        } else {
            print("Invalid operation name. Valid operations names are:"
                    + " [add, addMultiple takeAndComplete, totalPending, totalInProgress, totalCompleted, destroy]");
        }
    }

    private <T> T get(CompletableFuture<T> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
