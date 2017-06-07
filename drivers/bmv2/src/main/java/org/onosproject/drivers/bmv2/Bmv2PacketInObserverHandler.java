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

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.onosproject.grpc.api.GrpcObserverHandler;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample Implementation of a PacketInObserverHandler.
 * TODO refactor and actually use.
 */
public class Bmv2PacketInObserverHandler implements GrpcObserverHandler {

    private final Logger log = getLogger(getClass());

    //private final AbstractStub asyncStub;

    //FIXME put at object due to p4Runtime compilation problems to be fixed.
    private StreamObserver<Object> requestStreamObserver;

    @Override
    public void bindObserver(ManagedChannel channel) {

        //asyncStub = ProtoGeneratedClass.newStub(channel);

        //reqeustStreamObserver = asyncStub.MethodName(new PacketInObserver());

    }

    @Override
    public Optional<StreamObserver> requestStreamObserver() {
        return Optional.of(requestStreamObserver);
    }

    @Override
    public void removeObserver() {
        //this should complete the two way streaming
        requestStreamObserver.onCompleted();
    }

    private class PacketInObserver implements StreamObserver<Object> {

        @Override
        public void onNext(Object o) {
            log.info("onNext: {}", o.toString());

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    }
}
