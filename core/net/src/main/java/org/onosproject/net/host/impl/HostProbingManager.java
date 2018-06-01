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

package org.onosproject.net.host.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostLocationProbingService;
import org.onosproject.net.host.HostProbingEvent;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.net.host.HostProbingListener;
import org.onosproject.net.host.HostProbingProvider;
import org.onosproject.net.host.HostProbingProviderService;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostProbeStore;
import org.onosproject.net.host.HostProbingProviderRegistry;
import org.onosproject.net.host.HostProbingStoreDelegate;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
@Service
public class HostProbingManager extends
        AbstractListenerProviderRegistry<HostProbingEvent, HostProbingListener, HostProbingProvider,
                HostProbingProviderService>
        implements HostProbingService, HostProbingProviderRegistry, HostLocationProbingService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostProbeStore hostProbeStore;

    private static final String SCHEME = "hostprobing";

    private HostProbingStoreDelegate delegate = event -> {
        getProvider(SCHEME).processEvent(event);
    };

    @Activate
    public void activate() {
        hostProbeStore.setDelegate(delegate);
    }

    @Deactivate
    public void deactivate() {
        hostProbeStore.unsetDelegate(delegate);
    }

    @Override
    public void probeHost(Host host, ConnectPoint connectPoint, ProbeMode probeMode) {
        HostProbingProvider provider = getProvider(SCHEME);
        if (provider == null) {
            log.warn("Unable to find host probing provider. Cannot {} {} at {}",
                    probeMode, host, connectPoint);
            return;
        }
        provider.probeHost(host, connectPoint, probeMode);
    }

    @Override
    protected HostProbingProviderService createProviderService(HostProbingProvider provider) {
        return new InternalHostProbingProviderService(provider);
    }

    private class InternalHostProbingProviderService
            extends AbstractProviderService<HostProbingProvider>
            implements HostProbingProviderService {
        InternalHostProbingProviderService(HostProbingProvider provider) {
            super(provider);
        }

        @Override
        public MacAddress addProbingHost(Host host, ConnectPoint connectPoint, ProbeMode mode,
                                         MacAddress probeMac, int retry) {
            return hostProbeStore.addProbingHost(host, connectPoint, mode, probeMac, retry);
        }

        @Override
        public void removeProbingHost(MacAddress probeMac) {
            hostProbeStore.removeProbingHost(probeMac);
        }
    }
}
