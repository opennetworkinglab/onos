/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.net.routing;

import org.onlab.packet.IpPrefix;

/**
 * Routing tools.
 */
public final class RouteTools {

    private RouteTools() {
    }

    /**
     * Creates a binary string representation of an IP prefix.
     *
     * For each string, we put a extra "0" in the front. The purpose of
     * doing this is to store the default route inside InvertedRadixTree.
     *
     * @param ipPrefix the IP prefix to use
     * @return the binary string representation
     */
    public static String createBinaryString(IpPrefix ipPrefix) {
        byte[] octets = ipPrefix.address().toOctets();
        StringBuilder result = new StringBuilder(ipPrefix.prefixLength());
        result.append("0");
        for (int i = 0; i < ipPrefix.prefixLength(); i++) {
            int byteOffset = i / Byte.SIZE;
            int bitOffset = i % Byte.SIZE;
            int mask = 1 << (Byte.SIZE - 1 - bitOffset);
            byte value = octets[byteOffset];
            boolean isSet = ((value & mask) != 0);
            result.append(isSet ? "1" : "0");
        }
        return result.toString();
    }
}
