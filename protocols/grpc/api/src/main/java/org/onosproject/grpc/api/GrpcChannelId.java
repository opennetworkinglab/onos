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
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * gRPC managed channel identifier, unique in the scope of a gRPC controller
 * instance.
 */
@Beta
public final class GrpcChannelId extends Identifier<String> {

    private final DeviceId deviceId;
    private final String channelName;

    private GrpcChannelId(DeviceId deviceId, String channelName) {
        super(deviceId.toString() + ":" + channelName);
        checkNotNull(deviceId, "device ID must not be null");
        checkNotNull(channelName, "channel name must not be null");
        checkArgument(!channelName.isEmpty(), "channel name must not be empty");
        this.deviceId = deviceId;
        this.channelName = channelName;
    }

    /**
     * Returns the device part of this channel ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the channel name part of this channel ID.
     *
     * @return channel name
     */
    public String channelName() {
        return channelName;
    }

    /**
     * Instantiates a new channel ID for the given device ID and arbitrary
     * channel name (e.g. the name of the gRPC service).
     *
     * @param deviceId    device ID
     * @param channelName name of the channel
     * @return channel ID
     */
    public static GrpcChannelId of(DeviceId deviceId, String channelName) {
        return new GrpcChannelId(deviceId, channelName);
    }
}
