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

package org.onosproject.odtn.internal;

import org.onosproject.config.DynamicConfigService;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.utils.tapi.TapiGetSipListOutputHandler;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.TapiCommonService;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

/**
 * DCS-dependent tapi-common yang RPCs implementation.
 */
public class DcsBasedTapiCommonRpc implements TapiCommonService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected DynamicConfigService dcs;
    protected ModelConverter modelConverter;
    protected TapiResolver resolver;

    public void init() {
        dcs = getService(DynamicConfigService.class);
        modelConverter = getService(ModelConverter.class);
        resolver = getService(TapiResolver.class);
    }

    /**
     * Service interface of getServiceInterfacePointDetails.
     *
     * @param rpcInput input of service interface getServiceInterfacePointDetails
     * @return rpcOutput output of service interface getServiceInterfacePointDetails
     */
    @Override
    public RpcOutput getServiceInterfacePointDetails(RpcInput rpcInput) {
        log.error("Not implemented");
        return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
    }

    /**
     * Service interface of getServiceInterfacePointList.
     *
     * @param rpcInput input of service interface getServiceInterfacePointList
     * @return rpcOutput output of service interface getServiceInterfacePointList
     */
    @Override
    public RpcOutput getServiceInterfacePointList(RpcInput rpcInput) {

        try {
            TapiGetSipListOutputHandler output = TapiGetSipListOutputHandler.create();

            resolver.getNepRefs().stream()
                    .filter(nepRef -> nepRef.getSipId() != null)
                    .forEach(nepRef -> {
                        output.addSip(Uuid.fromString(nepRef.getSipId()));
                    });

            return new RpcOutput(RpcOutput.Status.RPC_SUCCESS, output.getDataNode());
        } catch (Throwable e) {
            log.error("Error:", e);
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }

    }

    /**
     * Service interface of updateServiceInterfacePoint.
     *
     * @param rpcInput input of service interface updateServiceInterfacePoint
     * @return rpcOutput output of service interface updateServiceInterfacePoint
     */
    @Override
    public RpcOutput updateServiceInterfacePoint(RpcInput rpcInput) {
        log.error("Not implemented");
        return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
    }
}

