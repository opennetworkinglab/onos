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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import p4.v1.P4RuntimeGrpc;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigRequest;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigResponse;
import p4.v1.P4RuntimeOuterClass.ReadRequest;
import p4.v1.P4RuntimeOuterClass.ReadResponse;
import p4.v1.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest;
import p4.v1.P4RuntimeOuterClass.SetForwardingPipelineConfigResponse;
import p4.v1.P4RuntimeOuterClass.StreamMessageResponse;
import p4.v1.P4RuntimeOuterClass.WriteRequest;
import p4.v1.P4RuntimeOuterClass.WriteResponse;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class MockP4RuntimeServer extends P4RuntimeGrpc.P4RuntimeImplBase {
    private CompletableFuture<Void> completeLock;
    private AtomicLong counter;

    // Requests
    private List<WriteRequest> writeReqs;
    private List<ReadRequest> readReqs;
    private List<ReadResponse> readResps;

    /**
     * Expect N times request sent by client.
     *
     * @param times the number of request will sent by client.
     * @return a completable future object, wll complete after client send N times requests.
     */
    public CompletableFuture<Void> expectRequests(long times) {
        counter = new AtomicLong(times);
        completeLock = new CompletableFuture<>();
        readReqs = Lists.newArrayList();
        writeReqs = Lists.newArrayList();
        return completeLock;
    }

    private void complete() {
        if (counter.decrementAndGet() == 0) {
            completeLock.complete(null);
        }
    }

    public void willReturnReadResult(Collection<ReadResponse> readResps) {
        this.readResps = Lists.newArrayList(readResps);
    }

    public List<WriteRequest> getWriteReqs() {
        return writeReqs;
    }

    public List<ReadRequest> getReadReqs() {
        return readReqs;
    }

    @Override
    public void write(WriteRequest request, StreamObserver<WriteResponse> responseObserver) {
        writeReqs.add(request);
        responseObserver.onNext(WriteResponse.getDefaultInstance());
        responseObserver.onCompleted();
        complete();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        readReqs.add(request);
        if (readResps != null && !readResps.isEmpty()) {
            ReadResponse readResp = readResps.remove(0); // get first response
            responseObserver.onNext(readResp);
            responseObserver.onCompleted();
        }
        complete();
    }

    @Override
    public void setForwardingPipelineConfig(SetForwardingPipelineConfigRequest request,
                                            StreamObserver<SetForwardingPipelineConfigResponse> responseObserver) {
        throw new UnsupportedOperationException("Not implement yet");
    }

    @Override
    public void getForwardingPipelineConfig(GetForwardingPipelineConfigRequest request,
                                            StreamObserver<GetForwardingPipelineConfigResponse> responseObserver) {
        throw new UnsupportedOperationException("Not implement yet");
    }

    @Override
    public StreamObserver<P4RuntimeOuterClass.StreamMessageRequest>
    streamChannel(StreamObserver<StreamMessageResponse> responseObserver) {
        // TODO: not implement yet
        return null;
    }
}
