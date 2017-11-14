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

package org.onosproject.t3.api;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;

/**
 * API for troubleshooting services, providing static analysis of installed
 * flows and groups.
 */
public interface TroubleshootService {


    /**
     * Requests a static trace be performed for the given traffic selector
     * starting at the given connect point.
     *
     * @param packet description of packet
     * @param in point at which packet starts
     * @return a trace result
     */
    StaticPacketTrace trace(TrafficSelector packet, ConnectPoint in);
}
