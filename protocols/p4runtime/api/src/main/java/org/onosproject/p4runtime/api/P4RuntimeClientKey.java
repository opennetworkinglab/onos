/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import static java.lang.String.format;

/**
 * Key that uniquely identifies a P4Runtime client.
 */
@Beta
public final class P4RuntimeClientKey extends GrpcClientKey {

    private static final String DEVICE_ID_PARAM = "device_id=";

    private static final String P4RUNTIME = "P4Runtime";
    private final long p4DeviceId;

    /**
     * Creates a new client key. The server URI is expected to carry the
     * P4runtime server-internal 'device_id' as a param in the query string. For
     * example, grpc://10.0.0.1:5001?device_id=1
     *
     * @param deviceId  ONOS device ID
     * @param serverUri P4Runtime server URI
     */
    public P4RuntimeClientKey(DeviceId deviceId, URI serverUri) {
        super(P4RUNTIME, deviceId, serverUri);
        this.p4DeviceId = extractP4DeviceId(serverUri);
    }

    private static Long extractP4DeviceId(URI uri) {
        String[] segments = uri.getRawQuery().split("&");
        try {
            for (String s : segments) {
                if (s.startsWith(DEVICE_ID_PARAM)) {
                    return Long.parseUnsignedLong(
                            URLDecoder.decode(
                                    s.substring(DEVICE_ID_PARAM.length()), "utf-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(format(
                    "Unable to decode P4Runtime-interval device_id from URI %s: %s",
                    uri, e.toString()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(format(
                    "Invalid P4Runtime-interval device_id in URI %s: %s",
                    uri, e.toString()));
        }
        throw new IllegalArgumentException(format(
                "Missing P4Runtime-interval device_id in URI %s",
                uri));
    }

    /**
     * Returns the P4Runtime server-internal device ID.
     *
     * @return P4Runtime server-internal device ID
     */
    public long p4DeviceId() {
        return p4DeviceId;
    }
}
