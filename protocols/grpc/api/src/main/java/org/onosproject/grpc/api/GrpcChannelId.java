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

package org.onosproject.grpc.api;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

/**
 * gRPC channel identifier, unique in the scope of an ONOS node.
 */
@Beta
public final class GrpcChannelId extends Identifier<String> {

    private GrpcChannelId(String channelName) {
        super(channelName);
    }

    /**
     * Instantiates a new channel ID.
     *
     * @param channelName name of the channel
     * @return channel ID
     */
    public static GrpcChannelId of(String channelName) {
        return new GrpcChannelId(channelName);
    }
}
