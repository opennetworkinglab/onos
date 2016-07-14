/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.store.mcast.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.net.ConnectPoint;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple entity maintaining a mapping between a source and a collection of sink
 * connect points.
 */
public final class MulticastData {

    private final AtomicReference<ConnectPoint> source =
            new AtomicReference<>();
    private final Map<ConnectPoint, Boolean> sinks;
    private final AtomicBoolean isEmpty = new AtomicBoolean();

    private MulticastData() {
        this.sinks = Maps.newConcurrentMap();
        isEmpty.set(true);
    }

    public MulticastData(ConnectPoint source) {
        this.source.set(checkNotNull(source, "Multicast source cannot be null."));
        this.sinks = Maps.newConcurrentMap();
        isEmpty.set(false);
    }

    public ConnectPoint source() {
        return source.get();
    }

    public Set<ConnectPoint> sinks() {
        return ImmutableSet.copyOf(sinks.keySet());
    }

    public void setSource(ConnectPoint source) {
        // FIXME: violates immutability
        isEmpty.set(false);
        this.source.set(source);
    }

    public void appendSink(ConnectPoint sink) {
        checkNotNull(sink);
        sinks.put(sink, true);
    }

    public void removeSink(ConnectPoint sink) {
        checkNotNull(sink);
        sinks.remove(sink);
    }

    public boolean isEmpty() {
        return isEmpty.get();
    }

    public static MulticastData empty() {
        return new MulticastData();
    }

}
