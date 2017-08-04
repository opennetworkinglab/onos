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

package org.onosproject.net;

import com.google.common.base.MoreObjects;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import java.util.Objects;

/**
 * Connection point with TrafficSelector field.
 */
public class FilteredConnectPoint {
    private final ConnectPoint connectPoint;
    private final TrafficSelector trafficSelector;

    /**
     * Creates filtered connect point with default traffic selector.
     *
     * @param connectPoint connect point
     */
    public FilteredConnectPoint(ConnectPoint connectPoint) {
        this.connectPoint = connectPoint;
        this.trafficSelector = DefaultTrafficSelector.emptySelector();
    }

    /**
     * Creates new filtered connection point.
     *
     * @param connectPoint connect point
     * @param trafficSelector traffic selector for this connect point
     */
    public FilteredConnectPoint(ConnectPoint connectPoint, TrafficSelector trafficSelector) {
        this.connectPoint = connectPoint;
        this.trafficSelector = trafficSelector;
    }

    /**
     * Returns the traffic selector for this connect point.
     *
     * @return Traffic selector for this connect point
     */
    public TrafficSelector trafficSelector() {
        return trafficSelector;
    }

    /**
     * Returns the connection point.
     *
     * @return connect point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, trafficSelector);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("connectPoint", connectPoint)
                .add("trafficSelector", trafficSelector)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof FilteredConnectPoint) {
            FilteredConnectPoint other = (FilteredConnectPoint) obj;
            return other.connectPoint().equals(connectPoint) &&
                    other.trafficSelector().equals(trafficSelector());
        } else {
            return false;
        }
    }
}
