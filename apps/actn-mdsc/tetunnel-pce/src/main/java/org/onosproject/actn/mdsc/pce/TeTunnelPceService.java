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

package org.onosproject.actn.mdsc.pce;

import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;

import java.util.Collection;
import java.util.List;

/**
 * TE tunnel PCE management API.
 */
public interface TeTunnelPceService {

    /**
     * Calculates available paths for the specified TE tunnel.
     * <p>
     * PCE which is suitable for the specified TE tunnel and with the highest
     * priority will be chosen for the path calculation.
     *
     * @param teTunnel tunnel information to be calculated
     * @return available paths for the specified TE tunnel
     */
    Collection<List<TeRouteSubobject>> computePaths(TeTunnel teTunnel);

    /**
     * Calculates available paths for the specified TE tunnel with specified
     * PCE.
     *
     * @param teTunnel tunnel information to be calculated
     * @param pce PCE to be used for path calculation
     * @return available paths for the specified TE tunnel
     */
    Collection<List<TeRouteSubobject>> computePaths(TeTunnel teTunnel,
                                                    TeTunnelPce pce);

    /**
     * Registers a new pce.
     *
     * @param pce new PCE to be registered.
     */
    void registerPce(TeTunnelPce pce);
}
