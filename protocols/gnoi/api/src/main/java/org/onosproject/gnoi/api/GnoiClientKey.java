/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.gnoi.api;

import com.google.common.annotations.Beta;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;

import java.net.URI;

/**
 * Key that uniquely identifies a gNOI client.
 */
@Beta
public class GnoiClientKey extends GrpcClientKey {

    private static final String GNOI = "gNOI";

    /**
     * Creates a new gNOI client key.
     *
     * @param deviceId  ONOS device ID
     * @param serverUri gNOI server URI
     */
    public GnoiClientKey(DeviceId deviceId, URI serverUri) {
        super(GNOI, deviceId, serverUri);
    }
}
