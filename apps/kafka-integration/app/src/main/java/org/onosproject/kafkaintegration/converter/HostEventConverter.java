/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.kafkaintegration.converter;

import com.google.protobuf.GeneratedMessageV3;

import org.onlab.packet.IpAddress;
import org.onosproject.event.Event;
import org.onosproject.grpc.net.host.models.HostEnumsProto.HostEventTypeProto;
import org.onosproject.grpc.net.host.models.HostEventProto.HostNotificationProto;
import org.onosproject.grpc.net.models.HostProtoOuterClass.HostProto;
import org.onosproject.incubator.protobuf.models.net.AnnotationsTranslator;
import org.onosproject.incubator.protobuf.models.net.HostIdProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.HostLocationProtoTranslator;
import org.onosproject.net.host.HostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

/**
 * Converts ONOS Host event message to protobuf format.
 */
public class HostEventConverter implements EventConverter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public byte[] convertToProtoMessage(Event<?, ?> event) {

        HostEvent hostEvent = (HostEvent) event;

        if (!hostEventTypeSupported(hostEvent)) {
            log.error("Unsupported Onos Host Event {}. There is no matching"
                    + "proto Host Event type", hostEvent.type().toString());
            return null;
        }

        return ((GeneratedMessageV3) buildHostProtoMessage(hostEvent)).toByteArray();

    }

    /**
     * Checks if the ONOS Host Event type is supported.
     *
     * @param event ONOS Host event
     * @return true if there is a match and false otherwise
     */
    private boolean hostEventTypeSupported(HostEvent event) {
        HostEventTypeProto[] hostEvents = HostEventTypeProto.values();
        for (HostEventTypeProto hostEventType : hostEvents) {
            if (hostEventType.name().equals(event.type().name())) {
                return true;
            }
        }

        return false;
    }

    private HostNotificationProto buildHostProtoMessage(HostEvent hostEvent) {
        HostNotificationProto.Builder notificationBuilder =
                HostNotificationProto.newBuilder();

        HostProto hostCore =
                HostProto.newBuilder()
                        .setHostId(HostIdProtoTranslator.translate(hostEvent
                                .subject().id()))
                        .setConfigured(hostEvent.subject().configured())
                        .addAllIpAddresses(hostEvent.subject().ipAddresses()
                                .stream().map(IpAddress::toString)
                                .collect(Collectors.toList()))
                        .setLocation(HostLocationProtoTranslator.translate(
                                hostEvent.subject().location()))
                        .setVlan(hostEvent.subject().vlan().toShort())
                        .putAllAnnotations(AnnotationsTranslator.asMap(
                                hostEvent.subject().annotations()))
                        .build();

        notificationBuilder.setHostEventType(getProtoType(hostEvent))
                .setHost(hostCore);

        return notificationBuilder.build();
    }

    /**
     * Retrieves the protobuf generated host event type.
     *
     * @param event ONOS Host Event
     * @return generated Host Event Type
     */
    private HostEventTypeProto getProtoType(HostEvent event) {
        HostEventTypeProto protobufEventType = null;
        HostEventTypeProto[] hostEvents = HostEventTypeProto.values();
        for (HostEventTypeProto hostEventType : hostEvents) {
            if (hostEventType.name().equals(event.type().name())) {
                protobufEventType = hostEventType;
            }
        }

        return protobufEventType;
    }
}
