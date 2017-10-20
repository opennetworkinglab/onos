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

import com.google.common.annotations.Beta;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.grpc.nb.net.region.RegionServiceGrpc.RegionServiceImplBase;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionDevicesReply;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionDevicesRequest;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionForDeviceReply;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionForDeviceRequest;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionHostsReply;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionHostsRequest;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionReply;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionRequest;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionsReply;
import org.onosproject.grpc.nb.net.region.RegionServiceNb.getRegionsRequest;
import org.onosproject.incubator.protobuf.models.net.RegionProtoTranslator;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionService;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A server that provides access to the methods exposed by {@link RegionService}.
 */
@Beta
@Component(immediate = true)
public class GrpcNbRegionService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RegionService regionService;

    private RegionServiceNbServerInternal innerClassInstance = null;

    @Activate
    public void activate() {

        registry.register(getInnerClassInstance());
        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {

        registry.unregister(getInnerClassInstance());
        log.info("Stopped.");
    }

    /**
     * Register Region Service, used for unit testing purposes.
     *
     * @return An instance of binding Region service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(GrpcNbRegionService.RegionServiceNbServerInternal.class);
        inprocessServer.addServiceToBind(getInnerClassInstance());

        return inprocessServer;
    }

    private final class RegionServiceNbServerInternal extends RegionServiceImplBase {
        /**
         * Service for interacting with inventory of network control regions.
         */
        @Override
        public void getRegions(getRegionsRequest request,
                               StreamObserver<getRegionsReply> responseObserver) {
            getRegionsReply.Builder builder = getRegionsReply.newBuilder();
            regionService.getRegions().forEach(r -> {
                builder.addRegion(RegionProtoTranslator.translate(r));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getRegion(getRegionRequest request,
                              StreamObserver<getRegionReply> responseObserver) {
            RegionId regionId = RegionId.regionId(request.getRegionId());
            Region region = regionService.getRegion(regionId);

            getRegionReply reply = getRegionReply.newBuilder()
                    .setRegion(RegionProtoTranslator.translate(region)).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void getRegionForDevice(getRegionForDeviceRequest request,
                                       StreamObserver<getRegionForDeviceReply> responseObserver) {
            DeviceId deviceId = DeviceId.deviceId(request.getDeviceId());
            Region region = regionService.getRegionForDevice(deviceId);

            getRegionForDeviceReply reply = getRegionForDeviceReply.newBuilder()
                    .setRegion(RegionProtoTranslator.translate(region)).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void getRegionDevices(getRegionDevicesRequest request,
                                     StreamObserver<getRegionDevicesReply> responseObserver) {
            RegionId regionId = RegionId.regionId(request.getRegionId());
            getRegionDevicesReply.Builder builder = getRegionDevicesReply.newBuilder();

            regionService.getRegionDevices(regionId).forEach(d -> {
                builder.addDeviceId(d.toString());
            });

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getRegionHosts(getRegionHostsRequest request,
                                   StreamObserver<getRegionHostsReply> responseObserver) {
            RegionId regionId = RegionId.regionId(request.getRegionId());
            getRegionHostsReply.Builder builder = getRegionHostsReply.newBuilder();

            regionService.getRegionHosts(regionId).forEach(h -> {
                builder.addHostId(h.toString());
            });

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private RegionServiceNbServerInternal getInnerClassInstance() {
        if (innerClassInstance == null) {
            innerClassInstance = new RegionServiceNbServerInternal();
        }
        return innerClassInstance;
    }
}







