/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.gnoi;

import gnoi.system.SystemOuterClass.RebootMethod;
import gnoi.system.SystemOuterClass.RebootRequest;
import gnoi.system.SystemOuterClass.RebootResponse;
import org.onosproject.gnoi.api.GnoiClient;
import org.onosproject.gnoi.api.GnoiController;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.behaviour.BasicSystemOperations;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the BasicSystemOperations behavior for gNOI-enabled
 * devices.
 */
public class GnoiBasicSystemOperationsImpl
        extends AbstractGrpcHandlerBehaviour<GnoiClient, GnoiController>
        implements BasicSystemOperations {

    public GnoiBasicSystemOperationsImpl() {
        super(GnoiController.class);
    }

    @Override
    public CompletableFuture<Boolean> reboot() {
        if (!setupBehaviour("reboot()")) {
            return CompletableFuture.completedFuture(false);
        }

        final RebootRequest.Builder requestMsg = RebootRequest.newBuilder().setMethod(RebootMethod.COLD);

        return client.reboot(requestMsg.build())
                .handle((response, error) -> {
                    if (error == null) {
                        log.debug("gNOI reboot() for device {} returned {}", deviceId, response);
                        return RebootResponse.getDefaultInstance().equals(response);
                    } else {
                        log.error("Exception while performing gNOI reboot() for device " + deviceId, error);
                        return false;
                    }
                });
    }

    @Override
    public CompletableFuture<Long> time() {
        if (!setupBehaviour("time()")) {
            return CompletableFuture.completedFuture(0L);
        }
        return client.time()
                .handle((response, error) -> {
                    if (error == null) {
                        log.debug("gNOI time() for device {} returned {}", deviceId.uri(), response.getTime());
                        return response.getTime();
                    } else {
                        log.error("Exception while performing gNOI time() for device " + deviceId, error);
                        return 0L;
                    }
                });
    }
}
