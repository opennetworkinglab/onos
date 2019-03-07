/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.gnmi.api;

import com.google.common.annotations.Beta;
import gnmi.Gnmi.CapabilityResponse;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import gnmi.Gnmi.SetRequest;
import gnmi.Gnmi.SetResponse;
import gnmi.Gnmi.SubscribeRequest;
import org.onosproject.grpc.api.GrpcClient;

import java.util.concurrent.CompletableFuture;

/**
 * Client to control a gNMI server.
 */
@Beta
public interface GnmiClient extends GrpcClient {

    /**
     * Gets capability from a target.
     *
     * @return the capability response
     */
    CompletableFuture<CapabilityResponse> capabilities();

    /**
     * Retrieves a snapshot of data from the device.
     *
     * @param request the get request
     * @return the snapshot of data from the device
     */
    CompletableFuture<GetResponse> get(GetRequest request);

    /**
     * Modifies the state of data on the device.
     *
     * @param request the set request
     * @return the set result
     */
    CompletableFuture<SetResponse> set(SetRequest request);

    /**
     * Starts a subscription for the given request. Updates will be notified by
     * the controller via {@link GnmiEvent.Type#UPDATE} events. The client
     * guarantees that a Subscription RPC is active at all times despite channel
     * or server failures, unless {@link #unsubscribe()} is called.
     *
     * @param request the subscribe request
     */
    void subscribe(SubscribeRequest request);

    /**
     * Terminates any Subscribe RPC active.
     */
    void unsubscribe();
}
