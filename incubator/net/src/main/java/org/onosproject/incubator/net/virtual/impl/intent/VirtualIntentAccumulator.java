/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl.intent;

import com.google.common.collect.Maps;
import org.onlab.util.AbstractAccumulator;
import org.onosproject.net.intent.IntentBatchDelegate;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.Key;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * An accumulator for building batches of intent operations for virtual network.
 * Only one batch should be in process per instance at a time.
 */
public class VirtualIntentAccumulator extends AbstractAccumulator<IntentData> {
    private static final int DEFAULT_MAX_EVENTS = 1000;
    private static final int DEFAULT_MAX_IDLE_MS = 10;
    private static final int DEFAULT_MAX_BATCH_MS = 50;

    // FIXME: Replace with a system-wide timer instance;
    // TODO: Convert to use HashedWheelTimer or produce a variant of that; then decide which we want to adopt
    private static final Timer TIMER = new Timer("virtual-intent-op-batching");

    private final IntentBatchDelegate delegate;

    private volatile boolean ready;

    /**
     * Creates an intent operation accumulator.
     *
     * @param delegate the intent batch delegate
     */
    public VirtualIntentAccumulator(IntentBatchDelegate delegate) {
        super(TIMER, DEFAULT_MAX_EVENTS, DEFAULT_MAX_BATCH_MS, DEFAULT_MAX_IDLE_MS);
        this.delegate = delegate;
        // Assume that the delegate is ready for work at the start
        ready = true; //TODO validate the assumption that delegate is ready
    }

    @Override
    public void processItems(List<IntentData> items) {
        ready = false;
        delegate.execute(reduce(items));
    }

    private Collection<IntentData> reduce(List<IntentData> ops) {
        Map<Key, IntentData> map = Maps.newHashMap();
        for (IntentData op : ops) {
            map.put(op.key(), op);
        }
        //TODO check the version... or maybe store will handle this.
        return map.values();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    public void ready() {
        ready = true;
    }
}
