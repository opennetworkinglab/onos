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

package org.onosproject.actn.mdsc.pce.impl;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.actn.mdsc.pce.TeTunnelPce;
import org.onosproject.actn.mdsc.pce.TeTunnelPceService;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of Te Tunnel PCE service.
 */
@Component(immediate = true)
@Service
public class TeTunnelPceManager implements TeTunnelPceService {

    private static final Logger log = getLogger(TeTunnelPceManager.class);

    private List<TeTunnelPce> pces = Lists.newLinkedList();

    @Activate
    protected void activate() {
        pces.add(0, new DefaultTeTunnelPce());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Collection<List<TeRouteSubobject>> computePaths(TeTunnel teTunnel) {
        TeTunnelPce pce = null;
        synchronized (pces) {
            for (TeTunnelPce p : pces) {
                if (p.isSuitable(teTunnel)) {
                    pce = p;
                }
            }
        }
        return pce.computePaths(teTunnel);
    }

    @Override
    public Collection<List<TeRouteSubobject>> computePaths(TeTunnel teTunnel,
                                                           TeTunnelPce pce) {
        return pce == null ? null : pce.computePaths(teTunnel);
    }

    @Override
    public void registerPce(TeTunnelPce pce) {
        synchronized (pces) {
            int index = 0;
            while (pces.get(index).getPriority() > pce.getPriority()) {
                index++;
            }

            pces.add(index, pce);
        }
    }
}
