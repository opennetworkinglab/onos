/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.provider.host;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.toHex;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ovsdb.controller.EventSubject;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbEvent;
import org.onosproject.ovsdb.controller.OvsdbEventListener;
import org.onosproject.ovsdb.controller.OvsdbEventSubject;
import org.slf4j.Logger;

/**
 * Provider which uses an ovsdb controller to detect host.
 */
@Component(immediate = true)
@Service
public class OvsdbHostProvider extends AbstractProvider implements HostProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    private HostProviderService providerService;
    private OvsdbEventListener innerEventListener = new InnerOvsdbEventListener();

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addOvsdbEventListener(innerEventListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removeOvsdbEventListener(innerEventListener);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    public OvsdbHostProvider() {
        super(new ProviderId("ovsdb", "org.onosproject.ovsdb.provider.host"));
    }

    @Override
    public void triggerProbe(Host host) {
        log.info("Triggering probe on host {}", host);
    }

    private class InnerOvsdbEventListener implements OvsdbEventListener {

        @Override
        public void handle(OvsdbEvent<EventSubject> event) {
            OvsdbEventSubject subject = null;
            if (event.subject() instanceof OvsdbEventSubject) {
                subject = (OvsdbEventSubject) event.subject();
            }
            checkNotNull(subject, "EventSubject is not null");
            // If ifaceid is null,it indicates this is not a vm port.
            if (subject.ifaceid() == null) {
                return;
            }
            switch (event.type()) {
            case PORT_ADDED:
                HostId hostId = HostId.hostId(subject.hwAddress(), VlanId.vlanId());
                DeviceId deviceId = DeviceId.deviceId(uri(subject.dpid().value()));
                PortNumber portNumber = PortNumber.portNumber(subject
                        .portNumber().value(), subject.portName().value());
                HostLocation loaction = new HostLocation(deviceId, portNumber,
                                                         0L);
                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set("ifaceid", subject.ifaceid().value()).build();
                HostDescription hostDescription = new DefaultHostDescription(
                                                                             subject.hwAddress(),
                                                                             VlanId.vlanId(),
                                                                             loaction,
                                                                             annotations);
                providerService.hostDetected(hostId, hostDescription, false);
                break;
            case PORT_REMOVED:
                HostId host = HostId.hostId(subject.hwAddress(), VlanId.vlanId());
                providerService.hostVanished(host);
                break;
            default:
                break;
            }

        }

    }

    public URI uri(String value) {
        try {
            return new URI("of", toHex(Long.valueOf(value)), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
