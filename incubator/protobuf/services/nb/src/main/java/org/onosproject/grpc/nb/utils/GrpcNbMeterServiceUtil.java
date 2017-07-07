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
package org.onosproject.grpc.nb.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterState;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.grpc.core.models.ApplicationIdProtoOuterClass.ApplicationIdProto;
import org.onosproject.grpc.net.meter.models.BandProtoOuterClass.BandProto;
import org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterRequestTypeProto;
import org.onosproject.grpc.net.meter.models.MeterRequestProtoOuterClass.MeterRequestProto;
import org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterUnitProto;
import org.onosproject.grpc.net.meter.models.BandEnumsProto.BandTypeProto;
import org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterStateProto;
import org.onosproject.grpc.net.meter.models.MeterProtoOuterClass.MeterProto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import com.google.common.annotations.Beta;

/**
 * gRPC message conversion related utilities for meter service.
 */
@Beta
public final class GrpcNbMeterServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(GrpcNbMeterServiceUtil.class);

    /**
     * Translates gRPC ApplicationId to {@link ApplicationId}.
     *
     * @param gAppId gRPC message
     * @return {@link ApplicationId}
     */
    public static ApplicationId translate(ApplicationIdProto gAppId) {
        int id = gAppId.getId();
        String name = gAppId.getName();
        ApplicationId appId = new DefaultApplicationId(id, name);
        return appId;
    }

    /**
     * Translates gRPC Band to {@link Band}.
     *
     * @param gBand gRPC message
     * @return {@link Band}
     */
    public static Band translate(BandProto gBand) {
        Band.Type type = translate(gBand.getType());
        long rate = gBand.getRate();
        long burstSize = gBand.getBurst();
        short prec = (short) gBand.getDropPrecedence();
        Band band = new DefaultBand(type, rate, burstSize, prec);
        return band;
    }

    /**
     * Translates gRPC List Bands to Collection Band.
     *
     * @param listBands gRPC message
     * @return Collection Band
     */
    public static Collection<Band> translate(java.util.List<BandProto>  listBands) {
        Collection<Band> bands = new ArrayList<>();
        listBands.forEach(d -> {
            bands.add(translate(d));
        });
        return bands;
    }

    /**
     * Translates gRPC MeterRequestType to {@link MeterRequest.Type}.
     *
     * @param type gRPC message
     * @return {@link MeterRequest.Type}
     */
    public static MeterRequest.Type translate(MeterRequestTypeProto type) {
        switch (type) {
            case ADD:
                return MeterRequest.Type.ADD;
            case MODIFY:
                return MeterRequest.Type.MODIFY;
            case REMOVE:
                return MeterRequest.Type.REMOVE;
            case UNRECOGNIZED:
                log.warn("Unrecognized MeterRequest type gRPC message: {}", type);
                return null;
            default:
                log.warn("Unrecognized MeterRequest type gRPC message: {}", type);
                return null;
        }
    }

    /**
     * Translates gRPC MeterRequest to {@link MeterRequest}.
     *
     * @param meterRequest gRPC message
     * @return {@link MeterRequest}
     */
    public static MeterRequest translate(MeterRequestProto meterRequest) {

        DeviceId deviceid = DeviceId.deviceId(meterRequest.getDeviceId());
        ApplicationId appId = translate(meterRequest.getApplicationId());
        Meter.Unit unit = translate(meterRequest.getUnit());
        boolean burst = meterRequest.getIsBurst();
        Collection<Band> bands = translate(meterRequest.getBandsList());
        MeterRequest.Type type = translate(meterRequest.getType());
        if (type == MeterRequest.Type.ADD) {
            return DefaultMeterRequest.builder()
                    .forDevice(deviceid)
                    .fromApp(appId)
                    .withUnit(unit)
                    .withBands(bands)
                    .add();
        } else {
            return DefaultMeterRequest.builder()
                    .forDevice(deviceid)
                    .fromApp(appId)
                    .withUnit(unit)
                    .withBands(bands)
                    .remove();
        }
    }

    /**
     * Translates {@link ApplicationId} to gRPC ApplicationId message.
     *
     * @param appId {@link ApplicationId}
     * @return gRPC ApplicationId message
     */
    public static ApplicationIdProto translate(ApplicationId appId) {
        return ApplicationIdProto.newBuilder()
                .setId(appId.id())
                .setName(appId.name())
                .build();
    }

    /**
     * Translates gRPC enum MeterUnit to ONOS enum.
     *
     * @param unit meterUnit in gRPC enum
     * @return equivalent in ONOS enum
     */
    public static Meter.Unit translate(MeterUnitProto unit) {
        switch (unit) {
            case PKTS_PER_SEC:
                return Meter.Unit.PKTS_PER_SEC;
            case KB_PER_SEC:
                return Meter.Unit.KB_PER_SEC;
            case UNRECOGNIZED:
                log.warn("Unrecognized MeterUnit gRPC message: {}", unit);
                return null;
            default:
                log.warn("Unrecognized MeterUnit gRPC message: {}", unit);
                return null;
        }
    }

    /**
     * Translates ONOS enum Meter.Unit Type to gRPC enum.
     *
     * @param unit Meter.Unit in ONOS enum
     * @return equivalent in gRPC enum
     */
    public static MeterUnitProto translate(Meter.Unit unit) {
        switch (unit) {
            case PKTS_PER_SEC:
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterUnitProto.PKTS_PER_SEC;
            case KB_PER_SEC:
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterUnitProto.KB_PER_SEC;
            default:
                log.warn("Unrecognized MeterUnit ONOS message: {}", unit);
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterUnitProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates gRPC enum Band Type to ONOS enum.
     *
     * @param bandType BandType in gRPC enum
     * @return equivalent in ONOS enum
     */
    public static Band.Type translate(BandTypeProto bandType) {
        switch (bandType) {
            case DROP:
                return Band.Type.DROP;
            case REMARK:
                return Band.Type.REMARK;
            case EXPERIMENTAL:
                return Band.Type.EXPERIMENTAL;
            case UNRECOGNIZED:
                log.warn("Unrecognized BandType gRPC message: {}", bandType);
                return null;
            default:
                log.warn("Unrecognized BandType gRPC message: {}", bandType);
                return null;
        }
    }

    /**
     * Translates ONOS enum Band Type to gRPC enum.
     *
     * @param bandType BandType in ONOS enum
     * @return equivalent in gRPC enum
     */
    public static BandTypeProto translate(Band.Type bandType) {
        switch (bandType) {
            case DROP:
                return org.onosproject.grpc.net.meter.models.BandEnumsProto.BandTypeProto.DROP;
            case REMARK:
                return org.onosproject.grpc.net.meter.models.BandEnumsProto.BandTypeProto.REMARK;
            case EXPERIMENTAL:
                return org.onosproject.grpc.net.meter.models.BandEnumsProto.BandTypeProto.EXPERIMENTAL;
            default:
                log.warn("Unrecognized BandType ONOS message: {}", bandType);
                return null;
        }
    }

    /**
     * Translates ONOS enum MeterState Type to gRPC enum.
     *
     * @param meterState MeterState in ONOS enum
     * @return equivalent in gRPC enum
     */
    public static MeterStateProto translate(MeterState meterState) {
        switch (meterState) {
            case PENDING_ADD:
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterStateProto.PENDING_ADD;
            case ADDED:
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterStateProto.ADDED;
            case PENDING_REMOVE:
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterStateProto.PENDING_REMOVE;
            case REMOVED:
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterStateProto.REMOVED;
            default:
                log.warn("Unrecognized MeterState ONOS message: {}", meterState);
                return org.onosproject.grpc.net.meter.models.MeterEnumsProto.MeterStateProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates {@link Meter} to gRPC MeterCore message.
     *
     * @param meter {@link Meter}
     * @return gRPC MeterCore message
     */
    public static MeterProto translate(Meter meter) {
        return MeterProto.newBuilder()
                .setDeviceId(meter.deviceId().toString())
                .setApplicationId(translate(meter.appId()))
                .setUnit(translate(meter.unit()))
                .setIsBurst(meter.isBurst())
                .addAllBands(meter.bands().stream()
                        .map(b -> BandProto.newBuilder()
                                .setRate(b.rate())
                                .setBurst(b.burst())
                                .setDropPrecedence(b.dropPrecedence())
                                .setType(translate(b.type()))
                                .setPackets(b.packets())
                                .setBytes(b.bytes())
                                .build())
                        .collect(Collectors.toList()))
                .setState(translate(meter.state()))
                .setLife(meter.life())
                .setReferenceCount(meter.referenceCount())
                .setPacketsSeen(meter.packetsSeen())
                .setBytesSeen(meter.bytesSeen())
                .build();
    }

    // Utility class not intended for instantiation.
    private GrpcNbMeterServiceUtil() {
    }
}
