/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.virtualbng;

import org.onosproject.net.ConnectPoint;

/**
 * Configuration for a connect point.
 */
public class ConnectPointConfiguration {

    private ConnectPoint connectPoint;

    /**
     * Creats a new connect point from a string representation.
     *
     * @param string connect point string
     */
    public ConnectPointConfiguration(String string) {
        connectPoint = ConnectPoint.deviceConnectPoint(string);
    }

    /**
     * Creates a new connect point from a string representation.
     *
     * @param string connect point string
     * @return new connect point configuration
     */
    public static ConnectPointConfiguration of(String string) {
        return new ConnectPointConfiguration(string);
    }

    /**
     * Gets the connect point.
     *
     * @return connect point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }
}
