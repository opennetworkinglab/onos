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

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.grpc.net.models.HostIdProtoOuterClass.HostIdProto;
import org.onosproject.net.HostId;

/**
 * gRPC HostIdProto message to equivalent ONOS HostId conversion related utilities.
 */
public final class HostIdProtoTranslator {

    /**
     * Translates gRPC HostId to {@link HostId}.
     *
     * @param hostId gRPC message
     * @return {@link HostId}
     */
    public static HostId translate(HostIdProto hostId) {

        if (hostId.equals(HostIdProto.getDefaultInstance())) {
            return null;
        }

        return HostId.hostId(MacAddress.valueOf(hostId.getMac()), VlanId.vlanId((short) hostId.getVlanId()));
    }

    /**
     * Translates {@link HostId} to gRPC HostId message.
     *
     * @param hostId {@link HostId}
     * @return gRPC HostId message
     */
    public static HostIdProto translate(HostId hostId) {

        if (hostId != null) {
            return HostIdProto.newBuilder()
                    .setMac(hostId.mac().toString())
                    .setVlanId(hostId.vlanId().toShort())
                    .build();
        }

        return HostIdProto.getDefaultInstance();
    }

    // Utility class not intended for instantiation.
    private HostIdProtoTranslator() {}
}
