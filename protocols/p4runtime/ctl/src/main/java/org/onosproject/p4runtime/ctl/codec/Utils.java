/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.codec;

import com.google.protobuf.ByteString;

import static java.lang.String.format;

/**
 * Codec utilities.
 */
final class Utils {

    private Utils() {
        // Hide default construction
    }

    static void assertSize(String entityDescr, ByteString value, int bitWidth)
            throws CodecException {

        int byteWidth = (int) Math.ceil((float) bitWidth / 8);
        if (value.size() != byteWidth) {
            throw new CodecException(format(
                    "Wrong size for %s, expected %d bytes, but found %d",
                    entityDescr, byteWidth, value.size()));
        }
    }

    static void assertPrefixLen(String entityDescr, int prefixLength, int bitWidth)
            throws CodecException {

        if (prefixLength > bitWidth) {
            throw new CodecException(format(
                    "wrong prefix length for %s, field size is %d bits, but found one is %d",
                    entityDescr, bitWidth, prefixLength));
        }
    }
}
