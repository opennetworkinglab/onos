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
package org.onosproject.net.statistic;

import com.google.common.annotations.Beta;
import org.onosproject.net.ConnectPoint;

/**
 * Service for obtaining statistic information about device ports.
 */
@Beta
public interface PortStatisticsService {

    /** Specifies the type of metric. */
    enum MetricType {
        /** Load is to be given in bytes/second. */
        BYTES,

        /** Load is to be given in packets/second. */
        PACKETS
    }

    /**
     * Obtain the egress load for the given port in terms of bytes per second.
     *
     * @param connectPoint the port to query
     * @return egress traffic load
     */
    Load load(ConnectPoint connectPoint);

    /**
     * Obtain the egress load for the given port in terms of the specified metric.
     *
     * @param connectPoint the port to query
     * @param metricType   metric type
     * @return egress traffic load
     */
    default Load load(ConnectPoint connectPoint, MetricType metricType) {
        return load(connectPoint);
    }

}
