/*
 * Copyright 2018-present Open Networking Foundation
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
package protocols.gnmi.ctl.java.org.onosproject.gnmi.ctl;

import gnmi.Gnmi.CapabilityRequest;
import gnmi.Gnmi.CapabilityResponse;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import gnmi.Gnmi.SetRequest;
import gnmi.Gnmi.SetResponse;
import gnmi.gNMIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.onosproject.gnmi.api.GnmiClientKey;
import org.onosproject.grpc.ctl.AbstractGrpcClient;
import org.slf4j.Logger;
import org.onosproject.gnmi.api.GnmiClient;

import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of gNMI client.
 */
public class GnmiClientImpl extends AbstractGrpcClient implements GnmiClient {
    private final Logger log = getLogger(getClass());
    private final gNMIGrpc.gNMIBlockingStub blockingStub;

    public GnmiClientImpl(GnmiClientKey clientKey, ManagedChannel managedChannel) {
        super(clientKey, managedChannel);
        this.blockingStub = gNMIGrpc.newBlockingStub(managedChannel);
    }

    @Override
    public CompletableFuture<CapabilityResponse> capability() {
        return supplyInContext(this::doCapability, "capability");
    }

    @Override
    public CompletableFuture<GetResponse> get(GetRequest request) {
        return supplyInContext(() -> doGet(request), "get");
    }

    @Override
    public CompletableFuture<SetResponse> set(SetRequest request) {
        return supplyInContext(() -> doSet(request), "set");
    }

    private CapabilityResponse doCapability() {
        CapabilityRequest request = CapabilityRequest.newBuilder().build();
        try {
            return blockingStub.capabilities(request);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to get capability from {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    private GetResponse doGet(GetRequest request) {
        try {
            return blockingStub.get(request);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to get data from {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    private SetResponse doSet(SetRequest request) {
        try {
            return blockingStub.set(request);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to set data to {}: {}", deviceId, e.getMessage());
            return null;
        }
    }
}
