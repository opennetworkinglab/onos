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

import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MepStore;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MepStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MEP Store implementation backed by consistent map.
 */
@Component(immediate = true, service = MepStore.class)
public class DistributedMepStore extends AbstractStore<CfmMepEvent, MepStoreDelegate>
    implements MepStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ConsistentMap<MepKeyId, Mep> mepConsistentMap;
    private Map<MepKeyId, Mep> mepMap;

    private MapEventListener<MepKeyId, Mep> mapListener = null;

    @Activate
    public void activate() {
        mepConsistentMap = storageService
                .<MepKeyId, Mep>consistentMapBuilder()
                .withName("onos-cfm-mep-map")
                .withSerializer(Serializer.using(new KryoNamespace.Builder()
                        .register(KryoNamespaces.API)
                        .register(DefaultMep.class)
                        .register(MepId.class)
                        .register(MepKeyId.class)
                        .register(NetworkResource.class)
                        .register(DeviceId.class)
                        .register(PortNumber.class)
                        .register(Mep.MepDirection.class)
                        .register(VlanId.class)
                        .register(Mep.Priority.class)
                        .register(Mep.FngAddress.class)
                        .register(Mep.FngAddressType.class)
                        .register(IpAddress.class)
                        .register(Mep.LowestFaultDefect.class)
                        .register(Duration.class)
                        .register(MdIdCharStr.class)
                        .register(MdIdDomainName.class)
                        .register(MdIdMacUint.class)
                        .register(MdIdNone.class)
                        .register(MaIdCharStr.class)
                        .register(MaIdShort.class)
                        .register(MaId2Octet.class)
                        .register(MaIdIccY1731.class)
                        .register(MaIdPrimaryVid.class)
                        .register(MaIdRfc2685VpnId.class)
                        .build("mep")))
                .build();
        mapListener = new InternalMepListener();
        mepConsistentMap.addListener(mapListener);

        mepMap = mepConsistentMap.asJavaMap();
        log.info("MepStore started");
    }

    @Deactivate
    public void deactivate() {
        mepConsistentMap.removeListener(mapListener);
        log.info("MepStore stopped");
    }

    @Override
    public Collection<Mep> getAllMeps() {
        return mepMap.values();
    }

    @Override
    public Collection<Mep> getMepsByMd(MdId mdName) {
        return mepMap.values().stream()
                .filter(mep -> mep.mdId().equals(mdName))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Mep> getMepsByMdMa(MdId mdName, MaIdShort maName) {
        return mepMap.values().stream()
                .filter(mep -> mep.mdId().equals(mdName) && mep.maId().equals(maName))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Mep> getMepsByDeviceId(DeviceId deviceId) {
        return mepMap.values().stream()
                .filter(mep -> mep.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Mep> getMep(MepKeyId mepKeyId) {
        return mepMap.values().stream()
                .filter(mep -> mep.mdId().equals(mepKeyId.mdId()) &&
                        mep.maId().equals(mepKeyId.maId()) &&
                        mep.mepId().equals(mepKeyId.mepId()))
                .findFirst();
    }

    @Override
    public boolean deleteMep(MepKeyId mepKeyId) {
        return mepMap.remove(mepKeyId) == null ? false : true;
    }

    @Override
    public boolean createUpdateMep(MepKeyId mepKeyId, Mep mep) {
        return mepMap.put(mepKeyId, mep) == null ? false : true;
    }

    private class InternalMepListener implements MapEventListener<MepKeyId, Mep> {

        @Override
        public void event(MapEvent<MepKeyId, Mep> mapEvent) {
            final CfmMepEvent.Type type;

            switch (mapEvent.type()) {
                case INSERT:
                    type = CfmMepEvent.Type.MEP_ADDED;
                    break;
                case UPDATE:
                    type = CfmMepEvent.Type.MEP_UPDATED;
                    break;
                default:
                case REMOVE:
                    type = CfmMepEvent.Type.MEP_REMOVED;
            }
            notifyDelegate(new CfmMepEvent(type, mapEvent.key()));
        }
    }
}
