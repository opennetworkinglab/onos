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
 * gRPCService identifier suitable as an external key.
 * <p>
 * This class is immutable.</p>
 */
@Beta
public final class GrpcServiceId extends Identifier<String> {

    private final GrpcChannelId channelId;

    private final String serviceName;

    /**
     * Instantiates a new gRPC Service id.
     *
     * @param channelId   the channel id
     * @param serviceName the name of the service on that channel
     */
    private GrpcServiceId(GrpcChannelId channelId, String serviceName) {
        super(channelId.toString() + ":" + serviceName);
        checkNotNull(channelId, "channel id must not be null");
        checkNotNull(serviceName, "service name must not be null");
        checkArgument(!serviceName.isEmpty(), "service name must not be empty");
        this.channelId = channelId;
        this.serviceName = serviceName;
    }

    /**
     * Returns the id of the channel that this service uses.
     *
     * @return the channel Id
     */
    public GrpcChannelId channelId() {
        return channelId;
    }

    /**
     * Returns the name of this service.
     *
     * @return the service name
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * Creates a gRPC Service identifier from the specified device id and
     * service name provided.
     *
     * @param id          channel id
     * @param serviceName name of the service
     * @return service name
     */
    public static GrpcServiceId of(GrpcChannelId id, String serviceName) {
        return new GrpcServiceId(id, serviceName);
    }
}
