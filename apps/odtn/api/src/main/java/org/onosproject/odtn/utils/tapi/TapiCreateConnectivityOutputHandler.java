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

package org.onosproject.odtn.utils.tapi;

import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.ConnectivityService;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.createconnectivityservice.DefaultCreateConnectivityServiceOutput;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.createconnectivityservice.createconnectivityserviceoutput.DefaultService;

/**
 * Utility class to deal with TAPI RPC output with DCS.
 */
public final class TapiCreateConnectivityOutputHandler
        extends TapiRpcOutputHandler<DefaultCreateConnectivityServiceOutput> {

    private TapiCreateConnectivityOutputHandler() {
        obj = new DefaultCreateConnectivityServiceOutput();
    }

    public static TapiCreateConnectivityOutputHandler create() {
        return new TapiCreateConnectivityOutputHandler();
    }

    public TapiCreateConnectivityOutputHandler addService(ConnectivityService res) {
        DefaultService rpcOutputService = new DefaultService();
        rpcOutputService.uuid(res.uuid());
        res.connection().forEach(rpcOutputService::addToConnection);
        res.endPoint().forEach(rpcOutputService::addToEndPoint);
        obj.service(rpcOutputService);
        return this;
    }
}
