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
package org.onosproject.drivers.optical;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Interface to allow for optical device drivers to add/remove links to
 * the store. Acts as a proxy to LinkProviderService.
 *
 * Registers a dummy LinkProvider to get an instance of LinkProviderService.
 */
@Beta
@Component(immediate = true)
@Service
public class OpticalAdjacencyLinkManager implements OpticalAdjacencyLinkService {

    private static final ProviderId PID =
            new ProviderId("of", "org.onosproject.drivers.optical");

    private final Logger log = getLogger(getClass());

    private LinkProvider linkProvider = new StubLinkProvider();
    private LinkProviderService linkProviderService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Activate
    public void activate() {
        linkProviderService = linkProviderRegistry.register(linkProvider);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        linkProviderRegistry.unregister(linkProvider);
        log.info("Stopped");
    }

    /**
     * Signals that an infrastructure link has been detected.
     *
     * @param linkDescription link information
     */
    @Override
    public void linkDetected(LinkDescription linkDescription) {
        linkProviderService.linkDetected(linkDescription);
    }

    /**
     * Signals that an infrastructure link has disappeared.
     *
     * @param linkDescription link information
     */
    @Override
    public void linkVanished(LinkDescription linkDescription) {
        linkProviderService.linkVanished(linkDescription);
    }

    /**
     * Signals that infrastructure links associated with the specified
     * connect point have vanished.
     *
     * @param connectPoint connect point
     */
    @Override
    public void linksVanished(ConnectPoint connectPoint) {
        linkProviderService.linksVanished(connectPoint);
    }

    /**
     * Signals that infrastructure links associated with the specified
     * device have vanished.
     *
     * @param deviceId device identifier
     */
    @Override
    public void linksVanished(DeviceId deviceId) {
        linkProviderService.linksVanished(deviceId);
    }

    // Stub provider used to get LinkProviderService
    private static final class StubLinkProvider implements LinkProvider {
        @Override
        public ProviderId id() {
            return PID;
        }
    }
}
