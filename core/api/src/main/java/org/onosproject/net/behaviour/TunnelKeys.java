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
package org.onosproject.net.behaviour;

/**
 * Static utility methods pertaining to {@link TunnelKey} instances.
 */
public final class TunnelKeys {

    private TunnelKeys() {
    }

    private static TunnelKey<String> flowKey = new TunnelKey<>("flow");

    /**
     * Returns a tunnel key with FLOW keyword.
     *
     * @return tunnel key instance
     */
    public static TunnelKey<String> flowTunnelKey() {
        return flowKey;
    }

    // TODO add more for various types of tunnel
}
