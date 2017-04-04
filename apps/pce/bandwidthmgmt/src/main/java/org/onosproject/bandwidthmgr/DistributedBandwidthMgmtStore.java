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
package org.onosproject.bandwidthmgr;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REMOVED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.LinkKey;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtStore;
import org.onosproject.pcep.api.TeLinkConfig;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the pool of available labels to devices, links and tunnels.
 */
@Component(immediate = true)
@Service
public class DistributedBandwidthMgmtStore implements BandwidthMgmtStore {
    private static final Logger log = LoggerFactory.getLogger(BandwidthManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private InternalConfigListener cfgListener = new InternalConfigListener();

    private ConsistentMap<LinkKey, Double> teCost;
    // Locally maintain unreserved bandwidth of each link.
    private ConsistentMap<LinkKey, Set<Double>> unResvBw;

    // Mapping tunnel with link key with local reserved bandwidth
    private ConsistentMap<LinkKey, Double> localReservedBw;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                    .register(KryoNamespaces.API)
                    .register(LinkKey.class)
                    .register(ConnectPoint.class)
                    .build());

    @Activate
    protected void activate() {
        netCfgService.addListener(cfgListener);

        localReservedBw = storageService.<LinkKey, Double>consistentMapBuilder()
                .withName("local-reserved-bandwith")
                .withSerializer(SERIALIZER)
                .build();

        unResvBw = storageService.<LinkKey, Set<Double>>consistentMapBuilder()
                .withName("onos-unreserved-bandwidth")
                .withSerializer(SERIALIZER)
                .build();

        teCost = storageService.<LinkKey, Double>consistentMapBuilder()
                .withName("onos-tecost")
                .withSerializer(SERIALIZER)
                .build();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        netCfgService.removeListener(cfgListener);
        log.info("Stopped");
    }

    @Override
    public Double getTeCost(LinkKey linkKey) {
        if (teCost.get(linkKey) != null) {
            return teCost.get(linkKey).value();
        }
        return null;
    }

    @Override
    public boolean allocLocalReservedBw(LinkKey linkkey, Double bandwidth) {
        Double allocatedBw = null;

        if (localReservedBw.get(linkkey) != null) {
            allocatedBw = localReservedBw.get(linkkey).value();
        }
        if (allocatedBw != null) {
            localReservedBw.put(linkkey, (allocatedBw + bandwidth));
        } else {
            localReservedBw.put(linkkey, bandwidth);
        }

        return true;
    }

    @Override
    public boolean releaseLocalReservedBw(LinkKey linkkey, Double bandwidth) {

        Double allocatedBw = null;
        if (localReservedBw.get(linkkey) != null) {
            allocatedBw = localReservedBw.get(linkkey).value();
        }

        if (allocatedBw == null || allocatedBw < bandwidth) {
            return false;
        }

        Double releasedBw = allocatedBw - bandwidth;
        if (releasedBw == 0.0) {
            localReservedBw.remove(linkkey);
        } else {
            localReservedBw.put(linkkey, releasedBw);
        }
        return true;
    }

    @Override
    public Double getAllocatedLocalReservedBw(LinkKey linkkey) {
        return localReservedBw.get(linkkey) != null ? localReservedBw.get(linkkey).value() : null;
    }

    @Override
    public boolean addUnreservedBw(LinkKey linkkey, Set<Double> bandwidth) {
        unResvBw.put(linkkey, bandwidth);
        return true;
    }

    @Override
    public boolean removeUnreservedBw(LinkKey linkkey) {
        unResvBw.remove(linkkey);
        return true;
    }

    @Override
    public Set<Double> getUnreservedBw(LinkKey linkkey) {
        checkNotNull(linkkey);
        return unResvBw.get(linkkey) != null ? unResvBw.get(linkkey).value() : null;
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {

            if (event.configClass().equals(TeLinkConfig.class)) {
                if ((event.type() != CONFIG_ADDED) &&  (event.type() != CONFIG_UPDATED)
                        && (event.type() != CONFIG_REMOVED)) {
                    return;
                }
                LinkKey linkKey = (LinkKey) event.subject();
                switch (event.type()) {
                    case  CONFIG_ADDED:
                    case  CONFIG_UPDATED:

                        TeLinkConfig cfg = netCfgService.getConfig(linkKey, TeLinkConfig.class);
                        if (cfg == null) {
                            log.error("Unable to get the configuration of the link.");
                            return;
                        }
                        Set<Double> unresvBw = new LinkedHashSet<>();
                        unresvBw.add(cfg.unResvBandwidth());
                        addUnreservedBw(linkKey, unresvBw);

                        if (cfg.teCost() != 0) {
                            teCost.put(linkKey, (double) cfg.teCost());
                        }

                        break;
                    case CONFIG_REMOVED:
                        removeUnreservedBw(linkKey);
                        localReservedBw.remove(linkKey);
                        teCost.remove(linkKey);

                        break;
                    default:
                        break;
                }
            }
        }
    }

}
