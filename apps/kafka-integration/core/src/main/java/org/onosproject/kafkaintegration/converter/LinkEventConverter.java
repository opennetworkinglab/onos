/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.event.Event;
import org.onosproject.grpc.net.Link.ConnectPoint;
import org.onosproject.grpc.net.Link.LinkCore;
import org.onosproject.grpc.net.Link.LinkState;
import org.onosproject.grpc.net.Link.LinkType;
import org.onosproject.grpc.net.LinkEvent.LinkEventType;
import org.onosproject.grpc.net.LinkEvent.LinkNotification;
import org.onosproject.net.link.LinkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessageV3;

/**
 * Converts for ONOS Link event message to protobuf format.
 */
public class LinkEventConverter implements EventConverter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public byte[] convertToProtoMessage(Event<?, ?> event) {

        LinkEvent linkEvent = (LinkEvent) event;

        if (!linkEventTypeSupported(linkEvent)) {
            log.error("Unsupported Onos Event {}. There is no matching "
                    + "proto Event type", linkEvent.type().toString());
            return null;
        }

        return ((GeneratedMessageV3) buildDeviceProtoMessage(linkEvent)).toByteArray();
    }

    private boolean linkEventTypeSupported(LinkEvent event) {
        LinkEventType[] kafkaLinkEvents = LinkEventType.values();
        for (LinkEventType linkEventType : kafkaLinkEvents) {
            if (linkEventType.name().equals(event.type().name())) {
                return true;
            }
        }
        return false;
    }

    private LinkNotification buildDeviceProtoMessage(LinkEvent linkEvent) {
        LinkNotification notification = LinkNotification.newBuilder()
                .setLinkEventType(getProtoType(linkEvent))
                .setLink(LinkCore.newBuilder()
                        .setState(LinkState
                                .valueOf(linkEvent.subject().state().name()))
                        .setType(LinkType.valueOf(linkEvent.subject().type().name()))
                        .setDst(ConnectPoint.newBuilder()
                                .setDeviceId(linkEvent.subject().dst()
                                        .deviceId().toString())
                                .setPortNumber(linkEvent.subject().dst().port()
                                        .toString()))
                        .setSrc(ConnectPoint.newBuilder()
                                .setDeviceId(linkEvent.subject().src()
                                        .deviceId().toString())
                                .setPortNumber(linkEvent.subject().src().port()
                                        .toString())))
                .build();

        return notification;
    }

    /**
     * Returns the specific Kafka Device Event Type for the corresponding ONOS
     * Device Event Type.
     *
     * @param event ONOS Device Event
     * @return Kafka Device Event Type
     */
    private LinkEventType getProtoType(LinkEvent event) {
        LinkEventType generatedEventType = null;
        LinkEventType[] kafkaEvents = LinkEventType.values();
        for (LinkEventType linkEventType : kafkaEvents) {
            if (linkEventType.name().equals(event.type().name())) {
                generatedEventType = linkEventType;
            }
        }

        return generatedEventType;
    }
}
