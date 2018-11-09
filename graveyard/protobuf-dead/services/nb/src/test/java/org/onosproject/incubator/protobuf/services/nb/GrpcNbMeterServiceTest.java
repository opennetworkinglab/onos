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

package org.onosproject.incubator.protobuf.services.nb;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.grpc.core.models.ApplicationIdProtoOuterClass;
import org.onosproject.grpc.nb.net.meter.MeterServiceGrpc;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto;
import org.onosproject.grpc.net.meter.models.BandProtoOuterClass;
import org.onosproject.grpc.net.meter.models.MeterEnumsProto;
import org.onosproject.grpc.net.meter.models.MeterProtoOuterClass;
import org.onosproject.grpc.net.meter.models.MeterRequestProtoOuterClass;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterListener;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;


public class GrpcNbMeterServiceTest {

    private static InProcessServer<BindableService> inProcessServer;
    private static ManagedChannel channel;
    private static MeterServiceGrpc.MeterServiceBlockingStub blockingStub;

    private static final MeterService MOCK_METER = new MockMeterService();

    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("d1");
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("d2");
    private static final DeviceId DEVICE_ID_3 = DeviceId.deviceId("d3");

    private static final long METER_ID_1 = 1L;
    private static final long METER_ID_2 = 2L;
    private static final long METER_ID_3 = 3L;
    private static final long METER_ID_4 = 4L;

    private static final Meter METER_1 = new MockMeter(DEVICE_ID_1, 1, METER_ID_1, 1);
    private static final Meter METER_2 = new MockMeter(DEVICE_ID_2, 1, METER_ID_2, 2);
    private static final Meter METER_3 = new MockMeter(DEVICE_ID_3, 1, METER_ID_3, 3);
    private static final Meter METER_4 = new MockMeter(DEVICE_ID_3, 1, METER_ID_4, 4);

    private static Set<Meter> allMeters = new HashSet<>();

    /**
     * Create inProcessServer and bind grpcNbMeterService.
     *
     * @throws IllegalAccessException
     * @throws IOException
     * @throws InstantiationException
     */
    @BeforeClass
    public static void setup() throws IllegalAccessException, IOException, InstantiationException {

        GrpcNbMeterService grpcNbMeterService = new GrpcNbMeterService();
        grpcNbMeterService.meterService = MOCK_METER;
        inProcessServer = new InProcessServer(GrpcNbMeterService.class);
        inProcessServer.addServiceToBind(grpcNbMeterService);

        inProcessServer.start();
        channel = InProcessChannelBuilder.forName("test").directExecutor()
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true).build();
        blockingStub = MeterServiceGrpc.newBlockingStub(channel);

