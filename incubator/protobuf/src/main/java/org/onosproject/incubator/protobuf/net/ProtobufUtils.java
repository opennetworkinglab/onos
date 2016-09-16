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
package org.onosproject.incubator.protobuf.net;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.onlab.packet.ChassisId;
import org.onosproject.grpc.net.Device.DeviceType;
import org.onosproject.grpc.net.Port.PortType;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.Port.Type;
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

import com.google.common.annotations.Beta;


/**
 * gRPC message conversion related utilities.
 */
@Beta
public final class ProtobufUtils {

    private static final Logger log = LoggerFactory.getLogger(ProtobufUtils.class);

    /**
     * Translates gRPC enum MastershipRole to ONOS enum.
     *
     * @param role mastership role in gRPC enum
     * @return equivalent in ONOS enum
     */
    public static MastershipRole translate(org.onosproject.grpc.net.Device.MastershipRole role) {
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
    public static org.onosproject.grpc.net.Device.MastershipRole translate(MastershipRole newRole) {
        switch (newRole) {
        case MASTER:
            return org.onosproject.grpc.net.Device.MastershipRole.MASTER;
        case STANDBY:
            return org.onosproject.grpc.net.Device.MastershipRole.STANDBY;
        case NONE:
        default:
            return org.onosproject.grpc.net.Device.MastershipRole.NONE;
        }
    }


    /**
     * Translates gRPC DeviceDescription to {@link DeviceDescription}.
     *
     * @param deviceDescription gRPC message
     * @return {@link DeviceDescription}
     */
    public static DeviceDescription translate(org.onosproject.grpc.net.Device.DeviceDescription deviceDescription) {
        URI uri = URI.create(deviceDescription.getDeviceUri());
        Device.Type type = translate(deviceDescription.getType());
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
    public static org.onosproject.grpc.net.Device.DeviceDescription translate(DeviceDescription deviceDescription) {

        return org.onosproject.grpc.net.Device.DeviceDescription.newBuilder()
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
     * Translates gRPC DeviceType to {@link Device.Type}.
     *
     * @param type      gRPC message
     * @return  {@link Device.Type}
     */
    public static Device.Type translate(org.onosproject.grpc.net.Device.DeviceType type) {
        switch (type) {
        case BALANCER:
            return Device.Type.BALANCER;
        case CONTROLLER:
            return Device.Type.CONTROLLER;
        case FIBER_SWITCH:
            return Device.Type.FIBER_SWITCH;
        case FIREWALL:
            return Device.Type.FIREWALL;
        case IDS:
            return Device.Type.IDS;
        case IPS:
            return Device.Type.IPS;
        case MICROWAVE:
            return Device.Type.MICROWAVE;
        case OTHER:
            return Device.Type.OTHER;
        case OTN:
            return Device.Type.OTN;
        case ROADM:
            return Device.Type.ROADM;
        case ROADM_OTN:
            return Device.Type.ROADM_OTN;
        case ROUTER:
            return Device.Type.ROUTER;
        case SWITCH:
            return Device.Type.SWITCH;
        case VIRTUAL:
            return Device.Type.VIRTUAL;

        case UNRECOGNIZED:
        default:
            log.warn("Unexpected DeviceType: {}", type);
            return Device.Type.OTHER;
        }
    }

    /**
     * Translates {@link Type} to gRPC DeviceType.
     *
     * @param type {@link Type}
     * @return  gRPC message
     */
    public static DeviceType translate(Device.Type type) {
        switch (type) {
        case BALANCER:
            return DeviceType.BALANCER;
        case CONTROLLER:
            return DeviceType.CONTROLLER;
        case FIBER_SWITCH:
            return DeviceType.FIBER_SWITCH;
        case FIREWALL:
            return DeviceType.FIREWALL;
        case IDS:
            return DeviceType.IDS;
        case IPS:
            return DeviceType.IPS;
        case MICROWAVE:
            return DeviceType.MICROWAVE;
        case OTHER:
            return DeviceType.OTHER;
        case OTN:
            return DeviceType.OTN;
        case ROADM:
            return DeviceType.ROADM;
        case ROADM_OTN:
            return DeviceType.ROADM_OTN;
        case ROUTER:
            return DeviceType.ROUTER;
        case SWITCH:
            return DeviceType.SWITCH;
        case VIRTUAL:
            return DeviceType.VIRTUAL;

        default:
            log.warn("Unexpected Device.Type: {}", type);
            return DeviceType.OTHER;
        }
    }

    /**
     * Translates gRPC PortDescription message to {@link PortDescription}.
     *
     * @param portDescription gRPC message
     * @return {@link PortDescription}
     */
    public static PortDescription translate(org.onosproject.grpc.net.Port.PortDescription portDescription) {
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
    public static org.onosproject.grpc.net.Port.PortDescription translate(PortDescription portDescription) {
        return org.onosproject.grpc.net.Port.PortDescription.newBuilder()
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
    public static Port.Type translate(PortType type) {
        switch (type) {
        case COPPER:
            return Type.COPPER;
        case FIBER:
            return Type.FIBER;
        case OCH:
            return Type.OCH;
        case ODUCLT:
            return Type.ODUCLT;
        case OMS:
            return Type.OMS;
        case PACKET:
            return Type.PACKET;
        case VIRTUAL:
            return Type.VIRTUAL;

        case UNRECOGNIZED:
        default:
            log.warn("Unexpected PortType: {}", type);
            return Type.COPPER;
        }
    }

    /**
     * Translates {@link Port.Type} to gRPC PortType.
     *
     * @param type      {@link Port.Type}
     * @return  gRPC message
     */
    public static PortType translate(Port.Type type) {
        switch (type) {
        case COPPER:
            return PortType.COPPER;
        case FIBER:
            return PortType.FIBER;
        case OCH:
            return PortType.OCH;
        case ODUCLT:
            return PortType.ODUCLT;
        case OMS:
            return PortType.OMS;
        case PACKET:
            return PortType.PACKET;
        case VIRTUAL:
            return PortType.VIRTUAL;

        default:
            log.warn("Unexpected Port.Type: {}", type);
            return PortType.COPPER;
        }
    }

    /**
     * Translates gRPC PortStatistics message to {@link PortStatistics}.
     *
     * @param portStatistics gRPC PortStatistics message
     * @return {@link PortStatistics}
     */
    public static PortStatistics translate(org.onosproject.grpc.net.Port.PortStatistics portStatistics) {
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
    public static org.onosproject.grpc.net.Port.PortStatistics translate(PortStatistics portStatistics) {
        // TODO implement adding missing fields
        return org.onosproject.grpc.net.Port.PortStatistics.newBuilder()
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
    private ProtobufUtils() {}
}
