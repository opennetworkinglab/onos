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

import org.onlab.packet.IpAddress;
import org.onosproject.grpc.net.models.ConnectPointProtoOuterClass.ConnectPointProto;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.IpElementId;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC ConnectPoint message to org.onosproject.net.ConnectPoint conversion related utilities.
 */
public final class ConnectPointProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(ConnectPointProtoTranslator.class);

    /**
     * Translates gRPC ConnectPoint message to Optional of {@link org.onosproject.net.ConnectPoint}.
     *
     * @param connectPoint gRPC message
     * @return Optional of equivalent {@link org.onosproject.net.ConnectPoint} or empty if ElementId is not recognized
     */
    public static Optional<ConnectPoint> translate(ConnectPointProto connectPoint) {
        switch (connectPoint.getElementIdCase()) {
            case DEVICE_ID:
                return Optional.of(new ConnectPoint(DeviceId.deviceId(connectPoint.getDeviceId()),
                                                    PortNumber.portNumber(connectPoint.getPortNumber())));
            case HOST_ID:
                return Optional.of(new ConnectPoint(HostId.hostId(connectPoint.getHostId()),
                                                    PortNumber.portNumber(connectPoint.getPortNumber())));
            case IP_ELEMENT_ID:
                return Optional.of(new ConnectPoint(IpElementId.ipElement(IpAddress
                                                                                  .valueOf(connectPoint
                                                                                                   .getIpElementId()
                                                                                  )),
                                                    PortNumber.portNumber(connectPoint.getPortNumber())));
            default:
                return Optional.empty();
        }
    }

    /**
     * Translates {@link org.onosproject.net.ConnectPoint} to gRPC ConnectPoint message.
     *
     * @param connectPoint {@link org.onosproject.net.ConnectPoint}
     * @return gRPC ConnectPoint message
     */
    public static ConnectPointProto translate(ConnectPoint connectPoint) {

        if (connectPoint.elementId() instanceof DeviceId) {
            return ConnectPointProto.newBuilder().setDeviceId(connectPoint.deviceId().toString())
                    .setPortNumber(connectPoint.port().toString())
                    .build();
        } else if (connectPoint.elementId() instanceof HostId) {
            return ConnectPointProto.newBuilder().setHostId(connectPoint.hostId().toString())
                    .setPortNumber(connectPoint.port().toString())
                    .build();
        } else if (connectPoint.ipElementId() != null) {
            return ConnectPointProto.newBuilder().setIpElementId(connectPoint.ipElementId().toString())
                    .setPortNumber(connectPoint.port().toString())
                    .build();
        } else {
            log.warn("Unrecognized ElementId", connectPoint);
            throw new IllegalArgumentException("Unrecognized ElementId");
        }
    }

    // Utility class not intended for instantiation.
    private ConnectPointProtoTranslator() {}
}