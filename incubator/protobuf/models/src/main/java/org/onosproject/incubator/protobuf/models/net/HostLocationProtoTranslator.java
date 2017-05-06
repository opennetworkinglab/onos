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

package org.onosproject.incubator.protobuf.models.net;

import org.onosproject.grpc.net.models.ConnectPointProtoOuterClass.ConnectPointProto;
import org.onosproject.grpc.net.models.HostLocationProtoOuterClass.HostLocationProto;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;

/**
 * gRPC HostLocationProto message to equivalent ONOS Host location conversion related utilities.
 */
public final class HostLocationProtoTranslator {

    /**
     * Translates gRPC HostLocation to {@link HostLocation}.
     *
     * @param hostLocation gRPC message
     * @return {@link HostLocation}
     */
    public static HostLocation translate(HostLocationProto hostLocation) {

        if (hostLocation.equals(HostLocationProto.getDefaultInstance())) {
            return null;
        }

        return new HostLocation(DeviceId.deviceId(hostLocation.getConnectPoint().getDeviceId()),
                                PortNumber.portNumber(hostLocation.getConnectPoint().getPortNumber()), 0L);
    }

    /**
     * Translates {@link HostLocation} to gRPC HostLocation message.
     *
     * @param hostLocation {@link HostLocation}
     * @return gRPC HostLocation message
     */
    public static HostLocationProto translate(HostLocation hostLocation) {

        if (hostLocation != null) {
            return HostLocationProto.newBuilder()
                    .setConnectPoint(ConnectPointProto.newBuilder()
                                             .setDeviceId(hostLocation.deviceId().toString())
                                             .setPortNumber(hostLocation.port().toString()))
                    .setTime(hostLocation.time())
                    .build();
        }

        return HostLocationProto.getDefaultInstance();
    }

    // Utility class not intended for instantiation.
    private HostLocationProtoTranslator() {}
}
