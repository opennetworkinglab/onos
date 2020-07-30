/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.gnoi.api;

import gnoi.system.SystemOuterClass.TimeResponse;
import gnoi.system.SystemOuterClass.RebootRequest;
import gnoi.system.SystemOuterClass.RebootResponse;
import com.google.common.annotations.Beta;
import org.onosproject.grpc.api.GrpcClient;
import java.util.concurrent.CompletableFuture;

/**
 * Client to control a gNOI server.
 */
@Beta
public interface GnoiClient extends GrpcClient {

    /**
     * Returns the current time on the target.
     *
     * @return the TimeResponse result
     */
    CompletableFuture<TimeResponse> time();

    /**
     * Causes the target to reboot immediately.
     *
     * @param request RebootRequest
     * @return the RebootResponse result
     */
    CompletableFuture<RebootResponse> reboot(RebootRequest request);
}
