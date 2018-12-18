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

import org.onlab.packet.ChassisId;
import org.onosproject.grpc.net.device.models.DeviceDescriptionProtoOuterClass;
import org.onosproject.grpc.net.device.models.DeviceDescriptionProtoOuterClass.DeviceDescriptionProto;
import org.onosproject.grpc.net.device.models.DeviceEnumsProto.DeviceTypeProto;
import org.onosproject.incubator.protobuf.models.net.AnnotationsTranslator;
import org.onosproject.net.Device.Type;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * gRPC message conversion related utilities for device service.
 */
public final class DeviceProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(DeviceProtoTranslator.class);

    /**
     * Translates gRPC DeviceDescription to {@link DeviceDescriptionProtoOuterClass}.
     *
     * @param deviceDescription gRPC message
     * @return {@link DeviceDescriptionProtoOuterClass}
     */
    public static DeviceDescription translate(
            DeviceDescriptionProto deviceDescription) {
        URI uri = URI.create(deviceDescription.getDeviceUri());
        Type type = translate(deviceDescription.getType());
        String manufacturer = deviceDescription.getManufacturer();
        String hwVersion = deviceDescription.getHwVersion();
        String swVersion = deviceDescription.getSwVersion();
        String serialNumber = deviceDescription.getSerialNumber();
        ChassisId chassis = new ChassisId(deviceDescription.getChassisId());
        boolean defaultAvailable = deviceDescription.getIsDefaultAvailable();
        return new DefaultDeviceDescription(uri, type, manufacturer,
                hwVersion, swVersion, serialNumber,
                chassis,
                defaultAvailable,
                AnnotationsTranslator.asAnnotations(deviceDescription.getAnnotationsMap()));
    }

    /**
     * Translates {@link DeviceDescription} to gRPC DeviceDescription message.
     *
     * @param deviceDescription {@link DeviceDescription}
     * @return gRPC DeviceDescription message
     */
    public static DeviceDescriptionProto translate(
            DeviceDescription deviceDescription) {

        return DeviceDescriptionProto.newBuilder()
                .setDeviceUri(deviceDescription.deviceUri().toString())
                .setType(translate(deviceDescription.type()))
                .setManufacturer(deviceDescription.manufacturer())
                .setHwVersion(deviceDescription.hwVersion())
                .setSwVersion(deviceDescription.swVersion())
                .setSerialNumber(deviceDescription.serialNumber())
                .setChassisId(deviceDescription.chassisId().toString())
                .setIsDefaultAvailable(deviceDescription.isDefaultAvailable())
                .putAllAnnotations(AnnotationsTranslator.asMap(deviceDescription.annotations()))
                .build();
    }


    /**
     * Translates gRPC DeviceType to {@link Type}.
     *
     * @param type gRPC message
     * @return {@link Type}
     */
    public static Type translate(DeviceTypeProto type) {
        switch (type) {
            case BALANCER:
                return Type.BALANCER;
            case CONTROLLER:
                return Type.CONTROLLER;
            case FIBER_SWITCH:
                return Type.FIBER_SWITCH;
            case FIREWALL:
                return Type.FIREWALL;
            case IDS:
                return Type.IDS;
            case IPS:
                return Type.IPS;
            case MICROWAVE:
                return Type.MICROWAVE;
            case OTHER:
                return Type.OTHER;
            case OTN:
                return Type.OTN;
            case ROADM:
                return Type.ROADM;
            case ROADM_OTN:
                return Type.ROADM_OTN;
            case ROUTER:
                return Type.ROUTER;
            case SWITCH:
                return Type.SWITCH;
            case OLS:
                return Type.OLS;
            case VIRTUAL_DEVICE:
                return Type.VIRTUAL;

            case UNRECOGNIZED:
            default:
                log.warn("Unexpected DeviceType: {}", type);
                return Type.OTHER;
        }
    }

    /**
     * Translates {@link Type} to gRPC DeviceType.
     *
     * @param type {@link Type}
     * @return gRPC message
     */
    public static DeviceTypeProto translate(Type type) {
        switch (type) {
            case BALANCER:
                return DeviceTypeProto.BALANCER;
            case CONTROLLER:
                return DeviceTypeProto.CONTROLLER;
            case FIBER_SWITCH:
                return DeviceTypeProto.FIBER_SWITCH;
            case FIREWALL:
                return DeviceTypeProto.FIREWALL;
            case IDS:
                return DeviceTypeProto.IDS;
            case IPS:
                return DeviceTypeProto.IPS;
            case MICROWAVE:
                return DeviceTypeProto.MICROWAVE;
            case OTHER:
                return DeviceTypeProto.OTHER;
            case OTN:
                return DeviceTypeProto.OTN;
            case ROADM:
                return DeviceTypeProto.ROADM;
            case ROADM_OTN:
                return DeviceTypeProto.ROADM_OTN;
            case ROUTER:
                return DeviceTypeProto.ROUTER;
            case SWITCH:
                return DeviceTypeProto.SWITCH;
            case VIRTUAL:
                return DeviceTypeProto.VIRTUAL_DEVICE;

            default:
                log.warn("Unexpected Device.Type: {}", type);
                return DeviceTypeProto.OTHER;
        }
    }

    // Utility class not intended for instantiation.
    private DeviceProtoTranslator() {
    }
}
