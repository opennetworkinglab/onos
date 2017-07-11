/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;

/**
 * Packet Programmable behaviour for BMv2 devices.
 */
public class Bmv2PacketProgrammable extends AbstractHandlerBehaviour implements PacketProgrammable {

    @Override
    public void emit(OutboundPacket packet) {
        // TODO: implement using P4runtime client.
        // DriverHandler handler = handler();
        // GrpcController controller = handler.get(GrpcController.class);
        // DeviceId deviceId = handler.data().deviceId();
        // GrpcChannelId channelId = GrpcChannelId.of(deviceId, "bmv2");
        // GrpcServiceId serviceId = GrpcServiceId.of(channelId, "p4runtime");
        // GrpcStreamObserverId observerId = GrpcStreamObserverId.of(serviceId,
        //         this.getClass().getSimpleName());
        // Optional<GrpcObserverHandler> manager = controller.getObserverManager(observerId);
        // if (!manager.isPresent()) {
        //     //this is the first time the behaviour is called
        //     controller.addObserver(observerId, new Bmv2PacketInObserverHandler());
        // }
        // //other already registered the observer for us.
        // Optional<StreamObserver> observer = manager.get().requestStreamObserver();
        // observer.ifPresent(objectStreamObserver -> objectStreamObserver.onNext(packet));
    }
}
