/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdListener;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdStore;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * Manager of Cfm Md Service - persists Maintenance Domain in distributed store.
 */
@Component(immediate = true, service = CfmMdService.class)
public class CfmMdManager extends AbstractListenerManager<MdEvent, MdListener>
        implements CfmMdService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.cfm";

    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MdStore store;

    protected final MdStoreDelegate delegate = new InternalStoreDelegate();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        eventDispatcher.addSink(MdEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("CFM Service Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(MdEvent.class);
        store.unsetDelegate(delegate);
        log.info("CFM Service Stopped");
    }

    @Override
    public Collection<MaintenanceDomain> getAllMaintenanceDomain() {
        log.debug("Retrieving all MDs from distributed store");
        return store.getAllMaintenanceDomain();
    }

    @Override
    public Optional<MaintenanceDomain> getMaintenanceDomain(MdId mdName) {
        log.debug("Retrieving MD {} from distributed store", mdName);
        return store.getMaintenanceDomain(mdName);
    }

    @Override
    public boolean deleteMaintenanceDomain(MdId mdName) throws CfmConfigException {
        log.info("Deleting MD {} from distributed store", mdName);
        return store.deleteMaintenanceDomain(mdName);
    }

    @Override
    public Collection<MaintenanceAssociation> getAllMaintenanceAssociation(MdId mdName) {
        log.debug("Retrieving all MA of MD {} from distributed store", mdName);
        return store.getMaintenanceDomain(mdName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown MD " + mdName))
                .maintenanceAssociationList();
    }

    @Override
    public Optional<MaintenanceAssociation> getMaintenanceAssociation(
                                                MdId mdName, MaIdShort maName) {
        log.debug("Retrieving MA {} of MD {} from distributed store", maName, mdName);
        return store.getMaintenanceDomain(mdName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown MD " + mdName))
                .maintenanceAssociationList()
                .stream().filter(ma -> ma.maId().equals(maName))
                .findFirst();
    }

    @Override
    public boolean deleteMaintenanceAssociation(MdId mdName, MaIdShort maName) throws CfmConfigException {
        log.info("Deleting MA {} of MD {} from distributed store", maName, mdName);
        MaintenanceDomain.MdBuilder builder = DefaultMaintenanceDomain
                .builder(store.getMaintenanceDomain(mdName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown MD: " + mdName)));

        //Check the MA is present
        if (!builder.checkMaExists(maName)) {
            return false;
        }

        builder = builder.deleteFromMaList(maName);

        store.createUpdateMaintenanceDomain(builder.build());
        return true;
    }

    @Override
    public boolean createMaintenanceDomain(MaintenanceDomain newMd) throws CfmConfigException {
        log.info("Creating/Updating MD {} in distributed store", newMd.mdId());
        return store.createUpdateMaintenanceDomain(newMd);
    }

    @Override
    public boolean createMaintenanceAssociation(MdId mdName, MaintenanceAssociation newMa)
            throws CfmConfigException {
        log.info("Updating MD {} in distributed store by adding new MA {}", mdName, newMa.maId());
        MaintenanceDomain.MdBuilder builder = DefaultMaintenanceDomain
                .builder(store.getMaintenanceDomain(mdName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown MD: " + mdName)));

        boolean replaced = false;
        //Check the MA is present
        if (builder.checkMaExists(newMa.maId())) {
            builder = builder.deleteFromMaList(newMa.maId());
            replaced = true;
        }

        builder.addToMaList(newMa);
        store.createUpdateMaintenanceDomain(builder.build());
        return replaced;
    }

    private class InternalStoreDelegate implements MdStoreDelegate {
        @Override
        public void notify(MdEvent event) {
            log.debug("New MD event: {}", event);
            eventDispatcher.post(event);
        }
    }
}
