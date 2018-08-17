/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.domain.impl;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.domain.DomainId;
import org.onosproject.net.domain.DomainService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exposes domain topology elements and listen for updates of such elements.
 */
@Component(immediate = true, service = DomainService.class)
public class DomainManager implements DomainService {

    private static final String DOMAIN_ID = "domainId";
    private static final String LOCAL_DOMAIN = "local";
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Set<DomainId> getDomainIds() {
        Set<DomainId> domIds = new HashSet<>();
        deviceService.getAvailableDevices().forEach(dev ->
                domIds.add(getAnnotatedDomainId(dev)));
        return domIds;
    }

    @Override
    public Set<DeviceId> getDeviceIds(DomainId domainId) {
        Set<DeviceId> domainIds = new HashSet<>();
        deviceService.getAvailableDevices().forEach(dev -> {
            if (getAnnotatedDomainId(dev).equals(domainId)) {
                domainIds.add(dev.id());
            }
        });
        return domainIds;
    }

    @Override
    public DomainId getDomain(DeviceId deviceId) {
        checkNotNull(deviceId);
        return Optional.ofNullable(deviceService.getDevice(deviceId))
                    .map(this::getAnnotatedDomainId)
                    .orElse(null);
    }

    private DomainId getAnnotatedDomainId(Device device) {
        if (!device.annotations().keys().contains(DOMAIN_ID)) {
            return DomainId.LOCAL;
        } else {
            return DomainId.domainId(
                    device.annotations().value(DOMAIN_ID));
        }
    }
}