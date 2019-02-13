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
package org.onosproject.incubator.protobuf.models.net.device;

import org.onosproject.grpc.net.device.models.PortDescriptionProtoOuterClass.PortDescriptionProto;
import org.onosproject.grpc.net.device.models.PortEnumsProto;
import org.onosproject.grpc.net.device.models.PortStatisticsProtoOuterClass;
import org.onosproject.grpc.net.device.models.PortStatisticsProtoOuterClass.PortStatisticsProto;
import org.onosproject.incubator.protobuf.models.net.AnnotationsTranslator;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC message conversion related utilities for port service.
 */
public final class PortProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(PortProtoTranslator.class);

    /**
     * Translates gRPC PortDescription message to {@link PortDescription}.
     *
     * @param portDescription gRPC message
     * @return {@link PortDescription}
     */
    public static PortDescription translate(PortDescriptionProto portDescription) {
        PortNumber number = PortNumber.fromString(portDescription.getPortNumber());
        boolean isEnabled = portDescription.getIsEnabled();
        Port.Type type = translate(portDescription.getType()).get();
        long portSpeed = portDescription.getPortSpeed();
        SparseAnnotations annotations = AnnotationsTranslator.asAnnotations(portDescription.getAnnotationsMap());
        // TODO How to deal with more specific Port...
        return DefaultPortDescription.builder().withPortNumber(number).isEnabled(isEnabled)
                .type(type).portSpeed(portSpeed).annotations(annotations)
                .build();
    }

    /**
     * Translates {@link PortDescription} to gRPC PortDescription message.
     *
     * @param portDescription {@link PortDescription}
     * @return gRPC PortDescription message
     */
    public static PortDescriptionProto translate(PortDescription portDescription) {
        return PortDescriptionProto.newBuilder()
                .setPortNumber(portDescription.portNumber().toString())
                .setIsEnabled(portDescription.isEnabled())
                .setType(translate(portDescription.type()))
                .setPortSpeed(portDescription.portSpeed())
                .putAllAnnotations(AnnotationsTranslator.asMap(portDescription.annotations()))
                .build();
    }

    /**
     * Translates gRPC PortType to {@link Port.Type}.
     *
     * @param type      gRPC message
     * @return  {@link Port.Type}
     */
    public static Optional<Port.Type> translate(PortEnumsProto.PortTypeProto type) {
        switch (type) {
            case COPPER:
                return Optional.of(Port.Type.COPPER);
            case FIBER:
                return Optional.of(Port.Type.FIBER);
            case OCH:
                return Optional.of(Port.Type.OCH);
            case ODUCLT:
                return Optional.of(Port.Type.ODUCLT);
            case OMS:
                return Optional.of(Port.Type.OMS);
            case PACKET:
                return Optional.of(Port.Type.PACKET);
            case VIRTUAL_PORT:
                return Optional.of(Port.Type.VIRTUAL);

            default:
                log.warn("Unexpected PortType: {}", type);
                return Optional.empty();
        }
    }

    /**
     * Translates {@link Port.Type} to gRPC PortType.
     *
     * @param type      {@link Port.Type}
     * @return  gRPC message
     */
    public static PortEnumsProto.PortTypeProto translate(Port.Type type) {
        switch (type) {
            case COPPER:
                return PortEnumsProto.PortTypeProto.COPPER;
            case FIBER:
                return PortEnumsProto.PortTypeProto.FIBER;
            case OCH:
                return PortEnumsProto.PortTypeProto.OCH;
            case ODUCLT:
                return PortEnumsProto.PortTypeProto.ODUCLT;
            case OMS:
                return PortEnumsProto.PortTypeProto.OMS;
            case PACKET:
                return PortEnumsProto.PortTypeProto.PACKET;
            case VIRTUAL:
                return PortEnumsProto.PortTypeProto.VIRTUAL_PORT;

            default:
                log.warn("Unexpected Port.Type: {}", type);
                return PortEnumsProto.PortTypeProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates gRPC PortStatistics message to {@link PortStatisticsProtoOuterClass}.
     *
     * @param portStatistics gRPC PortStatistics message
     * @return {@link PortStatisticsProtoOuterClass}
     */
    public static PortStatistics translate(PortStatisticsProto portStatistics) {
        // TODO implement adding missing fields
        return DefaultPortStatistics.builder()
                .setPort(PortNumber.portNumber(portStatistics.getPort()))
                .setPacketsReceived(portStatistics.getPacketsReceived())
                .setPacketsSent(portStatistics.getPacketsSent())
                .build();
    }

    /**
     * Translates {@link PortStatistics} to gRPC PortStatistics message.
     *
     * @param portStatistics {@link PortStatistics}
     * @return gRPC PortStatistics message
     */
    public static PortStatisticsProto translate(PortStatistics portStatistics) {
        // TODO implement adding missing fields
        return PortStatisticsProto.newBuilder()
                .setPort((int) portStatistics.portNumber().toLong())
                .setPacketsReceived(portStatistics.packetsReceived())
                .setPacketsSent(portStatistics.packetsSent())
                .build();
    }

    // Utility class not intended for instantiation.
    private PortProtoTranslator() {}
}
