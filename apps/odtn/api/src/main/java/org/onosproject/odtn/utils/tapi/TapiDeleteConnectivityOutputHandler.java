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

import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connectivitycontext.ConnectivityService;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.deleteconnectivityservice.DefaultDeleteConnectivityServiceOutput;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.deleteconnectivityservice.deleteconnectivityserviceoutput.DefaultService;

/**
 * Utility class to deal with TAPI RPC output with DCS.
 */
public final class TapiDeleteConnectivityOutputHandler
        extends TapiRpcOutputHandler<DefaultDeleteConnectivityServiceOutput> {

    private TapiDeleteConnectivityOutputHandler() {
        obj = new DefaultDeleteConnectivityServiceOutput();
    }

    public static TapiDeleteConnectivityOutputHandler create() {
        return new TapiDeleteConnectivityOutputHandler();
    }

    public TapiDeleteConnectivityOutputHandler addService(ConnectivityService res) {
        log.info("Output service: {}", res);
        DefaultService rpcOutputService = new DefaultService();
        rpcOutputService.uuid(res.uuid());
        obj.service(rpcOutputService);
        return this;
    }

}
