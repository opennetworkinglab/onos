/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.mcast.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple entity maintaining a mapping between a source and a collection of sink
 * connect points.
 */
public final class MulticastData {

    private final ConnectPoint source;
    private final List<ConnectPoint> sinks;
    private final boolean isEmpty;

    private MulticastData() {
        this.source = null;
        this.sinks = Collections.EMPTY_LIST;
        isEmpty = true;
    }

    public MulticastData(ConnectPoint source, List<ConnectPoint> sinks) {
        this.source = checkNotNull(source, "Multicast source cannot be null.");
        this.sinks = checkNotNull(sinks, "List of sinks cannot be null.");
        isEmpty = false;
    }

    public MulticastData(ConnectPoint source, ConnectPoint sink) {
        this.source = checkNotNull(source, "Multicast source cannot be null.");
        this.sinks = Lists.newArrayList(checkNotNull(sink, "Sink cannot be null."));
        isEmpty = false;
    }

    public MulticastData(ConnectPoint source) {
        this.source = checkNotNull(source, "Multicast source cannot be null.");
        this.sinks = Lists.newArrayList();
        isEmpty = false;
    }

    public ConnectPoint source() {
        return source;
    }

    public List<ConnectPoint> sinks() {
        return ImmutableList.copyOf(sinks);
    }

    public void appendSink(ConnectPoint sink) {
        sinks.add(sink);
    }

    public boolean removeSink(ConnectPoint sink) {
        return sinks.remove(sink);
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public static MulticastData empty() {
        return new MulticastData();
    }

}
