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
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * gRPCChannel identifier suitable as an external key.
 * <p>
 * This class is immutable.</p>
 */
@Beta
public final class GrpcChannelId extends Identifier<String> {

    private final DeviceId deviceId;

    private final String channelName;

    /**
     * Instantiates a new GrpcChannel id.
     *
     * @param deviceId    the device id
     * @param channelName the name of the channel
     */
    private GrpcChannelId(DeviceId deviceId, String channelName) {
        super(deviceId.toString() + ":" + channelName);
        checkNotNull(deviceId, "device id must not be null");
        checkNotNull(channelName, "channel name must not be null");
        checkArgument(!channelName.isEmpty(), "channel name must not be empty");
        this.deviceId = deviceId;
        this.channelName = channelName;
    }

    /**
     * Returns the deviceId of the device that uses this channel.
     *
     * @return the device Id
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the channel name.
     *
     * @return the channel name
     */
    public String channelName() {
        return channelName;
    }

    /**
     * Creates a grpc channel identifier from the specified device id and name provided.
     *
     * @param id          device id
     * @param channelName name of the channel
     * @return channel name
     */
    public static GrpcChannelId of(DeviceId id, String channelName) {
        return new GrpcChannelId(id, channelName);
    }
}
