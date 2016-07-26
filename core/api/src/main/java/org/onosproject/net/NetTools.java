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
package org.onosproject.net;

/**
 * Networking domain tools.
 *
 * @deprecated in Hummingbird release
 */
@Deprecated
public final class NetTools {

    private NetTools() {
    }

    /**
     * Converts DPIDs of the form xx:xx:xx:xx:xx:xx:xx to OpenFlow provider
     * device URIs. The is helpful for converting DPIDs coming from configuration
     * or REST to URIs that the core understands.
     *
     * @param dpid the DPID string to convert
     * @return the URI string for this device
     * @deprecated in Hummingbird release
     */
    @Deprecated
    public static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
