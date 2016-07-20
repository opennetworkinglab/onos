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
package org.onosproject.net.mcast;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * An entity representing a multicast event. Event either add or remove
 * sinks or sources.
 */
@Beta
public class McastEvent extends AbstractEvent<McastEvent.Type, McastRouteInfo> {


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

    public McastEvent(McastEvent.Type type, McastRouteInfo subject) {
        super(type, subject);
    }


    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type())
                .add("info", subject()).toString();
    }
}
