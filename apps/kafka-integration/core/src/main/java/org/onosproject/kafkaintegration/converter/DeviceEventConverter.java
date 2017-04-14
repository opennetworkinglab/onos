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

import com.google.protobuf.GeneratedMessageV3;

import org.onosproject.event.Event;
import org.onosproject.grpc.net.Device.DeviceCore;
import org.onosproject.grpc.net.Device.DeviceType;
import org.onosproject.grpc.net.DeviceEvent.DeviceEventType;
import org.onosproject.grpc.net.DeviceEvent.DeviceNotification;
import org.onosproject.grpc.net.Port.PortCore;
import org.onosproject.grpc.net.Port.PortType;
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
        DeviceEventType[] deviceEvents = DeviceEventType.values();
        for (DeviceEventType deviceEventType : deviceEvents) {
            if (deviceEventType.name().equals(event.type().name())) {
                return true;
            }
        }

        return false;
    }

    private DeviceNotification buildDeviceProtoMessage(DeviceEvent deviceEvent) {
        DeviceNotification.Builder notificationBuilder =
                DeviceNotification.newBuilder();

        DeviceCore deviceCore =
                DeviceCore.newBuilder()
                        .setChassisId(deviceEvent.subject().chassisId().id()
                                              .toString())
                        .setDeviceId(deviceEvent.subject().id().toString())
                        .setHwVersion(deviceEvent.subject().hwVersion())
                        .setManufacturer(deviceEvent.subject().manufacturer())
                        .setSerialNumber(deviceEvent.subject().serialNumber())
                        .setSwVersion(deviceEvent.subject().swVersion())
                        .setType(DeviceType
                                         .valueOf(deviceEvent.subject().type().name()))
                        .build();

        PortCore portCore = null;
        if (deviceEvent.port() != null) {
            portCore =
                    PortCore.newBuilder()
                            .setIsEnabled(deviceEvent.port().isEnabled())
                            .setPortNumber(deviceEvent.port().number()
                                                   .toString())
                            .setPortSpeed(deviceEvent.port().portSpeed())
                            .setType(PortType
                                             .valueOf(deviceEvent.port().type().name()))
                            .build();

            notificationBuilder.setPort(portCore);
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
    private DeviceEventType getProtoType(DeviceEvent event) {
        DeviceEventType protobufEventType = null;
        DeviceEventType[] deviceEvents = DeviceEventType.values();
        for (DeviceEventType deviceEventType : deviceEvents) {
            if (deviceEventType.name().equals(event.type().name())) {
                protobufEventType = deviceEventType;
            }
        }

        return protobufEventType;
    }
}
