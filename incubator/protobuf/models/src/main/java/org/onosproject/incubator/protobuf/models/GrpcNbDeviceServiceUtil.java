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
package org.onosproject.incubator.protobuf.models;

import org.onlab.packet.ChassisId;
import org.onosproject.grpc.net.device.models.DeviceDescriptionProtoOuterClass;
import org.onosproject.grpc.net.device.models.DeviceDescriptionProtoOuterClass.DeviceDescriptionProto;
import org.onosproject.grpc.net.device.models.DeviceEnumsProto.DeviceTypeProto;
import org.onosproject.grpc.net.device.models.DeviceEnumsProto.MastershipRoleProto;
import org.onosproject.grpc.net.device.models.PortDescriptionProtoOuterClass.PortDescriptionProto;
import org.onosproject.grpc.net.device.models.PortEnumsProto;

import org.onosproject.grpc.net.device.models.PortStatisticsProtoOuterClass;
import org.onosproject.grpc.net.device.models.PortStatisticsProtoOuterClass.PortStatisticsProto;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device.Type;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * gRPC message conversion related utilities for device service.
 */
public final class GrpcNbDeviceServiceUtil {

    private static final Logger log = LoggerFactory.getLogger(GrpcNbDeviceServiceUtil.class);

    /**
     * Translates gRPC enum MastershipRole to ONOS enum.
     *
     * @param role mastership role in gRPC enum
     * @return equivalent in ONOS enum
     */
    public static MastershipRole translate(MastershipRoleProto role) {
        switch (role) {
            case NONE:
                return MastershipRole.NONE;
            case MASTER:
                return MastershipRole.MASTER;
            case STANDBY:
                return MastershipRole.STANDBY;
            case UNRECOGNIZED:
                log.warn("Unrecognized MastershipRole gRPC message: {}", role);
                return MastershipRole.NONE;
            default:
                return MastershipRole.NONE;
        }
    }

    /**
     * Translates ONOS enum MastershipRole to gRPC enum.
     *
     * @param newRole ONOS' mastership role
     * @return equivalent in gRPC message enum
     */
    public static MastershipRoleProto translate(MastershipRole newRole) {
        switch (newRole) {
            case MASTER:
                return MastershipRoleProto.MASTER;
            case STANDBY:
                return MastershipRoleProto.STANDBY;
            case NONE:
            default:
                return MastershipRoleProto.NONE;
        }
    }


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
                asAnnotations(deviceDescription.getAnnotationsMap()));
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
                .putAllAnnotations(asMap(deviceDescription.annotations()))
                .build();
    }


    /**
     * Translates gRPC DeviceType to {@link Type}.
     *
     * @param type      gRPC message
     * @return  {@link Type}
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
     * @return  gRPC message
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

    /**
     * Translates gRPC PortDescription message to {@link PortDescription}.
     *
     * @param portDescription gRPC message
     * @return {@link PortDescription}
     */
    public static PortDescription translate(PortDescriptionProto portDescription) {
        PortNumber number = PortNumber.fromString(portDescription.getPortNumber());
        boolean isEnabled = portDescription.getIsEnabled();
        Port.Type type = translate(portDescription.getType());
        long portSpeed = portDescription.getPortSpeed();
        SparseAnnotations annotations = asAnnotations(portDescription.getAnnotationsMap());
        // TODO How to deal with more specific Port...
        return new DefaultPortDescription(number, isEnabled, type, portSpeed, annotations);
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
                .putAllAnnotations(asMap(portDescription.annotations()))
                .build();
    }

    /**
     * Translates gRPC PortType to {@link Port.Type}.
     *
     * @param type      gRPC message
     * @return  {@link Port.Type}
     */
    public static Port.Type translate(PortEnumsProto.PortTypeProto type) {
        switch (type) {
            case COPPER:
                return Port.Type.COPPER;
            case FIBER:
                return Port.Type.FIBER;
            case OCH:
                return Port.Type.OCH;
            case ODUCLT:
                return Port.Type.ODUCLT;
            case OMS:
                return Port.Type.OMS;
            case PACKET:
                return Port.Type.PACKET;
            case VIRTUAL_PORT:
                return Port.Type.VIRTUAL;

            case UNRECOGNIZED:
            default:
                log.warn("Unexpected PortType: {}", type);
                return Port.Type.COPPER;
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
                return PortEnumsProto.PortTypeProto.COPPER;
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
                .setPort(portStatistics.getPort())
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
                .setPort(portStatistics.port())
                .setPacketsReceived(portStatistics.packetsReceived())
                .setPacketsSent(portStatistics.packetsSent())
                .build();
    }

    // may be this can be moved to Annotation itself or AnnotationsUtils
    /**
     * Converts Annotations to Map of Strings.
     *
     * @param annotations {@link Annotations}
     * @return Map of annotation key and values
     */
    public static Map<String, String> asMap(Annotations annotations) {
        if (annotations instanceof DefaultAnnotations) {
            return ((DefaultAnnotations) annotations).asMap();
        }
        Map<String, String> map = new HashMap<>();
        annotations.keys()
                .forEach(k -> map.put(k, annotations.value(k)));

        return map;
    }

    // may be this can be moved to Annotation itself or AnnotationsUtils
    /**
     * Converts Map of Strings to {@link SparseAnnotations}.
     *
     * @param annotations Map of annotation key and values
     * @return {@link SparseAnnotations}
     */
    public static SparseAnnotations asAnnotations(Map<String, String> annotations) {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        annotations.entrySet().forEach(e -> {
            if (e.getValue() != null) {
                builder.set(e.getKey(), e.getValue());
            } else {
                builder.remove(e.getKey());
            }
        });
        return builder.build();
    }

    // Utility class not intended for instantiation.
    private GrpcNbDeviceServiceUtil() {}
}
