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
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * GrpcStreamObserver identifier suitable as an external key.
 * <p>
 * This class is immutable.</p>
 */
@Beta
public final class GrpcStreamObserverId extends Identifier<String> {

    private GrpcServiceId serviceId;
    private String streamName;

    /**
     * Instantiates a new GrpcStreamObserver id.
     *
     * @param serviceId  the service id
     * @param streamName the name of the stream on that device
     */
    private GrpcStreamObserverId(GrpcServiceId serviceId, String streamName) {
        super(serviceId.toString() + ":" + streamName);
        checkNotNull(serviceId, "service id must not be null");
        checkNotNull(streamName, "stream name must not be null");
        checkArgument(!streamName.isEmpty(), "stream name must not be empty");
        this.serviceId = serviceId;
        this.streamName = streamName;
    }

    /**
     * Returns the id of the service that this stream observer uses.
     *
     * @return the service Id
     */
    public GrpcServiceId serviceId() {
        return serviceId;
    }

    /**
     * Returns the name of this stream.
     *
     * @return the stream name
     */
    public String streamName() {
        return streamName;
    }

    /**
     * Creates a gRPC Stream Observer identifier from the specified service id and
     * stream name provided.
     *
     * @param id         service id
     * @param streamName stream name
     * @return stream name
     */
    public static GrpcStreamObserverId of(GrpcServiceId id, String streamName) {
        return new GrpcStreamObserverId(id, streamName);
    }
}
