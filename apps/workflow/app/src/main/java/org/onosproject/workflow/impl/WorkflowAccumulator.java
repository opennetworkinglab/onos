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

import com.google.common.collect.Maps;
import org.onlab.util.AbstractAccumulator;
import org.onosproject.workflow.api.WorkflowData;
import org.onosproject.workflow.api.WorkflowBatchDelegate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * An accumulator for building batches of workflow operations. Only one batch should
 * be in process per instance at a time.
 */
public class WorkflowAccumulator extends AbstractAccumulator<WorkflowData> {

    private static final int DEFAULT_MAX_EVENTS = 1000;
    private static final int DEFAULT_MAX_IDLE_MS = 10;
    private static final int DEFAULT_MAX_BATCH_MS = 50;

    private static final Timer TIMER = new Timer("onos-workflow-op-batching");

    private final WorkflowBatchDelegate delegate;

    private volatile boolean ready;

    /**
     * Creates an intent operation accumulator.
     *
     * @param delegate the intent batch delegate
     */
    protected WorkflowAccumulator(WorkflowBatchDelegate delegate) {
        super(TIMER, DEFAULT_MAX_EVENTS, DEFAULT_MAX_BATCH_MS, DEFAULT_MAX_IDLE_MS);
        this.delegate = delegate;
        // Assume that the delegate is ready for worklettype at the start
        ready = true; //TODO validate the assumption that delegate is ready
    }

    @Override
    public void processItems(List<WorkflowData> items) {
        ready = false;
        delegate.execute(reduce(items));
    }

    private Collection<WorkflowData> reduce(List<WorkflowData> ops) {
        Map<String, WorkflowData> map = Maps.newHashMap();
        for (WorkflowData op : ops) {
            map.put(op.name(), op);
        }
        //TODO check the version... or maybe workplaceStore will handle this.
        return map.values();
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
