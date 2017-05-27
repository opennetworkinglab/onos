/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.vpls.store;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.StoreDelegate;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vpls.VplsManager;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.VplsOperation;
import org.onosproject.vpls.config.VplsAppConfig;
import org.onosproject.vpls.api.VplsStore;
import org.onosproject.vpls.config.VplsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Objects.*;

/**
 * Implementation of VPLSConfigurationService which reads VPLS configuration
 * from the network configuration service.
 */
@Component(immediate = true)
@Service
public class DistributedVplsStore
        extends AbstractStore<VplsStoreEvent, StoreDelegate<VplsStoreEvent>>
        implements VplsStore {

    private static final KryoNamespace APP_KRYO = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Interface.class)
            .register(VplsData.class)
            .register(VplsData.VplsState.class)
            .register(VplsOperation.class)
            .build();

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    private EventuallyConsistentMap<String, VplsData> vplsDataStore;
    private EventuallyConsistentMapListener<String, VplsData> vplsDataListener;
    private ApplicationId appId;

    @Activate
    protected void active() {
        appId = coreService.registerApplication(VplsManager.VPLS_APP);

        vplsDataStore = storageService.<String, VplsData>eventuallyConsistentMapBuilder()
                .withName("VPLS-Data")
                .withTimestampProvider((name, vpls) -> new WallClockTimestamp())
                .withSerializer(APP_KRYO)
                .build();
        vplsDataListener = new InternalVplsDataListener();
        vplsDataStore.addListener(vplsDataListener);
        log.info("Started");
    }

    @Deactivate
    protected  void deactive() {
        vplsDataStore.removeListener(vplsDataListener);
        networkConfigService.removeConfig(appId);
        log.info("Stopped");
    }

    @Override
    public void addVpls(VplsData vplsData) {
        requireNonNull(vplsData);
        if (vplsData.name().isEmpty()) {
            throw new IllegalArgumentException("VPLS name is empty.");
        }
        vplsData.state(VplsData.VplsState.ADDING);
        this.vplsDataStore.put(vplsData.name(), vplsData);
    }

    @Override
    public void removeVpls(VplsData vplsData) {
        requireNonNull(vplsData);
        if (vplsData.name().isEmpty()) {
            throw new IllegalArgumentException("VPLS name is empty.");
        }
        vplsData.state(VplsData.VplsState.REMOVING);
        if (!this.vplsDataStore.containsKey(vplsData.name())) {
            // notify the delegate asynchronously if VPLS does not exists
            CompletableFuture.runAsync(() -> {
                VplsStoreEvent event = new VplsStoreEvent(VplsStoreEvent.Type.REMOVE,
                                                          vplsData);
                notifyDelegate(event);
            });
            return;
        }
        this.vplsDataStore.remove(vplsData.name());
    }

    @Override
    public void updateVpls(VplsData vplsData) {
        switch (vplsData.state()) {
            case ADDED:
            case REMOVED:
            case FAILED:
                // state update only
                this.vplsDataStore.put(vplsData.name(), vplsData);
                break;
            default:
                vplsData.state(VplsData.VplsState.UPDATING);
                this.vplsDataStore.put(vplsData.name(), vplsData);
                break;
        }
    }

    @Override
    public VplsData getVpls(String vplsName) {
        return vplsDataStore.get(vplsName);
    }

    @Override
    public Collection<VplsData> getAllVpls() {
        return vplsDataStore.values();
    }

    /**
     * Writes all VPLS data to the network configuration store.
     *
     * @param vplsDataCollection the VPLSs data
     */
    public void writeVplsToNetConfig(Collection<VplsData> vplsDataCollection) {
        VplsAppConfig config = networkConfigService.addConfig(appId, VplsAppConfig.class);
        if (config == null) {
            log.debug("VPLS config is not available now");
            return;
        }
        config.clearVplsConfig();

        // Setup update time for this VPLS application configuration
        WallClockTimestamp ts = new WallClockTimestamp();
        config.updateTime(ts.unixTimestamp());

        vplsDataCollection.forEach(vplsData -> {
            Set<String> interfaceNames = vplsData.interfaces()
                    .stream()
                    .map(Interface::name)
                    .collect(Collectors.toSet());
            VplsConfig vplsConfig = new VplsConfig(vplsData.name(), interfaceNames,
                                                   vplsData.encapsulationType());
            config.addVpls(vplsConfig);
        });

        networkConfigService.applyConfig(appId, VplsAppConfig.class, config.node());
    }

    /**
     * Listener for VPLS data store.
     */
    private class InternalVplsDataListener implements EventuallyConsistentMapListener<String, VplsData> {
        private static final String STATE_UPDATE = "VPLS state updated, new VPLS: {}";

        @Override
        public void event(EventuallyConsistentMapEvent<String, VplsData> event) {
            VplsData vplsData = event.value();
            // Update network config
            writeVplsToNetConfig(getAllVpls());
            switch (event.type()) {
                case PUT:
                    // Add or Update
                    if (vplsData.state() == VplsData.VplsState.ADDING) {
                        VplsStoreEvent vplsStoreEvent =
                                new VplsStoreEvent(VplsStoreEvent.Type.ADD, vplsData);
                        notifyDelegate(vplsStoreEvent);
                    } else if (vplsData.state() == VplsData.VplsState.UPDATING) {
                        VplsStoreEvent vplsStoreEvent =
                                new VplsStoreEvent(VplsStoreEvent.Type.UPDATE, vplsData);
                        notifyDelegate(vplsStoreEvent);
                    } else {
                        // Do nothing here, just update state from operation service
                        log.debug(STATE_UPDATE, vplsData);
                    }
                    break;
                case REMOVE:
                    if (vplsData == null) {
                        vplsData = VplsData.of(event.key());
                    }
                    vplsData.state(VplsData.VplsState.REMOVING);
                    VplsStoreEvent vplsStoreEvent =
                            new VplsStoreEvent(VplsStoreEvent.Type.REMOVE, vplsData);
                    notifyDelegate(vplsStoreEvent);
                    break;
                default:
                    break;
            }

        }
    }
}