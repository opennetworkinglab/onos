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
package org.onosproject.workflow.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.onlab.util.AbstractAccumulator;
import org.onosproject.workflow.api.HandlerTask;
import org.onosproject.workflow.api.HandlerTaskBatchDelegate;

import java.util.Collection;
import java.util.List;
import java.util.Timer;

/**
 * An accumulator for building batches of event task operations. Only one batch should
 * be in process per instance at a time.
 */
public class HandlerTaskAccumulator extends AbstractAccumulator<HandlerTask> {

    private static final int DEFAULT_MAX_EVENTS = 5000;
    private static final int DEFAULT_MAX_IDLE_MS = 10;
    private static final int DEFAULT_MAX_BATCH_MS = 50;

    private static final Timer TIMER = new Timer("onos-workflow-handlertask-batching");

    private final HandlerTaskBatchDelegate delegate;

    private volatile boolean ready;

    /**
     * Creates an event-task operation accumulator.
     *
     * @param delegate the event-task batch delegate
     */
    protected HandlerTaskAccumulator(HandlerTaskBatchDelegate delegate) {
        super(TIMER, DEFAULT_MAX_EVENTS, DEFAULT_MAX_BATCH_MS, DEFAULT_MAX_IDLE_MS);
        this.delegate = delegate;
        // Assume that the delegate is ready for workletType at the start
        ready = true; //TODO validate the assumption that delegate is ready
    }

    @Override
    public void processItems(List<HandlerTask> items) {
        ready = false;
        delegate.execute(epoch(items));
    }

    /**
     * Gets epoch.
     * @param ops handler tasks
     * @return collection of collection of handler task.
     */
    private Collection<Collection<HandlerTask>> epoch(List<HandlerTask> ops) {

        ListMultimap<String, HandlerTask> tasks = ArrayListMultimap.create();

        // align event-tasks with context
        for (HandlerTask op : ops) {
            tasks.put(op.context().name(), op);
        }

        return tasks.asMap().values();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    /**
     * Making accumulator to be ready.
     */
    public void ready() {
        ready = true;
    }
}
