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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.createconnectivityservice.CreateConnectivityServiceInput;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.createconnectivityservice.DefaultCreateConnectivityServiceInput;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.createconnectivityservice.createconnectivityserviceinput.EndPoint;

/**
 * Utility class to deal with TAPI RPC input with DCS.
 */
public final class TapiCreateConnectivityInputHandler
        extends TapiRpcInputHandler<DefaultCreateConnectivityServiceInput> {

    public List<EndPoint> getEndPoints() {
        List<EndPoint> eps = CreateConnectivityServiceInput.class.cast(obj).endPoint();
        if (eps == null) {
            return Collections.emptyList();
        }
        return eps;
    }

    public List<String> getSips() {
        return getEndPoints().stream()
                .map(ep -> ep.serviceInterfacePoint().serviceInterfacePointUuid().toString())
                .collect(Collectors.toList());
    }

}
