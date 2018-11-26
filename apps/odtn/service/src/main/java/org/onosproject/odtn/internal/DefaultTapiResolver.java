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

package org.onosproject.odtn.internal;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * OSGi Component for ODTN TAPI resolver application.
 */
@Component(immediate = true, service = TapiResolver.class)
public class DefaultTapiResolver implements TapiResolver {

    private final Logger log = getLogger(getClass());

    protected TapiDataProducer dataProvider = new DcsBasedTapiDataProducer();

    private List<TapiNodeRef> tapiNodeRefList = new CopyOnWriteArrayList<>();
    private List<TapiNepRef> tapiNepRefList = new CopyOnWriteArrayList<>();

    /**
     * When source (e.g. DCS) is updated, set true
     * When cache update completed successfully, set false
     * <p>
     * This flag takes effect when cache update failed with exception,
     * this remains to be true so the cache update process conducts again
     */
    private Boolean isDirty = false;

    /**
     * When source (e.g. DCS) is updated, set true
     * When cache update started, set false
     * <p>
     * This flag takes effect when source updated during cache updating
     * this forces cache update again at the next request
     */
    private Boolean sourceUpdated = false;

    @Activate
    public void activate() {
        log.info("Started");
        dataProvider.init();
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean hasNodeRef(ElementId deviceId) {
        updateCache();
        return tapiNodeRefList.stream()
                .anyMatch(node -> node.getDeviceId().equals(deviceId));
    }

    @Override
    public boolean hasNepRef(ConnectPoint cp) {
        updateCache();
        return tapiNepRefList.stream()
                .anyMatch(nep -> nep.getConnectPoint().equals(cp));
    }

    @Override
    public boolean hasNepRef(String sipId) {
        updateCache();
        return tapiNepRefList.stream()
                .anyMatch(nep -> nep.getSipId() != null && nep.getSipId().equals(sipId));
    }

    @Override
    public TapiNodeRef getNodeRef(TapiNodeRef nodeRef) throws NoSuchElementException {
        updateCache();
        TapiNodeRef ret = null;
        try {
            ret = tapiNodeRefList.stream()
                    .filter(nodeRef::equals)
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Node not found of {}", nodeRef);
            throw e;
        }
        return ret;
    }

    @Override
    public TapiNodeRef getNodeRef(ElementId deviceId) throws NoSuchElementException {
        updateCache();
        TapiNodeRef ret = null;
        try {
            ret = tapiNodeRefList.stream()
                    .filter(node -> node.getDeviceId() != null && node.getDeviceId().equals(deviceId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Node not found associated with {}", deviceId);
            throw e;
        }
        return ret;
    }

    @Override
    public List<TapiNodeRef> getNodeRefs() {
        updateCache();
        return new ArrayList<>(tapiNodeRefList);
    }

    @Override
    public List<TapiNodeRef> getNodeRefs(Map<String, String> filter) {
        updateCache();
        Stream<TapiNodeRef> filterStream = tapiNodeRefList.stream();
        for (String key : filter.keySet()) {
            filterStream = filterStream.filter(nodeRef -> nodeRef.is(key, filter.get(key)));
        }
        return filterStream.collect(Collectors.toList());
    }

    @Override
    public TapiNepRef getNepRef(TapiNepRef nepRef) throws NoSuchElementException {
        updateCache();
        TapiNepRef ret = null;
        try {
            ret = tapiNepRefList.stream()
                    .filter(nepRef::equals)
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Nep not found of {}", nepRef);
            throw e;
        }
        return ret;
    }

    @Override
    public TapiNepRef getNepRef(ConnectPoint cp) throws NoSuchElementException {
        updateCache();
        TapiNepRef ret = null;
        try {
            ret = tapiNepRefList.stream()
                    .filter(nep -> nep.getConnectPoint() != null && nep.getConnectPoint().equals(cp))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Nep not found associated with {}", cp);
            throw e;
        }
        return ret;
    }

    @Override
    public TapiNepRef getNepRef(String sipId) throws NoSuchElementException {
        updateCache();
        TapiNepRef ret = null;
        try {
            ret = tapiNepRefList.stream()
                    .filter(nep -> nep.getSipId() != null && nep.getSipId().equals(sipId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.error("Nep not found associated with {}", sipId);
            throw e;
        }
        return ret;
    }

    @Override
    public List<TapiNepRef> getNepRefs() {
        updateCache();
        return new ArrayList<>(tapiNepRefList);
    }

    @Override
    public List<TapiNepRef> getNepRefs(Map<String, String> filter) {
        updateCache();
        Stream<TapiNepRef> filterStream = tapiNepRefList.stream();
        for (String key : filter.keySet()) {
            filterStream = filterStream.filter(nepRef -> nepRef.is(key, filter.get(key)));
        }
        return filterStream.collect(Collectors.toList());
    }

    @Override
    public void makeDirty() {
        sourceUpdated = true;
        isDirty = true;
    }

    protected void addNodeRef(TapiNodeRef nodeRef) {
        tapiNodeRefList.add(nodeRef);
        log.debug("Nodes: {}", tapiNodeRefList);
    }

    protected void addNepRef(TapiNepRef nepRef) {
        tapiNepRefList.add(nepRef);
        log.debug("Neps: {}", tapiNepRefList);
    }

    protected void addNodeRefList(List<TapiNodeRef> nodes) {
        tapiNodeRefList = nodes;
        log.debug("Nodes: {}", tapiNodeRefList);
    }

    protected void addNepRefList(List<TapiNepRef> neps) {
        tapiNepRefList = neps;
        log.debug("Neps: {}", tapiNepRefList);
    }

    private void updateCache() {
        log.debug("Dirty: {}, Source updated: {}", isDirty, sourceUpdated);
        if (isDirty || sourceUpdated) {
            sourceUpdated = false;
            clearCache();
            dataProvider.updateCacheRequest(this);
            log.debug("Update completed: {}", tapiNodeRefList);
            isDirty = false;
        }
    }

    private void clearCache() {
        tapiNodeRefList.clear();
        tapiNepRefList.clear();
    }
}
