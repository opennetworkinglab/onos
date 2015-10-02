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
package org.onosproject.net.mcast;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.ConnectPoint;

import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * An entity representing a multicast event. Event either add or remove
 * sinks or sources.
 */
@Beta
public class McastEvent extends AbstractEvent<McastEvent.Type, McastRoute> {

    private final Optional<ConnectPoint> sink;
    private final Optional<ConnectPoint> source;

    public enum Type {
        /**
         * A new mcast route has been added.
         */
        ROUTE_ADDED,

        /**
         * A mcast route has been removed.
         */
        ROUTE_REMOVED,

        /**
         * A source for a mcast route (ie. the subject) has been added.
         */
        SOURCE_ADDED,

        /**
         * A sink for a mcast route (ie. the subject) has been added.
         */
        SINK_ADDED,

        /**
         * A source for a mcast route (ie. the subject) has been removed.
         */
        SINK_REMOVED
    }

    private McastEvent(McastEvent.Type type, McastRoute subject) {
        super(type, subject);
        sink = Optional.empty();
        source = Optional.empty();
    }

    private McastEvent(McastEvent.Type type, McastRoute subject, long time) {
        super(type, subject, time);
        sink = Optional.empty();
        source = Optional.empty();
    }

    public McastEvent(McastEvent.Type type, McastRoute subject,
                      ConnectPoint sink,
                      ConnectPoint source) {
        super(type, subject);
        this.sink = Optional.ofNullable(sink);
        this.source = Optional.ofNullable(source);
    }

    public McastEvent(McastEvent.Type type, McastRoute subject, long time,
                       ConnectPoint sink,
                       ConnectPoint source) {
        super(type, subject, time);
        this.sink = Optional.ofNullable(sink);
        this.source = Optional.ofNullable(source);
    }

    /**
     * The sink which has been removed or added. The field may not be set
     * if the sink has not been detected yet or has been removed.
     *
     * @return an optional connect point
     */
    public Optional<ConnectPoint> sink() {
        return sink;
    }

    /**
     * The source which has been removed or added.

     * @return an optional connect point
     */
    public Optional<ConnectPoint> source() {
        return source;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type())
                .add("route", subject())
                .add("source", source)
                .add("sinks", sink).toString();
    }
}
