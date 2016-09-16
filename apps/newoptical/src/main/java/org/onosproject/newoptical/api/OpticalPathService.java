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
package org.onosproject.newoptical.api;

import com.google.common.annotations.Beta;
import org.onlab.util.Bandwidth;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service to setup optical domain connectivity.
 */
@Beta
public interface OpticalPathService extends ListenerService<OpticalPathEvent, OpticalPathListener> {

    /**
     * Calculates multi-layer path between connect points and sets up connectivity.
     *
     * @param ingress   ingress port
     * @param egress    egress port
     * @param bandwidth required bandwidth. No bandwidth is assured if null.
     * @param latency   required latency. No latency is assured if null.
     * @return ID of created connectivity if successful. null otherwise.
     */
    OpticalConnectivityId setupConnectivity(ConnectPoint ingress, ConnectPoint egress,
                                            Bandwidth bandwidth, Duration latency);

    /**
     * Sets up connectivity along given multi-layer path including cross-connect links.
     *
     * @param path      multi-layer path along which connectivity will be set up
     * @param bandwidth required bandwidth. No bandwidth is assured if null.
     * @param latency   required latency. No latency is assured if null.
     * @return true if successful. false otherwise.
     */
    OpticalConnectivityId setupPath(Path path, Bandwidth bandwidth, Duration latency);

    /**
     * Removes connectivity with given ID.
     *
     * @param id ID of connectivity
     * @return true if succeed. false if failed.
     */
    boolean removeConnectivity(OpticalConnectivityId id);

    /**
     * Returns path assigned to given ID.
     * @param id ID of connectivity
     * @return list of link that compose a path. empty if ID is invalid.
     */
    Optional<List<Link>> getPath(OpticalConnectivityId id);
}
