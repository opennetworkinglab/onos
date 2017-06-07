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
package org.onosproject.grpc.api;

import com.google.common.annotations.Beta;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.Optional;

/**
 * Implementation to add or remove an observer to the managed channel.
 *
 */
@Beta
public interface GrpcObserverHandler {
    /**
     * The implementation of this method adds an
     * observer on a stub generated on the specific channel.
     * This method will be called by the gRPC controller.
     *
     * @param channel the channel from which to derive the stub.
     */
    void bindObserver(ManagedChannel channel);

    /**
     * The implementation of this method returns the request stream
     * observer, if any, on a stub generated on the specific channel.
     *
     * @return the observer on the stub, empty if observer is server-side unidirectional.
     */
    Optional<StreamObserver> requestStreamObserver();

    /**
     * The implementation of this method removes an
     * observer on a stub generated on the specific channel.
     * This method will be called by the gRPC controller.
     */
    void removeObserver();
}
