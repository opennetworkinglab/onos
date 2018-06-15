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

package org.onosproject.p4runtime.ctl;

import com.google.protobuf.ByteString;
import p4.v1.P4RuntimeOuterClass;

import static java.lang.String.format;

/**
 * Utilities for P4 runtime control.
 */
final class P4RuntimeUtils {

    private P4RuntimeUtils() {
        // Hide default construction
    }

    static void assertSize(String entityDescr, ByteString value, int bitWidth)
            throws EncodeException {

        int byteWidth = (int) Math.ceil((float) bitWidth / 8);
        if (value.size() != byteWidth) {
            throw new EncodeException(format("Wrong size for %s, expected %d bytes, but found %d",
                                             entityDescr, byteWidth, value.size()));
        }
    }

    static void assertPrefixLen(String entityDescr, int prefixLength, int bitWidth)
            throws EncodeException {

        if (prefixLength > bitWidth) {
            throw new EncodeException(format(
                    "wrong prefix length for %s, field size is %d bits, but found one is %d",
                    entityDescr, bitWidth, prefixLength));
        }
    }

    static P4RuntimeOuterClass.Index indexMsg(long index) {
        return P4RuntimeOuterClass.Index.newBuilder().setIndex(index).build();
    }
}