        allMeters.add(METER_2);
        allMeters.add(METER_3);
        allMeters.add(METER_4);
    }

    @AfterClass
    public static void down() {
        channel.shutdown();

        inProcessServer.stop();
    }

    @Test
    public void testSubmit() {
        MeterServiceNbProto.submitRequest request = MeterServiceNbProto.submitRequest.newBuilder()
                .setMeter(MeterRequestProtoOuterClass.MeterRequestProto.newBuilder()
                        .setDeviceId(DEVICE_ID_1.toString())
                        .setApplicationId(ApplicationIdProtoOuterClass.ApplicationIdProto.newBuilder()
                                .setId(METER_1.appId().id())
                                .build())
                        .setUnit(MeterEnumsProto.MeterUnitProto.KB_PER_SEC)
                        .setIsBurst(false)
                        .addBands(BandProtoOuterClass.BandProto.getDefaultInstance())
                        .setType(MeterEnumsProto.MeterRequestTypeProto.ADD)
                        .build())
                .build();
        MeterServiceNbProto.submitReply reply;

        int size = allMeters.size();
        reply = blockingStub.submit(request);
        MeterProtoOuterClass.MeterProto meter = reply.getSubmitMeter();
        assertTrue(allMeters.size() == (size + 1)
                && meter.getDeviceId().equals(METER_1.deviceId().toString())
                && meter.getApplicationId().getId() == METER_1.appId().id());

    }

    @Test
    public void testWithdraw() {
        MeterServiceNbProto.withdrawRequest request = MeterServiceNbProto.withdrawRequest.newBuilder()
                .setMeter(MeterRequestProtoOuterClass.MeterRequestProto.newBuilder()
                        .setDeviceId(DEVICE_ID_2.toString())
                        .setApplicationId(ApplicationIdProtoOuterClass.ApplicationIdProto.newBuilder()
                                .setId(1)
                                .build())
                        .setUnit(MeterEnumsProto.MeterUnitProto.KB_PER_SEC)
                        .setIsBurst(false)
                        .addBands(BandProtoOuterClass.BandProto.getDefaultInstance())
                        .setType(MeterEnumsProto.MeterRequestTypeProto.REMOVE)
                        .build())
                .setMeterId(METER_ID_2)
                .build();
        MeterServiceNbProto.withdrawReply reply;

        int size = allMeters.size();
        reply = blockingStub.withdraw(request);
        assertTrue(allMeters.size() == (size - 1));
    }

    @Test
    public void testGetMeter() {
        MeterServiceNbProto.getMeterRequest request = MeterServiceNbProto.getMeterRequest.newBuilder()
                .setDeviceId(DEVICE_ID_3.toString())
                .setMeterId(METER_ID_3)
                .build();
        MeterServiceNbProto.getMeterReply reply;

        reply = blockingStub.getMeter(request);
        MeterProtoOuterClass.MeterProto meter = reply.getMeter();
        assertTrue(meter.getApplicationId().getId() == METER_3.appId().id()
                && meter.getDeviceId().equals(DEVICE_ID_3.toString()));
    }

    @Test
    public void testGetAllMeters() {
        MeterServiceNbProto.getAllMetersRequest request = MeterServiceNbProto.getAllMetersRequest.getDefaultInstance();
        MeterServiceNbProto.getAllMetersReply reply;

        reply = blockingStub.getAllMeters(request);
        assertTrue(reply.getMetersCount() == allMeters.size());

    }

    @Test
    public void testGetMeters() {
        MeterServiceNbProto.getMetersRequest request = MeterServiceNbProto.getMetersRequest.newBuilder()
                .setDeviceId(DEVICE_ID_3.toString())
                .build();
        MeterServiceNbProto.getMetersReply reply;

        reply = blockingStub.getMeters(request);
        assertTrue(reply.getMetersCount() == 2);
    }


    /**
     * A mock class of meter.
     */
    private static class MockMeter implements Meter {

        final DeviceId deviceId;
        final ApplicationId appId;
        final MeterId meterId;
        final long baseValue;
        final List<Band> bandList;

        public MockMeter(DeviceId deviceId, int appId, long meterId, int id) {
            this.deviceId = deviceId;
            this.appId = new DefaultApplicationId(appId, String.valueOf(appId));
            this.baseValue = id * 200L;
            this.meterId = MeterId.meterId(meterId);

            Band band = DefaultBand.builder()
                    .ofType(Band.Type.REMARK)
                    .withRate(10)
                    .dropPrecedence((short) 20)
                    .burstSize(30).build();

            this.bandList = new ArrayList<>();
            this.bandList.add(band);
        }

        @Override
        public DeviceId deviceId() {
            return this.deviceId;
        }

        @Override
        public MeterId id() {
            return this.meterId;
        }

        @Override
        public MeterCellId meterCellId() {
            return this.id();
        }

        @Override
        public ApplicationId appId() {
            return this.appId;
        }

        @Override
        public Unit unit() {
            return Unit.KB_PER_SEC;
        }

        @Override
        public boolean isBurst() {
            return false;
        }

        @Override
        public Collection<Band> bands() {
            return this.bandList;
        }

        @Override
        public MeterState state() {
            return MeterState.ADDED;
        }

        @Override
        public long life() {
            return baseValue + 11;
        }

        @Override
        public long referenceCount() {
            return baseValue + 22;
        }

        @Override
        public long packetsSeen() {
            return baseValue + 33;
        }

        @Override
        public long bytesSeen() {
            return baseValue + 44;
        }
    }

    /**
     * A mock class of MeterService.
     */
    public static class MockMeterService implements MeterService {
        @Override
        public void addListener(MeterListener listener) {
        }

        @Override
        public void removeListener(MeterListener listener) {
        }

        @Override
        public Meter submit(MeterRequest meter) {
            Meter m = new MockMeter(meter.deviceId(), meter.appId().id(), METER_ID_1, 1);
            allMeters.add(m);
            return m;
        }

        @Override
        public void withdraw(MeterRequest meter, MeterId meterId) {
            Meter toRemove = null;
            for (Meter m: allMeters) {
                if (meter.appId().id() == m.appId().id() && meter.deviceId().equals(m.deviceId())
                        && m.id().equals(meterId)) {
                    toRemove = m;
                    break;
                }
            }
            if (null != toRemove) {
                allMeters.remove(toRemove);
            }
        }

        @Override
        public Meter getMeter(DeviceId deviceId, MeterId id) {
            for (Meter m: allMeters) {
                if (deviceId.equals(m.deviceId()) && m.id().equals(id)) {
                    return m;
                }
            }
            return null;
        }

        @Override
        public Collection<Meter> getAllMeters() {
            return allMeters;
        }

        @Override
        public Collection<Meter> getMeters(DeviceId deviceId) {
            List<Meter> meters = new ArrayList<>();
            for (Meter m: allMeters) {
                if (deviceId.equals(m.deviceId())) {
                    meters.add(m);
                }
            }
            return meters;
        }

        @Override
        public MeterId allocateMeterId(DeviceId deviceId) {
            return null;
        }

        @Override
        public void freeMeterId(DeviceId deviceId, MeterId meterId) {

        }
    }

}
