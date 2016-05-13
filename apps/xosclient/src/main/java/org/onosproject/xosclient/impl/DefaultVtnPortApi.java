/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.xosclient.impl;

import org.onosproject.xosclient.api.VtnPort;
import org.onosproject.xosclient.api.VtnPortApi;
import org.onosproject.xosclient.api.VtnPortId;
import org.onosproject.xosclient.api.VtnServiceId;
import org.onosproject.xosclient.api.XosAccess;

import java.util.Set;

/**
 * Provides CORD VTN port APIs.
 */
public class DefaultVtnPortApi extends XosApi implements VtnPortApi {

    /**
     * Default constructor.
     *
     * @param baseUrl base url
     * @param access xos access
     */
    public DefaultVtnPortApi(String baseUrl, XosAccess access) {
        super(baseUrl, access);
    }

    @Override
    public Set<VtnPort> vtnPorts() {
        // TODO implement this when XOS provides this information
        return null;
    }

    @Override
    public Set<VtnPort> vtnPorts(VtnServiceId serviceId) {
        // TODO implement this when XOS provides this information
        return null;
    }

    @Override
    public VtnPort vtnPort(VtnPortId portId) {
        // TODO implement this when XOS provides this information
        return null;
    }
}
