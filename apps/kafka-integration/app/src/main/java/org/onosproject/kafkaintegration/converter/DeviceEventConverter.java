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

import org.onosproject.event.Event;
import org.onosproject.grpc.net.device.models.DeviceEnumsProto.DeviceEventTypeProto;
import org.onosproject.grpc.net.device.models.DeviceEnumsProto.DeviceTypeProto;
import org.onosproject.grpc.net.device.models.DeviceEventProto.DeviceNotificationProto;
import org.onosproject.grpc.net.device.models.PortEnumsProto;
import org.onosproject.grpc.net.models.DeviceProtoOuterClass.DeviceProto;
import org.onosproject.grpc.net.models.PortProtoOuterClass;
import org.onosproject.incubator.protobuf.models.net.AnnotationsTranslator;
import org.onosproject.net.device.DeviceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts ONOS Device event message to protobuf format.
 */
public class DeviceEventConverter implements EventConverter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public byte[] convertToProtoMessage(Event<?, ?> event) {

        DeviceEvent deviceEvent = (DeviceEvent) event;

        if (!deviceEventTypeSupported(deviceEvent)) {
            log.error("Unsupported Onos Device Event {}. There is no matching"
                              + "proto Device Event type", deviceEvent.type().toString());
            return null;
        }

        return ((GeneratedMessageV3) buildDeviceProtoMessage(deviceEvent)).toByteArray();
    }

    /**
     * Checks if the ONOS Device Event type is supported.
     *
     * @param event ONOS Device event
     * @return true if there is a match and false otherwise
     */
    private boolean deviceEventTypeSupported(DeviceEvent event) {
        DeviceEventTypeProto[] deviceEvents = DeviceEventTypeProto.values();
        for (DeviceEventTypeProto deviceEventType : deviceEvents) {
            if (deviceEventType.name().equals(event.type().name())) {
                return true;
            }
        }

        return false;
    }

    private DeviceNotificationProto buildDeviceProtoMessage(DeviceEvent deviceEvent) {
        DeviceNotificationProto.Builder notificationBuilder =
                DeviceNotificationProto.newBuilder();

        DeviceProto deviceCore =
                DeviceProto.newBuilder()
                        .setChassisId(deviceEvent.subject().chassisId().id()
                                              .toString())
                        .setDeviceId(deviceEvent.subject().id().toString())
                        .setHwVersion(deviceEvent.subject().hwVersion())
                        .setManufacturer(deviceEvent.subject().manufacturer())
                        .setSerialNumber(deviceEvent.subject().serialNumber())
                        .setSwVersion(deviceEvent.subject().swVersion())
                        .setType(DeviceTypeProto
                                         .valueOf(deviceEvent.subject().type().name()))
                        .putAllAnnotations(AnnotationsTranslator.asMap(deviceEvent.subject().annotations()))
                        .build();

        PortProtoOuterClass.PortProto portProto = null;
        if (deviceEvent.port() != null) {
            portProto =
                    PortProtoOuterClass.PortProto.newBuilder()
                            .setIsEnabled(deviceEvent.port().isEnabled())
                            .setPortNumber(deviceEvent.port().number()
                                                   .toString())
                            .setPortSpeed(deviceEvent.port().portSpeed())
                            .setType(PortEnumsProto.PortTypeProto
                                             .valueOf(deviceEvent.port().type().name()))
                            .build();

            notificationBuilder.setPort(portProto);
        }

        notificationBuilder.setDeviceEventType(getProtoType(deviceEvent))
                .setDevice(deviceCore);

        return notificationBuilder.build();
    }

    /**
     * Retrieves the protobuf generated device event type.
     *
     * @param event ONOS Device Event
     * @return generated Device Event Type
     */
    private DeviceEventTypeProto getProtoType(DeviceEvent event) {
        DeviceEventTypeProto protobufEventType = null;
        DeviceEventTypeProto[] deviceEvents = DeviceEventTypeProto.values();
        for (DeviceEventTypeProto deviceEventType : deviceEvents) {
            if (deviceEventType.name().equals(event.type().name())) {
                protobufEventType = deviceEventType;
            }
        }

        return protobufEventType;
    }
}
