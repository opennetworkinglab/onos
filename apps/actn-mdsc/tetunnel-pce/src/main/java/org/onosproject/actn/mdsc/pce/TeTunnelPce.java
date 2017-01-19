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
 * PCE which calculates paths for TE tunnels.
 */
public interface TeTunnelPce {

    static final int PRIORITY_HIGHEST = 255;
    static final int PRIORITY_HIGH = PRIORITY_HIGHEST * 3 / 4;
    static final int PRIORITY_MEDIUM = PRIORITY_HIGHEST / 2;
    static final int PRIORITY_LOW = PRIORITY_HIGHEST / 4;
    static final int PRIORITY_LOWEST = 0;

    /**
     * Returns priority of this PCE.
     *
     * @return priority of this PCE
     */
    int getPriority();

    /**
     * Signifies whether this PCE is suitable for the specified TE tunnel.
     *
     * @param teTunnel tunnel to check
     * @return true if this PCE can calculate path for the TE tunnel
     */
    boolean isSuitable(TeTunnel teTunnel);

    /**
     * Calculates available paths for the specified TE tunnel.
     *
     * @param teTunnel tunnel information to be calculated
     * @return available paths for the specified TE tunnel
     */
    Collection<List<TeRouteSubobject>> computePaths(TeTunnel teTunnel);
}
